package com.djrapitops.plan.system.listeners.sponge;

import com.djrapitops.plan.data.container.Session;
import com.djrapitops.plan.data.store.mutators.formatting.Formatters;
import com.djrapitops.plan.system.cache.SessionCache;
import com.djrapitops.plan.system.processing.Processing;
import com.djrapitops.plan.system.processing.processors.player.MobKillProcessor;
import com.djrapitops.plan.system.processing.processors.player.PlayerKillProcessor;
import com.djrapitops.plugin.logging.L;
import com.djrapitops.plugin.logging.error.ErrorHandler;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.animal.Wolf;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener for Deaths and Kills on Sponge.
 *
 * @author Rsl1122
 */
public class SpongeDeathListener {

    private final Processing processing;
    private ErrorHandler errorHandler;

    @Inject
    public SpongeDeathListener(
            Processing processing,
            ErrorHandler errorHandler
    ) {
        this.processing = processing;
        this.errorHandler = errorHandler;
    }

    @Listener
    public void onEntityDeath(DestructEntityEvent.Death event) {
        long time = System.currentTimeMillis();
        Living dead = event.getTargetEntity();

        if (dead instanceof Player) {
            // Process Death
            SessionCache.getCachedSession(dead.getUniqueId()).ifPresent(Session::died);
        }

        try {
            Optional<EntityDamageSource> optDamageSource = event.getCause().first(EntityDamageSource.class);
            if (optDamageSource.isPresent()) {
                EntityDamageSource damageSource = optDamageSource.get();
                Entity killerEntity = damageSource.getSource();
                handleKill(time, dead, killerEntity);
            }
        } catch (Exception e) {
            errorHandler.log(L.ERROR, this.getClass(), e);
        }
    }

    private void handleKill(long time, Living dead, Entity killerEntity) {
        Runnable processor = null;
        UUID victimUUID = getUUID(dead);
        if (killerEntity instanceof Player) {
            processor = handlePlayerKill(time, victimUUID, (Player) killerEntity);
        } else if (killerEntity instanceof Wolf) {
            processor = handleWolfKill(time, victimUUID, (Wolf) killerEntity);
        } else if (killerEntity instanceof Projectile) {
            processor = handleProjectileKill(time, victimUUID, (Projectile) killerEntity);
        }
        if (processor != null) {
            processing.submit(processor);
        }
    }

    private Runnable handlePlayerKill(long time, UUID victimUUID, Player killer) {

        Optional<ItemStack> inMainHand = killer.getItemInHand(HandTypes.MAIN_HAND);
        ItemStack inHand = inMainHand.orElse(killer.getItemInHand(HandTypes.OFF_HAND).orElse(ItemStack.empty()));
        ItemType type = inHand.isEmpty() ? ItemTypes.AIR : inHand.getType();

        return victimUUID != null
                ? new PlayerKillProcessor(killer.getUniqueId(), time, victimUUID, Formatters.itemName().apply(type.getName()))
                : new MobKillProcessor(killer.getUniqueId());
    }

    private UUID getUUID(Living dead) {
        if (dead instanceof Player) {
            return dead.getUniqueId();
        }
        return null;
    }

    private Runnable handleWolfKill(long time, UUID victimUUID, Wolf wolf) {
        Optional<Optional<UUID>> owner = wolf.get(Keys.TAMED_OWNER);

        // Has been tamed
        return owner.map(ownerUUID ->
                // Has tame owner
                ownerUUID.map(uuid ->
                        // Player or mob
                        victimUUID != null
                                ? new PlayerKillProcessor(uuid, time, victimUUID, "Wolf")
                                : new MobKillProcessor(uuid)
                ).orElse(null)).orElse(null);

    }

    private Runnable handleProjectileKill(long time, UUID victimUUID, Projectile projectile) {
        ProjectileSource source = projectile.getShooter();
        if (!(source instanceof Player)) {
            return null;
        }

        Player player = (Player) source;
        String projectileName = Formatters.entityName().apply(projectile.getType().getName());

        return victimUUID != null
                ? new PlayerKillProcessor(player.getUniqueId(), time, victimUUID, projectileName)
                : new MobKillProcessor(player.getUniqueId());
    }

}