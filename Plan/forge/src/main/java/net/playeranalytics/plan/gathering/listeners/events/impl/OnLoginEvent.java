package net.playeranalytics.plan.gathering.listeners.events.impl;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.Event;

import java.net.SocketAddress;

public class OnLoginEvent extends Event {
    private SocketAddress address;
    private GameProfile gameProfile;
    private Component component;

    public OnLoginEvent(SocketAddress address, GameProfile gameProfile, Component component) {
        this.address = address;
        this.gameProfile = gameProfile;
        this.component = component;
    }

    public SocketAddress address() {
        return address;
    }

    public GameProfile gameProfile() {
        return gameProfile;
    }

    public Component component() {
        return component;
    }
}
