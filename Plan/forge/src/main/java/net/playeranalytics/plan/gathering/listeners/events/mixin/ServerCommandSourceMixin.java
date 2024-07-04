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
package net.playeranalytics.plan.gathering.listeners.events.mixin;

import com.djrapitops.plan.commands.use.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.playeranalytics.plan.commands.ForgeCommandManager;
import net.playeranalytics.plan.commands.use.ForgeMessageBuilder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.UUID;

@Mixin(CommandSourceStack.class)
public abstract class ServerCommandSourceMixin implements CMDSender {

    @Override
    public boolean isPlayer() {
        return getPlayer().isPresent();
    }

    @Override
    public boolean supportsChatEvents() {
        return isPlayer();
    }

    @Shadow
    public abstract void sendSuccess(Component supplier, boolean broadcastToOps);

    @Shadow
    @Nullable
    public abstract Entity getEntity();

    @Override
    public MessageBuilder buildMessage() {
        return new ForgeMessageBuilder((CommandSourceStack) (Object) this);
    }

    @Override
    public Optional<String> getPlayerName() {
        return getPlayer().map(ServerPlayer::getGameProfile).map(GameProfile::getName);
    }

    @Override
    public boolean hasPermission(String permission) {
        return ForgeCommandManager.checkPermission((CommandSourceStack) (Object) this, permission);
    }

    @Override
    public Optional<UUID> getUUID() {
        return getPlayer().map(Entity::getUUID);
    }

    @Override
    public void send(String message) {
        this.sendSuccess(Component.literal(message), false);
    }

    @Override
    public ChatFormatter getFormatter() {
        return isConsole() ? new ConsoleChatFormatter() : new PlayerChatFormatter();
    }

    private boolean isConsole() {
        return getEntity() == null;
    }

    private Optional<ServerPlayer> getPlayer() {
        if (getEntity() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(isConsole()) + getUUID().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServerCommandSourceMixin other)) return false;

        return isConsole() == other.isConsole()
                && getUUID().equals(other.getUUID());
    }
}
