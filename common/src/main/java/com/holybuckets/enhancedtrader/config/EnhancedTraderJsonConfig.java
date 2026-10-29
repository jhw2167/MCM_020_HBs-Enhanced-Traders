package com.holybuckets.enhancedtrader.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.holybuckets.enhancedtrader.Constants;
import com.holybuckets.enhancedtrader.LoggerProject;
import com.holybuckets.enhancedtrader.config.model.TradeCategory;
import com.holybuckets.enhancedtrader.config.model.TradeEntry;
import com.holybuckets.foundation.modelInterface.IStringSerializable;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnhancedTraderJsonConfig implements IStringSerializable {

    public static final String CLASS_ID = "003";
    public static final String DEF_CONFIG_FILE_PATH = EnhancedTraderConfig.DEF_TRADER_CONFIG_PATH;
    public static final String DEFAULT_CONFIG_RESOURCE = "/assets/" + Constants.MOD_ID + "/config/default_trader_config.json";

    public static final float DEF_MARKUP = 1.5f;
    public static final int DEF_MAX_USES = 12;
    public static final float DEF_FAVORABLE_WEIGHT_MULTIPLIER = 2f;
    public static final float DEF_UNFAVORABLE_WEIGHT_MULTIPLIER = 0.25f;
    public static final int DEF_MIN_CATEGORIES = 1;
    public static final int DEF_MAX_CATEGORIES = 3;

    private float markup = DEF_MARKUP;
    private int defaultMaxUses = DEF_MAX_USES;
    private float favorableWeightMultiplier = DEF_FAVORABLE_WEIGHT_MULTIPLIER;
    private float unfavorableWeightMultiplier = DEF_UNFAVORABLE_WEIGHT_MULTIPLIER;
    private int minCategoriesPerTrader = DEF_MIN_CATEGORIES;
    private int maxCategoriesPerTrader = DEF_MAX_CATEGORIES;
    private final Map<String, TradeCategory> tradeCategories = new LinkedHashMap<>();

    public EnhancedTraderJsonConfig() {
    }

    public EnhancedTraderJsonConfig(String jsonString) {
        this();
        deserialize(jsonString);
    }

    public float getMarkup() { return markup; }

    public int getDefaultMaxUses() { return defaultMaxUses; }

    public float getFavorableWeightMultiplier() { return favorableWeightMultiplier; }

    public float getUnfavorableWeightMultiplier() { return unfavorableWeightMultiplier; }

    public int getMinCategoriesPerTrader() { return minCategoriesPerTrader; }

    public int getMaxCategoriesPerTrader() { return Math.max(minCategoriesPerTrader, maxCategoriesPerTrader); }

    public Collection<TradeCategory> getCategories() {
        return Collections.unmodifiableCollection(tradeCategories.values());
    }

    @Nullable
    public TradeCategory getCategory(String id) {
        return (id == null) ? null : tradeCategories.get(id);
    }

    public void removeCategory(String id) {
        tradeCategories.remove(id);
    }

    public void linkExclusiveCategories() {
        for (TradeCategory category : tradeCategories.values()) {
            for (String otherId : new ArrayList<>(category.getMutuallyExclusiveCategories())) {
                TradeCategory other = tradeCategories.get(otherId);
                if (other == null) {
                    LoggerProject.logWarning(CLASS_ID + "001", "Category '" + category.getId()
                        + "' lists unknown mutually exclusive category '" + otherId + "', reference removed");
                    category.removeExclusive(otherId);
                    continue;
                }
                other.addExclusive(category.getId());
            }
        }
    }

    @Override
    public String serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("markup", markup);
        root.addProperty("defaultMaxUses", defaultMaxUses);
        root.addProperty("favorableWeightMultiplier", favorableWeightMultiplier);
        root.addProperty("unfavorableWeightMultiplier", unfavorableWeightMultiplier);
        root.addProperty("minCategoriesPerTrader", minCategoriesPerTrader);
        root.addProperty("maxCategoriesPerTrader", maxCategoriesPerTrader);

        JsonArray categories = new JsonArray();
        tradeCategories.values().forEach(c -> categories.add(c.serialize()));
        root.add("tradeCategories", categories);

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(root);
    }

    @Override
    public void deserialize(String jsonString) throws RuntimeException {
        if (jsonString == null || jsonString.isBlank()) return;

        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(jsonString);
            if (!parsed.isJsonObject())
                throw new JsonParseException("Expected a JSON object at the root of the config");
            root = parsed.getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON format for EnhancedTraderJsonConfig", e);
        }

        parseGlobals(root);
        parseCategories(root);
    }

    private void parseGlobals(JsonObject root) {
        try {
            if (root.has("markup")) setMarkup(root.get("markup").getAsFloat());
            if (root.has("defaultMaxUses")) this.defaultMaxUses = root.get("defaultMaxUses").getAsInt();
            if (root.has("favorableWeightMultiplier"))
                this.favorableWeightMultiplier = Math.max(0f, root.get("favorableWeightMultiplier").getAsFloat());
            if (root.has("unfavorableWeightMultiplier"))
                this.unfavorableWeightMultiplier = Math.max(0f, root.get("unfavorableWeightMultiplier").getAsFloat());
            if (root.has("minCategoriesPerTrader"))
                this.minCategoriesPerTrader = Math.max(1, root.get("minCategoriesPerTrader").getAsInt());
            if (root.has("maxCategoriesPerTrader"))
                this.maxCategoriesPerTrader = Math.max(1, root.get("maxCategoriesPerTrader").getAsInt());
        } catch (Exception e) {
            LoggerProject.logError(CLASS_ID + "002", "Error parsing global trader settings. " + e.getMessage());
        }
    }

    private void setMarkup(float markup) {
        if (markup < 1f) {
            LoggerProject.logWarning(CLASS_ID + "003", "Global markup below 1 would let players profit by buying and reselling, using 1");
            this.markup = 1f;
            return;
        }
        this.markup = markup;
    }

    private void parseCategories(JsonObject root) {
        tradeCategories.clear();
        if (!root.has("tradeCategories") || !root.get("tradeCategories").isJsonArray()) {
            LoggerProject.logWarning(CLASS_ID + "004", "Trader config has no 'tradeCategories' array, traders will have nothing to trade");
            return;
        }

        for (JsonElement element : root.getAsJsonArray("tradeCategories")) {
            if (!element.isJsonObject()) continue;
            try {
                TradeCategory category = TradeCategory.deserialize(element.getAsJsonObject());
                if (category.getId().isEmpty()) continue;
                if (tradeCategories.containsKey(category.getId())) {
                    LoggerProject.logWarning(CLASS_ID + "005", "Duplicate trade category id '" + category.getId() + "', later entry replaces earlier");
                }
                tradeCategories.put(category.getId(), category);
            } catch (Exception e) {
                LoggerProject.logError(CLASS_ID + "006", "Error deserializing a trade category. " + e.getMessage());
            }
        }
    }

    public static EnhancedTraderJsonConfig buildDefaultConfig() {
        String json = readDefaultResource();
        if (json != null) {
            try {
                return new EnhancedTraderJsonConfig(json);
            } catch (RuntimeException e) {
                LoggerProject.logError(CLASS_ID + "007", "Bundled default trader config is invalid, using fallback. " + e.getMessage());
            }
        }
        return buildFallbackConfig();
    }

    @Nullable
    private static String readDefaultResource() {
        try (InputStream stream = EnhancedTraderJsonConfig.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (stream == null) {
                LoggerProject.logWarning(CLASS_ID + "008", "Bundled default trader config not found at " + DEFAULT_CONFIG_RESOURCE);
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LoggerProject.logError(CLASS_ID + "009", "Failed to read bundled default trader config. " + e.getMessage());
            return null;
        }
    }

    private static EnhancedTraderJsonConfig buildFallbackConfig() {
        EnhancedTraderJsonConfig config = new EnhancedTraderJsonConfig();
        TradeCategory gatherer = new TradeCategory("Novice Gatherer");
        gatherer.setIcon("minecraft:sweet_berries");
        gatherer.addTrade(new TradeEntry("minecraft:apple", 0.5f));
        gatherer.addTrade(new TradeEntry("minecraft:bread", 1f));
        gatherer.addTrade(new TradeEntry("minecraft:sweet_berries", 0.125f));
        config.tradeCategories.put(gatherer.getId(), gatherer);
        return config;
    }
}
