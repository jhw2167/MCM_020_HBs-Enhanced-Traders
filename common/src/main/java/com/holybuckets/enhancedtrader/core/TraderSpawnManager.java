package com.holybuckets.enhancedtrader.core;

import com.holybuckets.enhancedtrader.LoggerProject;
import com.holybuckets.enhancedtrader.config.EnhancedTraderConfig;
import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.enhancedtrader.core.model.StructureTraderSite;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.PlayerNearStructureEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.structure.StructureInfo;
import io.netty.util.collection.IntObjectHashMap;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TraderSpawnManager {

    public static final String CLASS_ID = "007";
    private static final int SPAWN_ATTEMPTS = 12;
    private static final int MIN_TETHER_RADIUS = 32;

    private static final IntObjectHashMap<StructureInfo> SPAWN_SITES = new IntObjectHashMap<>();

    private TraderSpawnManager() {
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerNearStructure(null, TraderSpawnManager::onPlayerNearStructure);
        reg.registerOnServerStarted(TraderSpawnManager::onServerStarted);
        reg.registerOnDailyTick(null, TraderSpawnManager::onDailyTick);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = GeneralConfig.getInstance().getServer();
        if (server == null) return;
        server.getGameRules().getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        LoggerProject.logInfo("007002", "Vanilla wandering trader spawning disabled, enhanced traders spawn around structures instead");
    }

    private static void onDailyTick(ServerTickEvent event) {
        SPAWN_SITES.clear();
    }

    private static void onPlayerNearStructure(PlayerNearStructureEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        StructureInfo info = event.getStructureInfo();
        if (info == null || info.getOrigin() == null) return;
        ResourceLocation structureId = info.getStructureLocation();
        if (!ModConfig.getInstance().isSpawningStructure(structureId)) return;
        int id = (int) (info.getRegistryId()*info.getOrigin().asLong());
        if(SPAWN_SITES.containsKey(id)) return;
        SPAWN_SITES.put(id, info);

        //Check area for existing traders, if any are found, do not spawn a new one
        BlockPos origin = info.getOrigin();
        AABB matrix = new AABB(
        origin.getX() - MIN_TETHER_RADIUS, origin.getY() - MIN_TETHER_RADIUS, origin.getZ() - MIN_TETHER_RADIUS,
            origin.getX() + MIN_TETHER_RADIUS, origin.getY() + MIN_TETHER_RADIUS, origin.getZ() + MIN_TETHER_RADIUS
        );
        Collection<WanderingTrader> traders = level.getEntitiesOfClass(WanderingTrader.class, matrix);
        if (!traders.isEmpty()) return;

        //Spawn new trader
        BlockPos findSpawnPos = findSpawnPos(level, origin);
        if (findSpawnPos == null) return;
        Entity trader = EntityType.WANDERING_TRADER.spawn(level, findSpawnPos, MobSpawnType.EVENT);
        if (trader == null) return;
        trader.setPos(findSpawnPos.getX() + 0.5, findSpawnPos.getY(), findSpawnPos.getZ() + 0.5);
        level.addFreshEntity(trader);


    }



    @Nullable
    private static BlockPos findSpawnPos(ServerLevel level, BlockPos origin) {
        RandomSource random = level.getRandom();

        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = random.nextInt(MIN_TETHER_RADIUS/2);
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, origin.getY(), z))) continue;

            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafeSpawn(level, pos)) return pos;
        }
        return null;
    }

    private static boolean isSafeSpawn(ServerLevel level, BlockPos pos) {
        return NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND,
             level, pos, EntityType.WANDERING_TRADER);
    }


}
