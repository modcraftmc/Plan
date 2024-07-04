package net.playeranalytics.plan.gathering.listeners.events.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.eventbus.api.Event;

public class OnGamemodeChangeEvent extends Event {

    private ServerPlayer serverPlayer;
    private GameType gameType;

    public OnGamemodeChangeEvent(ServerPlayer serverPlayer, GameType gameType) {
        this.serverPlayer = serverPlayer;
        this.gameType = gameType;
    }

    public ServerPlayer serverPlayer() {
        return serverPlayer;
    }

    public GameType gameType() {
        return gameType;
    }
}
