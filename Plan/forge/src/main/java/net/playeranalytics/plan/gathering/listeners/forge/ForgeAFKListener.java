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

import com.djrapitops.plan.gathering.afk.AFKTracker;
import com.djrapitops.plan.settings.config.PlanConfig;
import com.djrapitops.plan.utilities.logging.ErrorContext;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.playeranalytics.plan.commands.ForgeCommandManager;
import net.playeranalytics.plan.gathering.listeners.ForgeListener;
import net.playeranalytics.plan.gathering.listeners.events.PlanForgeEvents;
import net.playeranalytics.plan.gathering.listeners.events.impl.OnCommandEvent;
import net.playeranalytics.plan.gathering.listeners.events.impl.OnMoveEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ForgeAFKListener implements ForgeListener {

    // Static so that /reload does not cause afk tracking to fail.
    static AFKTracker afkTracker;
    private final Map<UUID, Boolean> ignorePermissionInfo;
    private final ErrorLogger errorLogger;
    private boolean isEnabled = false;
    private boolean wasRegistered = false;

    @Inject
    public ForgeAFKListener(PlanConfig config, ErrorLogger errorLogger) {
        this.errorLogger = errorLogger;
        this.ignorePermissionInfo = new ConcurrentHashMap<>();

        ForgeAFKListener.assignAFKTracker(config);
    }

    private static void assignAFKTracker(PlanConfig config) {
        if (afkTracker == null) {
            afkTracker = new AFKTracker(config);
        }
    }

    public static AFKTracker getAfkTracker() {
        return afkTracker;
    }

    private void event(ServerPlayer player) {
        try {
            UUID uuid = player.getUUID();
            long time = System.currentTimeMillis();

            boolean ignored = ignorePermissionInfo.computeIfAbsent(uuid, keyUUID -> checkPermission(player, com.djrapitops.plan.settings.Permissions.IGNORE_AFK.getPermission()));
            if (ignored) {
                afkTracker.hasIgnorePermission(uuid);
                ignorePermissionInfo.put(uuid, true);
                return;
            } else {
                ignorePermissionInfo.put(uuid, false);
            }

            afkTracker.performedAction(uuid, time);
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), player).build());
        }
    }

    private boolean checkPermission(ServerPlayer player, String permission) {
        return false;
        //TODO: impl perms
//        if (ForgeCommandManager.isPermissionsApiAvailable()) {
//            return Permissions.check(player, permission);
//        } else {
//            return false;
//        }
    }

    @Override
    public void register() {
        if (this.wasRegistered) {
            return;
        }


        MinecraftForge.EVENT_BUS.addListener(this::onChat);

        MinecraftForge.EVENT_BUS.addListener(this::onCommand);

        MinecraftForge.EVENT_BUS.addListener(this::onDisconnect);


        MinecraftForge.EVENT_BUS.addListener(this::onMove);

        this.enable();
        this.wasRegistered = true;
    }

    public void onMove(OnMoveEvent event) {
        if (!this.isEnabled) {
            return;
        }
        event(event.serverGamePacketListener().player);
    }

    public void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!this.isEnabled) {
            return;
        }
        ignorePermissionInfo.remove(((ServerPlayer) event.getEntity()).getUUID());
    }

    public void onChat(ServerChatEvent event) {
        if (!isEnabled) {
            return;
        }
        event(event.getPlayer());
    }

    public void onCommand(OnCommandEvent event) {
        if (!isEnabled) {
            return;
        }
        event(event.serverGamePacketListener().player);
        boolean isAfkCommand = event.command().toLowerCase().startsWith("afk");
        if (isAfkCommand) {
            UUID uuid = event.serverGamePacketListener().player.getUUID();
            afkTracker.usedAfkCommand(uuid, System.currentTimeMillis());
        }
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
