package net.playeranalytics.plan.gathering.listeners.events.impl;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraftforge.eventbus.api.Event;

public class OnHandshakeEvent extends Event {

    private ClientIntentionPacket packet;
    public OnHandshakeEvent(final ClientIntentionPacket packet) {
        this.packet = packet;
    }

    public ClientIntentionPacket packet() {
        return packet;
    }
}
