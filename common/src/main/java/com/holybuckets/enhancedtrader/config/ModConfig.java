package com.holybuckets.enhancedtrader.config;

import com.holybuckets.enhancedtrader.EnhancedTraderMain;
import com.holybuckets.enhancedtrader.LoggerProject;
import com.holybuckets.enhancedtrader.config.model.TradeCategory;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ModConfig {

    private static final String CLASS_ID = "002";

    private static ModConfig INSTANCE;

    private EnhancedTraderJsonConfig traderConfig = new EnhancedTraderJsonConfig();
    private final Set<ResourceLocation> spawningStructureLocs = new HashSet<>();
    private final Set<Structure> spawningStructures = new HashSet<>();
    private final Map<Structure, ResourceLocation> structureToLocationMap = new HashMap<>();

    private Registry<Structure> structureRegistry;
    private Item baseTradeItem;

    private static GeneralConfig CONFIG;
    private static EnhancedTraderConfig ACTIVE_CONFIG;

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        return INSTANCE;
    }

    private ModConfig() {
    }

    public static void init(EventRegistrar registrar) {
        INSTANCE = ModConfig.getInstance();
        registrar.registerOnServerStopped(ModConfig::onServerStopped, EventPriority.Low);
    }

    public EnhancedTraderJsonConfig getTraderConfig() {
        return traderConfig;
    }

    public Item getBaseTradeItem() {
        return baseTradeItem;
    }

    public float getMarkup(@Nullable TradeCategory category) {
        Float markup = (category == null) ? null : category.getMarkup();
        return (markup != null) ? markup : traderConfig.getMarkup();
    }

    public int getMaxStackSize() {
        return 64;
    }

    public Collection<TradeCategory> getCategories() {
        return traderConfig.getCategories();
    }

    @Nullable
    public TradeCategory getCategory(String id) {
        return traderConfig.getCategory(id);
    }

    public Set<ResourceLocation> getSpawningStructureLocs() {
        return Collections.unmodifiableSet(spawningStructureLocs);
    }

    public Set<Structure> getSpawningStructures() {
        return Collections.unmodifiableSet(spawningStructures);
    }

    public boolean isSpawningStructure(@Nullable ResourceLocation structureLoc) {
        return structureLoc != null && spawningStructureLocs.contains(structureLoc);
    }

    public boolean isSpawningStructure(@Nullable Structure structure) {
        return structure != null && spawningStructures.contains(structure);
    }

    @Nullable
    public ResourceLocation getStructureLoc(Structure structure) {
        return structureToLocationMap.get(structure);
    }

    @Nullable
    public Registry<Structure> getStructureRegistry() {
        return structureRegistry;
    }

    @Nullable
    public static ResourceLocation parseLocation(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new ResourceLocation(raw.trim().toLowerCase(Locale.ROOT));
        } catch (ResourceLocationException e) {
            return null;
        }
    }


    // ** EVENTS
    public static void configureOnServerStart(EnhancedTraderConfig config)
    {
        ACTIVE_CONFIG = config;
        CONFIG = GeneralConfig.getInstance();
        INSTANCE.loadTraderConfig(config);
        INSTANCE.hydrateSpawningStructures(config);
        int configured = (config.spawningStructures == null) ? 0 : config.spawningStructures.size();
        LoggerProject.logInfo("002001", "Enhanced traders config loaded: "
            + INSTANCE.traderConfig.getCategories().size() + " trade categories, "
            + INSTANCE.spawningStructureLocs.size() + " of " + configured + " spawning structures");
    }


    private void loadTraderConfig(EnhancedTraderConfig activeConfig) {
        File configFile = new File(activeConfig.traderConfigFile);
        File defaultConfigFile = new File(EnhancedTraderJsonConfig.DEF_CONFIG_FILE_PATH);

        LoggerProject.logInfo("002002", "Loading enhanced trader config from: " + configFile.getAbsolutePath());

        String json = HBUtil.FileIO.loadJsonConfigs(
            configFile,
            defaultConfigFile,
            EnhancedTraderJsonConfig.buildDefaultConfig()
        );

        try {
            this.traderConfig = new EnhancedTraderJsonConfig(json);
        } catch (RuntimeException e) {
            String msg = String.format("Failed to parse user trader config JSON: %s. Error:\n %s.\n\n Default configs will be applied",
                configFile.getAbsolutePath(), e.getCause());
            LoggerProject.logError("002003", msg);
            this.traderConfig = EnhancedTraderJsonConfig.buildDefaultConfig();
        }
    }


    private void hydrateSpawningStructures(EnhancedTraderConfig activeConfig)
    {
        spawningStructureLocs.clear();
        spawningStructures.clear();
        structureToLocationMap.clear();

        List<String> configured = activeConfig.spawningStructures;
        if (configured == null || configured.isEmpty()) return;

        for (String structId : configured)
        {
            if (structId == null || structId.isBlank()) continue;

            ResourceLocation loc = HBUtil.LevelUtil.toStructureLocation(structId);
            Structure structure = HBUtil.LevelUtil.getStructure(loc);
            if(structure == null) {
                LoggerProject.logError("002011", "Failed to find structure for: " + structId);
                continue;
            }
            spawningStructureLocs.add(loc);
            spawningStructures.add(structure);
            structureToLocationMap.put(structure, loc);
        }

        LoggerProject.logDebug("002012", "Spawning structures: " + spawningStructureLocs);

    }

    private static void onServerStopped(ServerStoppedEvent event) {
        getInstance().onServerStopped();
    }

    private void onServerStopped() {
        structureRegistry = null;
        spawningStructureLocs.clear();
        spawningStructures.clear();
        structureToLocationMap.clear();
        INSTANCE = null;
    }


}
