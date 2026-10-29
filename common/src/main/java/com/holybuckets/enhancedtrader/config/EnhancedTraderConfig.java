package com.holybuckets.enhancedtrader.config;

import com.holybuckets.enhancedtrader.Constants;
import net.blay09.mods.balm.api.config.reflection.Comment;
import net.blay09.mods.balm.api.config.reflection.Config;
import net.blay09.mods.balm.api.config.reflection.NestedType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(Constants.MOD_ID)
public class EnhancedTraderConfig {

    public static final String DEF_TRADER_CONFIG_PATH = "config/HBEnhancedTradersConfig.json";
    public static final String DEF_BASE_TRADE_ITEM = "minecraft:iron_nugget";
    public static final int DEF_MARKET_RATE = 8;

    @Comment("devMode==true logs trader spawns, category rolls and config hydration results")
    public boolean devMode = false;

    @Comment("File path to the .json file where trade categories, their trades, weights, biomes and markup are configured")
    public String traderConfigFile = DEF_TRADER_CONFIG_PATH;

    @Comment("Item used as currency for every enhanced trader trade")
    public String baseTradeItem = DEF_BASE_TRADE_ITEM;

    @Comment("Default market rate for items in the trade store. Items costing less than the market rate give you more items per purchase. A good number here is 8.")
    public int marketRate = DEF_MARKET_RATE;

    @Comment("Structures that traders spawn around.")
    @NestedType(String.class)
    public List<String> spawningStructures = new ArrayList<>(Arrays.asList(
        "minecraft:village_plains",
        "minecraft:village_desert",
        "minecraft:village_savanna",
        "minecraft:village_snowy",
        "minecraft:village_taiga",
        "minecraft:desert_pyramid",
        "minecraft:jungle_pyramid",
        "minecraft:igloo",
        "minecraft:trail_ruins",
        "minecraft:ruined_portal"
    ));

    public static class SpawnConfigs {

        @Comment("Chance (0 to 1) a trader spawns when a player is near an eligible structure whose spawn cooldown has elapsed")
        public float spawnChance = 0.35f;

        @Comment("Ticks a structure waits after each spawn attempt before it may attempt again")
        public int spawnCooldownTicks = 24000;

        @Comment("Maximum number of enhanced traders maintained around a single structure")
        public int maxTradersPerStructure = 1;

        @Comment("Minimum horizontal distance in blocks from the structure origin a trader may spawn")
        public int minSpawnDistance = 8;

        @Comment("Maximum horizontal distance in blocks from the structure origin a trader may spawn")
        public int maxSpawnDistance = 24;

        @Comment("Traders that wander further than this many blocks from their structure are walked back toward it")
        public int tetherRadius = 32;

        @Comment("Ticks a trader remains in the world before despawning. Vanilla wandering traders use 48000")
        public int traderLifetimeTicks = 48000;

        @Comment("true sets the doTraderSpawning gamerule to false on server start so vanilla traders stop spawning at the player's base")
        public boolean disableVanillaTraderSpawns = true;
    }

    public SpawnConfigs spawnConfigs = new SpawnConfigs();

    public static class TradeConfigs {

        @Comment("Maximum stack size accepted by the trader screen input slot")
        public int tradeSlotCapacity = 999;
    }

    public TradeConfigs tradeConfigs = new TradeConfigs();
}
