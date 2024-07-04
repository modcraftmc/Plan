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

import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.common.MinecraftForge;
import net.playeranalytics.plan.gathering.listeners.events.PlanForgeEvents;
import net.playeranalytics.plan.gathering.listeners.events.impl.OnCommandEvent;
import net.playeranalytics.plan.gathering.listeners.events.impl.OnMoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {

    @Inject(method = "handleChatCommand", at = @At("TAIL"))
    public void onCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new OnCommandEvent((ServerGamePacketListenerImpl) (Object) this, packet.command()));
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getLevel()Lnet/minecraft/server/level/ServerLevel;"))
    public void onPlayerMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new OnMoveEvent((ServerGamePacketListenerImpl) (Object) this, packet));
    }

}
