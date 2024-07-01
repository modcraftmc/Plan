/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package net.playeranalytics.plan.gathering.listeners.forge;

import com.djrapitops.plan.gathering.JoinAddressValidator;
import com.djrapitops.plan.gathering.cache.JoinAddressCache;
import com.djrapitops.plan.gathering.domain.event.PlayerJoin;
import com.djrapitops.plan.gathering.domain.event.PlayerLeave;
import com.djrapitops.plan.gathering.events.PlayerJoinEventConsumer;
import com.djrapitops.plan.gathering.events.PlayerLeaveEventConsumer;
import com.djrapitops.plan.identification.ServerInfo;
import com.djrapitops.plan.identification.ServerUUID;
import com.djrapitops.plan.storage.database.DBSystem;
import com.djrapitops.plan.storage.database.transactions.events.BanStatusTransaction;
import com.djrapitops.plan.storage.database.transactions.events.KickStoreTransaction;
import com.djrapitops.plan.utilities.logging.ErrorContext;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.playeranalytics.plan.gathering.ForgePlayerPositionTracker;
import net.playeranalytics.plan.gathering.domain.ForgePlayerData;
import net.playeranalytics.plan.gathering.listeners.ForgeListener;
import net.playeranalytics.plan.gathering.listeners.events.PlanForgeEvents;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class PlayerOnlineListener implements ForgeListener {

    private final PlayerJoinEventConsumer joinEventConsumer;
    private final PlayerLeaveEventConsumer leaveEventConsumer;
    private final JoinAddressCache joinAddressCache;
    private final JoinAddressValidator joinAddressValidator;

    private final ServerInfo serverInfo;
    private final DBSystem dbSystem;
    private final ErrorLogger errorLogger;
    private final MinecraftServer server;

    private final AtomicReference<String> joinAddress = new AtomicReference<>();

    private boolean isEnabled = false;
    private boolean wasRegistered = false;

    @Inject
    public PlayerOnlineListener(
            PlayerJoinEventConsumer joinEventConsumer,
            PlayerLeaveEventConsumer leaveEventConsumer,
            JoinAddressCache joinAddressCache,
            JoinAddressValidator joinAddressValidator,
            ServerInfo serverInfo,
            DBSystem dbSystem,
            ErrorLogger errorLogger,
            MinecraftServer server
    ) {
        this.joinEventConsumer = joinEventConsumer;
        this.leaveEventConsumer = leaveEventConsumer;
        this.joinAddressCache = joinAddressCache;
        this.joinAddressValidator = joinAddressValidator;
        this.serverInfo = serverInfo;
        this.dbSystem = dbSystem;
        this.errorLogger = errorLogger;
        this.server = server;
    }

    @Override
    public void register() {
        if (this.wasRegistered) {
            return;
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!this.isEnabled) {
                return;
            }
            onPlayerJoin(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!this.isEnabled) {
                return;
            }
            beforePlayerQuit(handler.player);
            onPlayerQuit(handler.player);
        });
        PlanForgeEvents.ON_KICKED.register((source, targets, reason) -> {
            if (!this.isEnabled) {
                return;
            }
            for (ServerPlayer target : targets) {
                onPlayerKick(target);
            }
        });
        PlanForgeEvents.ON_LOGIN.register((address, profile, reason) -> {
            if (!this.isEnabled) {
                return;
            }
            onPlayerLogin(address, profile, reason != null);
        });
        PlanForgeEvents.ON_HANDSHAKE.register(packet -> {
            if (!this.isEnabled) {
                return;
            }
            onHandshake(packet);
        });

        this.enable();
        this.wasRegistered = true;
    }

    private void onHandshake(HandshakeC2SPacket packet) {
        try {
            if (packet.intendedState() == ConnectionIntent.LOGIN) {
                String address = joinAddressValidator.sanitize(packet.address());
                joinAddress.set(address);
            }
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), "onHandshake").build());
        }
    }

    public void onPlayerLogin(SocketAddress address, GameProfile profile, boolean banned) {
        try {
            UUID playerUUID = profile.getId();
            ServerUUID serverUUID = serverInfo.getServerUUID();

            String playerJoinAddress = joinAddress.get();
            if (joinAddressValidator.isValid(playerJoinAddress)) {
                joinAddressCache.put(playerUUID, playerJoinAddress);
            }

            dbSystem.getDatabase().executeTransaction(new BanStatusTransaction(playerUUID, serverUUID, banned));
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), address, profile, banned).build());
        }
    }

    public void onPlayerKick(ServerPlayer player) {
        try {
            UUID uuid = player.getUUID();
            if (ForgeAFKListener.afkTracker.isAfk(uuid)) {
                return;
            }

            dbSystem.getDatabase().executeTransaction(new KickStoreTransaction(uuid));
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), player).build());
        }
    }

    public void onPlayerJoin(ServerPlayer player) {
        try {
            actOnJoinEvent(player);
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), player).build());
        }
    }

    private void actOnJoinEvent(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        long time = System.currentTimeMillis();

        ForgeAFKListener.afkTracker.performedAction(playerUUID, time);

        joinEventConsumer.onJoinGameServer(PlayerJoin.builder()
                .server(serverInfo.getServer())
                .player(new ForgePlayerData(player, server, joinAddressCache.getNullableString(playerUUID)))
                .time(time)
                .build());
    }

    // No event priorities on Fabric, so this has to be called with onPlayerQuit()
    public void beforePlayerQuit(ServerPlayer player) {
        leaveEventConsumer.beforeLeave(PlayerLeave.builder()
                .server(serverInfo.getServer())
                .player(new ForgePlayerData(player, server, null))
                .time(System.currentTimeMillis())
                .build());
    }

    public void onPlayerQuit(ServerPlayer player) {
        try {
            actOnQuitEvent(player);
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), player).build());
        }
    }

    private void actOnQuitEvent(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        long time = System.currentTimeMillis();
        ForgeAFKListener.afkTracker.loggedOut(playerUUID, time);
        ForgePlayerPositionTracker.removePlayer(playerUUID);

        leaveEventConsumer.onLeaveGameServer(PlayerLeave.builder()
                .server(serverInfo.getServer())
                .player(new ForgePlayerData(player, server, null))
                .time(time)
                .build());
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    @Override
    public void enable() {
        this.isEnabled = true;
    }

    @Override
    public void disable() {
        this.isEnabled = false;
    }
}
