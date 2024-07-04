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
package net.playeranalytics.plan;

import com.djrapitops.plan.PlanPlugin;
import com.djrapitops.plan.PlanSystem;
import com.djrapitops.plan.commands.use.ColorScheme;
import com.djrapitops.plan.commands.use.Subcommand;
import com.djrapitops.plan.exceptions.EnableException;
import com.djrapitops.plan.gathering.ServerShutdownSave;
import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.settings.locale.lang.PluginLang;
import com.djrapitops.plan.settings.theme.PlanColorScheme;
import com.djrapitops.plan.utilities.java.ThreadContextClassLoaderSwap;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.playeranalytics.plan.commands.ForgeCommandManager;
import net.playeranalytics.plan.identification.properties.ForgeServerProperties;
import net.playeranalytics.plugin.ForgePlatformLayer;
import net.playeranalytics.plugin.PlatformAbstractionLayer;
import net.playeranalytics.plugin.scheduling.RunnableFactory;
import net.playeranalytics.plugin.server.ForgePluginLogger;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main class for Plan's Fabric version.
 *
 * @author Kopo942
 */
@Mod("planforge")
public class PlanForge implements PlanPlugin {

    private MinecraftServer server;
    private ForgeCommandManager fabricCommandManager;

    private PlanSystem system;
    private Locale locale;
    private ServerShutdownSave serverShutdownSave;

    private ForgePluginLogger pluginLogger;
    private RunnableFactory runnableFactory;
    private PlatformAbstractionLayer abstractionLayer;
    private ErrorLogger errorLogger;

    @Override
    public InputStream getResource(String resource) {
        return this.getClass().getResourceAsStream("/" + resource);
    }

    @Override
    public ColorScheme getColorScheme() {
        return PlanColorScheme.create(system.getConfigSystem().getConfig(), pluginLogger);
    }

    @Override
    public PlanSystem getSystem() {
        return system;
    }

    @Override
    public void registerCommand(Subcommand command) {
        fabricCommandManager.registerRoot(command, runnableFactory);
    }

    @Override
    public void onEnable() {
        PlanForgeComponent component = DaggerPlanForgeComponent.builder()
                .plan(this)
                .abstractionLayer(abstractionLayer)
                .server(server)
                .serverProperties(new ForgeServerProperties(getServer()))
                .build();

        try {
            system = ThreadContextClassLoaderSwap.performOperation(getClass().getClassLoader(), component::system);
            errorLogger = component.errorLogger();
            serverShutdownSave = component.serverShutdownSave();
            locale = system.getLocaleSystem().getLocale();
            registerCommand(component.planCommand().build());
            system.enable();

            pluginLogger.info(locale.getString(PluginLang.ENABLED));
        } catch (AbstractMethodError e) {
            pluginLogger.error("Plugin ran into AbstractMethodError, server restart is required! This error is likely caused by updating the JAR without a restart.");
        } catch (EnableException e) {
            pluginLogger.error("----------------------------------------");
            pluginLogger.error("Error: " + e.getMessage());
            pluginLogger.error("----------------------------------------");
            pluginLogger.error("Plugin failed to initialize correctly. If this issue is caused by config settings you can use /plan reload");
            onDisable();
        } catch (Exception e) {
            String version = abstractionLayer.getPluginInformation().getVersion();
            pluginLogger.error(this.getClass().getSimpleName() + "-v" + version, e);
            pluginLogger.error("Plugin Failed to Initialize Correctly. If this issue is caused by config settings you can use /plan reload");
            pluginLogger.error("This error should be reported at https://github.com/plan-player-analytics/Plan/issues");
            onDisable();
        }
        if (system != null) {
            system.getProcessing().submitNonCritical(() -> system.getListenerSystem().callEnableEvent(this));
        }
    }

    @Override
    public void onDisable() {
        storeSessionsOnShutdown();
        runnableFactory.cancelAllKnownTasks();

        if (system != null) system.disable();

        pluginLogger.info(Locale.getStringNullSafe(locale, PluginLang.DISABLED));
    }

    private void storeSessionsOnShutdown() {
        if (serverShutdownSave != null) {
            Optional<Future<?>> complete = serverShutdownSave.performSave();
            if (complete.isPresent()) {
                try {
                    complete.get().get(4, TimeUnit.SECONDS); // wait for completion for 4s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    pluginLogger.error("Failed to save sessions to database on shutdown: " + e.getCause().getMessage());
                } catch (TimeoutException e) {
                    pluginLogger.info(Locale.getStringNullSafe(locale, PluginLang.DISABLED_UNSAVED_SESSIONS_TIMEOUT));
                }
            }
        }
    }

    @Override
    public File getDataFolder() {
        return FMLPaths.CONFIGDIR.get().resolve("plan").toFile();
    }

    public PlanForge() {
        abstractionLayer = new ForgePlatformLayer(this);
        pluginLogger = (ForgePluginLogger) abstractionLayer.getPluginLogger();
        runnableFactory = abstractionLayer.getRunnableFactory();

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerSopping);

        MinecraftForge.EVENT_BUS.addListener(this::onCommandRegister);

       // CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> fabricCommandManager = new FabricCommandManager(dispatcher, this, errorLogger));

       // ServerLifecycleEvents.SERVER_STOPPING.register(server -> onDisable());
    }

    public void onServerStarting(ServerAboutToStartEvent event) {
        this.server = event.getServer();
        onEnable();
    }

    public void onServerSopping(ServerStoppedEvent event) {
        this.onDisable();
    }

    public void onCommandRegister(RegisterCommandsEvent event) {
        fabricCommandManager = new ForgeCommandManager(event.getDispatcher(), this, errorLogger);
    }

    public MinecraftServer getServer() {
        return server;
    }
}
