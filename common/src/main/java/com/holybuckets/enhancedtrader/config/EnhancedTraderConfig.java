package com.holybuckets.enhancedtrader.config;

import com.holybuckets.enhancedtrader.Constants;
import net.blay09.mods.balm.api.config.reflection.Comment;
import net.blay09.mods.balm.api.config.reflection.Config;
import net.blay09.mods.balm.api.config.reflection.NestedType;
import net.blay09.mods.balm.api.config.reflection.Synced;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(Constants.MOD_ID)
public class EnhancedTraderConfig {

    public static final String DEF_TRADER_CONFIG_PATH = "config/HBEnhancedTradersConfig.json";
    public static final String DEF_BASE_TRADE_ITEM = "minecraft:iron_nugget";
    public static final int DEF_MARKET_RATE = 8;

    @Comment("File path to the .json file where trade categories, their trades, weights, biomes and markup are configured")
    public String traderConfigFile = DEF_TRADER_CONFIG_PATH;

    @Comment("Item used as currency for every enhanced trader trade")
    @Synced
    public String baseTradeItem = DEF_BASE_TRADE_ITEM;

    @Comment("Default market rate for items in the trade store. Items costing less than the market rate give you more items per purchase. A good number here is 8.")
    @Synced
    public int marketRate = DEF_MARKET_RATE;

    @Comment("Default markup price for trades. You may set a different markup per Trade Category using the JSON config. Default: 1.5")
    @Synced
    public float defaultMarkup = 1.5f;

    @Comment("Maximum number of trade categories a trader can have. Each trade category has several trades according to the config. Default: 5")
    @Synced
    public int maxTradeCategories = 5;

    @Comment("Minimum number of trade categories a trader can have. Each trade category has several trades according to the config. Default: 5")
    @Synced
    public int minTradeCategories = 2;

    @Comment("Average amount of wares a trader has per trade. Default: 5")
    @Synced
    public int tradeStockMean = 5;

    @Comment("Standard deviation of the amount of wares a trader has per trade. Default: 1")
    @Synced
    public int tradeStockStdDev = 2;

    @Comment("A wandering trader tries to spawn at one of the below structures once per day with this chance. 0 to 1. Default: 0.35")
    @Synced
    public float spawnChance = 0.35f;


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

}
