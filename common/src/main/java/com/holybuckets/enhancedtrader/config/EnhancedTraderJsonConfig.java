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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnhancedTraderJsonConfig implements IStringSerializable {

    public static final String CLASS_ID = "003";
    public static final String DEF_CONFIG_FILE_PATH = EnhancedTraderConfig.DEF_TRADER_CONFIG_PATH;
    public static final String DEFAULT_CONFIG_RESOURCE = "/assets/" + Constants.MOD_ID + "/config/default_trader_config.json";
    private final Map<String, TradeCategory> tradeCategories = new LinkedHashMap<>();

    public EnhancedTraderJsonConfig() {
    }

    public EnhancedTraderJsonConfig(String jsonString) {
        this();
        deserialize(jsonString);
    }

    public Collection<TradeCategory> getCategories() {
        return Collections.unmodifiableCollection(tradeCategories.values());
    }

    @Nullable
    public TradeCategory getCategory(String id) {
        return (id == null) ? null : tradeCategories.get(id);
    }


    @Override
    public String serialize() {
        JsonObject root = new JsonObject();

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
        parseCategories(root);
    }


    private void parseCategories(JsonObject root)
    {
        tradeCategories.clear();
        if (!root.has("tradeCategories") || !root.get("tradeCategories").isJsonArray()) {
            LoggerProject.logWarning("003004", "Trader config has no 'tradeCategories' array, traders will have nothing to trade");
            return;
        }

        for (JsonElement element : root.getAsJsonArray("tradeCategories")) {
            if (!element.isJsonObject()) continue;
            try {
                TradeCategory category = TradeCategory.deserialize(element.getAsJsonObject());

                if (category.getId().isEmpty()) continue;
                if (tradeCategories.containsKey(category.getId())) {
                    LoggerProject.logWarning("003005", "Duplicate trade category id '" + category.getId() + "', later entry replaces earlier");
                }
                tradeCategories.put(category.getId(), category);
            } catch (Exception e) {
                LoggerProject.logError("003006", "Error deserializing a trade category. " + e.getMessage());
            }
        }
    }


    public static EnhancedTraderJsonConfig defaultConfig() {
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
