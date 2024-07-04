package net.playeranalytics.plan.gathering.listeners.events.impl;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.eventbus.api.Event;

public class OnMoveEvent extends Event {

    private ServerGamePacketListenerImpl serverGamePacketListener;
    private ServerboundMovePlayerPacket packet;

    public OnMoveEvent(ServerGamePacketListenerImpl serverGamePacketListener, ServerboundMovePlayerPacket packet) {
        this.serverGamePacketListener = serverGamePacketListener;
        this.packet = packet;
    }

    public ServerGamePacketListenerImpl serverGamePacketListener() {
        return serverGamePacketListener;
    }

    public ServerboundMovePlayerPacket packet() {
        return packet;
    }
}
