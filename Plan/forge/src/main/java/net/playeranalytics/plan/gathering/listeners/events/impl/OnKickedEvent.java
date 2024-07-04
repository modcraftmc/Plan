package net.playeranalytics.plan.gathering.listeners.events.impl;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collection;

public class OnKickedEvent extends Event {
    CommandSourceStack source;
    Collection<ServerPlayer> targets;
    Component reason;

    public OnKickedEvent(CommandSourceStack source, Collection<ServerPlayer> targets, Component reason) {
        this.source = source;
        this.targets = targets;
        this.reason = reason;
    }

    public CommandSourceStack source() {
        return source;
    }

    public Collection<ServerPlayer> targets() {
        return targets;
    }

    public Component reason() {
        return reason;
    }
}
