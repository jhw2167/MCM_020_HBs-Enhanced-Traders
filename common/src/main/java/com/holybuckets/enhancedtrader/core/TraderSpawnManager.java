package com.holybuckets.enhancedtrader.core;

import com.holybuckets.enhancedtrader.LoggerProject;
import com.holybuckets.enhancedtrader.config.EnhancedTraderConfig;
import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.enhancedtrader.core.model.StructureTraderSite;
import com.holybuckets.enhancedtrader.entity.EnhancedTraderEntity;
import com.holybuckets.enhancedtrader.entity.TraderEntities;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.PlayerNearStructureEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.structure.StructureInfo;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TraderSpawnManager {

    public static final String CLASS_ID = "007";
    private static final int SPAWN_ATTEMPTS = 12;
    private static final int MIN_TETHER_RADIUS = 4;

    private static final Map<ResourceKey<Level>, Map<Long, StructureTraderSite>> SITES = new HashMap<>();

    private TraderSpawnManager() {
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerNearStructure(null, TraderSpawnManager::onPlayerNearStructure);
        reg.registerOnServerTick(TickType.ON_120_TICKS, TraderSpawnManager::on120Ticks);
        reg.registerOnServerStarted(TraderSpawnManager::onServerStarted);
        reg.registerOnServerStopped(TraderSpawnManager::onServerStopped);
    }

    public static Collection<StructureTraderSite> getSites(ServerLevel level) {
        Map<Long, StructureTraderSite> sites = SITES.get(level.dimension());
        return (sites == null) ? Collections.emptyList() : Collections.unmodifiableCollection(sites.values());
    }

    @Nullable
    public static StructureTraderSite getSite(ServerLevel level, BlockPos origin) {
        Map<Long, StructureTraderSite> sites = SITES.get(level.dimension());
        return (sites == null) ? null : sites.get(origin.asLong());
    }

    private static StructureTraderSite getOrCreateSite(ServerLevel level, ResourceLocation structureId, BlockPos origin) {
        return SITES.computeIfAbsent(level.dimension(), k -> new HashMap<>())
            .computeIfAbsent(origin.asLong(), k -> new StructureTraderSite(structureId, origin));
    }


    private static void onPlayerNearStructure(PlayerNearStructureEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        StructureInfo info = event.getStructureInfo();
        if (info == null || info.getOrigin() == null) return;
        ResourceLocation structureId = info.getStructureLocation();
        if (!ModConfig.getInstance().isSpawningStructure(structureId)) return;

        StructureTraderSite site = getOrCreateSite(level, structureId, info.getOrigin());
        pruneSite(level, site);

        EnhancedTraderConfig.SpawnConfigs cfg = ModConfig.getSpawnConfigs();
        long gameTime = level.getGameTime();
        if (!site.canSpawn(gameTime, Math.max(0, cfg.maxTradersPerStructure))) return;

        site.setNextSpawnTime(gameTime + Math.max(0, cfg.spawnCooldownTicks));
        if (level.getRandom().nextFloat() >= cfg.spawnChance) return;

        EnhancedTraderEntity trader = spawnTrader(level, site);
        if (trader != null && ModConfig.isDevMode()) {
            LoggerProject.logDebug("007001", "Spawned enhanced trader " + trader.getUUID() + " at " + trader.blockPosition()
                + " for " + structureId + " at " + site.getOrigin() + " with categories " + trader.getCategoryIds());
        }
    }

    private static void on120Ticks(ServerTickEvent event) {
        MinecraftServer server = GeneralConfig.getInstance().getServer();
        if (server == null) return;

        for (Map.Entry<ResourceKey<Level>, Map<Long, StructureTraderSite>> entry : SITES.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) continue;
            for (StructureTraderSite site : entry.getValue().values()) {
                pruneSite(level, site);
                tetherTraders(level, site);
            }
        }
    }

    private static void onServerStarted(ServerStartedEvent event) {
        if (!ModConfig.getSpawnConfigs().disableVanillaTraderSpawns) return;
        MinecraftServer server = GeneralConfig.getInstance().getServer();
        if (server == null) return;
        server.getGameRules().getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        LoggerProject.logInfo("007002", "Vanilla wandering trader spawning disabled, enhanced traders spawn around structures instead");
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        SITES.clear();
    }


    public static void traderTicked(EnhancedTraderEntity trader) {
        if (!trader.hasHome() || !(trader.level() instanceof ServerLevel level)) return;
        StructureTraderSite site = getOrCreateSite(level, trader.getHomeStructure(), trader.getHomeOrigin());
        site.addTrader(trader.getUUID());
        site.deferSpawnUntil(trader.getSpawnGameTime() + Math.max(0, ModConfig.getSpawnConfigs().spawnCooldownTicks));
    }

    public static void traderRemoved(EnhancedTraderEntity trader, Entity.RemovalReason reason) {
        if (reason == null || !reason.shouldDestroy()) return;
        if (!trader.hasHome() || !(trader.level() instanceof ServerLevel level)) return;
        StructureTraderSite site = getSite(level, trader.getHomeOrigin());
        if (site != null) site.removeTrader(trader.getUUID());
    }

    @Nullable
    public static EnhancedTraderEntity spawnTrader(ServerLevel level, StructureTraderSite site) {
        BlockPos pos = findSpawnPos(level, site.getOrigin());
        if (pos == null) {
            LoggerProject.logWarning("007003", "No safe spawn position for an enhanced trader near " + site.getStructureId() + " at " + site.getOrigin());
            return null;
        }

        EnhancedTraderEntity trader = TraderEntities.enhancedTrader.get().create(level);
        if (trader == null) return null;

        RandomSource random = level.getRandom();
        trader.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        trader.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        trader.setHome(site.getStructureId(), site.getOrigin());
        trader.setDespawnDelay(Math.max(1, ModConfig.getSpawnConfigs().traderLifetimeTicks));
        trader.initializeTrades(level.getBiome(pos), random);

        if (!trader.hasTrades() || !level.addFreshEntity(trader)) return null;
        site.addTrader(trader.getUUID());
        return trader;
    }

    @Nullable
    private static BlockPos findSpawnPos(ServerLevel level, BlockPos origin) {
        EnhancedTraderConfig.SpawnConfigs cfg = ModConfig.getSpawnConfigs();
        int min = Math.max(0, cfg.minSpawnDistance);
        int max = Math.max(min, cfg.maxSpawnDistance);
        RandomSource random = level.getRandom();

        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = min + random.nextInt(max - min + 1);
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, origin.getY(), z))) continue;

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafeSpawn(level, pos)) return pos;
        }
        return null;
    }

    private static boolean isSafeSpawn(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP)) return false;
        if (!level.getFluidState(below).isEmpty()) return false;

        return isOpenSpace(level, pos) && isOpenSpace(level, pos.above());
    }

    private static boolean isOpenSpace(ServerLevel level, BlockPos pos) {
        return NaturalSpawner.isValidEmptySpawnBlock(level, pos, level.getBlockState(pos),
            level.getFluidState(pos), TraderEntities.enhancedTrader.get());
    }

    private static void pruneSite(ServerLevel level, StructureTraderSite site) {
        if (!level.hasChunkAt(site.getOrigin())) return;
        site.pruneTraders(id -> {
            Entity entity = level.getEntity(id);
            return entity == null || !entity.isAlive();
        });
    }

    private static void tetherTraders(ServerLevel level, StructureTraderSite site) {
        int tether = Math.max(MIN_TETHER_RADIUS, ModConfig.getSpawnConfigs().tetherRadius);
        double tetherSq = (double) tether * tether;
        BlockPos origin = site.getOrigin();

        for (UUID id : site.getTraders()) {
            if (!(level.getEntity(id) instanceof EnhancedTraderEntity trader)) continue;
            double dx = trader.getX() - origin.getX();
            double dz = trader.getZ() - origin.getZ();
            double distSq = dx * dx + dz * dz;

            if (distSq > tetherSq) {
                trader.setWanderTarget(origin);
            } else if (distSq < tetherSq / 4.0 && origin.equals(trader.getWanderTarget())) {
                trader.setWanderTarget(null);
            }
        }
    }
}
