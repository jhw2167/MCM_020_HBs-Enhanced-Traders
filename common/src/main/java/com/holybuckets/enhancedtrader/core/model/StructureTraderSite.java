package com.holybuckets.enhancedtrader.core.model;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class StructureTraderSite {

    private final ResourceLocation structureId;
    private final BlockPos origin;
    private final Set<UUID> traders = new LinkedHashSet<>();
    private long nextSpawnTime = 0L;

    public StructureTraderSite(ResourceLocation structureId, BlockPos origin) {
        this.structureId = structureId;
        this.origin = origin.immutable();
    }

    public ResourceLocation getStructureId() { return structureId; }

    public BlockPos getOrigin() { return origin; }

    public long getKey() { return origin.asLong(); }

    public Set<UUID> getTraders() { return Collections.unmodifiableSet(traders); }

    public long getNextSpawnTime() { return nextSpawnTime; }

    public void addTrader(UUID id) {
        if (id != null) traders.add(id);
    }

    public void removeTrader(UUID id) {
        traders.remove(id);
    }

    public void pruneTraders(Predicate<UUID> isGone) {
        traders.removeIf(isGone);
    }

    public boolean canSpawn(long gameTime, int maxTraders) {
        return traders.size() < maxTraders && gameTime >= nextSpawnTime;
    }

    public void setNextSpawnTime(long nextSpawnTime) {
        this.nextSpawnTime = nextSpawnTime;
    }

    public void deferSpawnUntil(long gameTime) {
        this.nextSpawnTime = Math.max(this.nextSpawnTime, gameTime);
    }
}
