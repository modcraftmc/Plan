package net.playeranalytics.plan.gathering.listeners.events.impl;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

public class OnKilledEvent extends Event {

    private Entity killed;
    private Entity killer;

    public OnKilledEvent(Entity killed, Entity killer) {
        this.killed = killed;
        this.killer = killer;
    }

    public Entity killed() {
        return killed;
    }

    public Entity killer() {
        return killer;
    }
}
