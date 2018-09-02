package com.djrapitops.plan.system.processing.processors.player;

import com.djrapitops.plan.data.store.objects.DateObj;
import com.djrapitops.plan.system.cache.DataCache;
import com.djrapitops.plan.system.database.databases.Database;
import com.djrapitops.plan.system.processing.Processing;
import dagger.Lazy;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Factory for creating Runnables related to Player data to run with {@link com.djrapitops.plan.system.processing.Processing}.
 *
 * @author Rsl1122
 */
@Singleton
public class PlayerProcessors {

    private final Lazy<Processing> processing;
    private final Lazy<Database> database;
    private final Lazy<DataCache> dataCache;

    @Inject
    public PlayerProcessors(
            Lazy<Processing> processing,
            Lazy<Database> database,
            Lazy<DataCache> dataCache
    ) {
        this.processing = processing;
        this.database = database;
        this.dataCache = dataCache;
    }

    public BanAndOpProcessor banAndOpProcessor(UUID uuid, Supplier<Boolean> banned, boolean op) {
        return new BanAndOpProcessor(uuid, banned, op, database.get());
    }

    public BungeeRegisterProcessor bungeeRegisterProcessor(UUID uuid, String name, long registered, Runnable... afterProcess) {
        return new BungeeRegisterProcessor(uuid, name, registered, processing.get(), database.get(), afterProcess);
    }

    public EndSessionProcessor endSessionProcessor(UUID uuid, long time) {
        return new EndSessionProcessor(uuid, time, dataCache.get());
    }

    public IPUpdateProcessor ipUpdateProcessor(UUID uuid, InetAddress ip, long time) {
        return new IPUpdateProcessor(uuid, ip, time, database.get());
    }

    public KickProcessor kickProcessor(UUID uuid) {
        return new KickProcessor(uuid, database.get());
    }

    public NameProcessor nameProcessor(UUID uuid, String playerName, String displayName) {
        return new NameProcessor(uuid, playerName, displayName, database.get(), dataCache.get());
    }

    public PingInsertProcessor pingInsertProcessor(UUID uuid, List<DateObj<Integer>> pingList) {
        return new PingInsertProcessor(uuid, pingList, database.get());
    }

    public RegisterProcessor registerProcessor(UUID uuid, Supplier<Long> registered, String name, Runnable... afterProcess) {
        return new RegisterProcessor(uuid, registered, name, processing.get(), database.get(), afterProcess);
    }

}