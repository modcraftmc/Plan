package net.playeranalytics.plan.gathering.listeners.events.impl;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.eventbus.api.Event;

public class OnCommandEvent extends Event {

    private ServerGamePacketListenerImpl serverGamePacketListener;
    private String command;

    public OnCommandEvent(ServerGamePacketListenerImpl serverGamePacketListener, String command) {
        this.serverGamePacketListener = serverGamePacketListener;
        this.command = command;
    }

    public ServerGamePacketListenerImpl serverGamePacketListener() {
        return serverGamePacketListener;
    }

    public String command() {
        return command;
    }
}
