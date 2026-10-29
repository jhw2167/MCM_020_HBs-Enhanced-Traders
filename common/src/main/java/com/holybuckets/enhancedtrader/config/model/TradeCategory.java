package com.holybuckets.enhancedtrader.config.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.holybuckets.enhancedtrader.LoggerProject;
import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.*;

public class TradeCategory {

    public static final String CLASS_ID = "004";
    public static final int DEF_WEIGHT = 10;

    private final String id;
    private int weight = DEF_WEIGHT;
    private Float markup;
    private Integer maxUses;
    private int tradeCount = 0;
    private Item iconItem;
    private final List<TradeEntry> trades = new ArrayList<>();
    private Set<Biome> favorableBiomes = new HashSet<>();
    private Set<Biome> unfavorableBiomes = new HashSet<>();
    private Set<Biome> forceSpawnBiomes = new HashSet<>();

    private final Set<String> mutuallyExclusiveCategories = new LinkedHashSet<>();

    public TradeCategory(String id) {
        this.id = (id == null) ? "" : id.trim();
    }

    public String getId() { return id; }

    public int getWeight() { return weight; }

    public Float getMarkup() { return markup; }

    public int getTradeCount() { return tradeCount; }

    public List<TradeEntry> getTrades() { return Collections.unmodifiableList(trades); }

    public Item getIconItem() { return iconItem; }

    public boolean isFavorable(@Nullable Holder<Biome> biome) { return favorableBiomes.contains(biome); }

    public boolean isUnfavorable(@Nullable Holder<Biome> biome) { return unfavorableBiomes.contains(biome); }

    public boolean isForcedIn(@Nullable Holder<Biome> biome) { return forceSpawnBiomes.contains(biome); }

    public boolean isExclusiveWith(String categoryId) {
        return mutuallyExclusiveCategories.contains(categoryId);
    }

    @Nullable
    public TradeEntry findTrade(Item item) {
        if (item == null) return null;
        for (TradeEntry entry : trades) {
            if (entry.getItem() == item) return entry;
        }
        return null;
    }

    public void setWeight(int weight) {
        if (weight < 0) weight=1;
        this.weight = weight;
    }

    public void setMarkup(@Nullable Float markup) {
        if (markup != null && markup < 1f) markup = 1f;
        this.markup = markup;
    }

    public void setTradeCount(int tradeCount) { this.tradeCount = Math.max(0, tradeCount); }

    private void setIcon(String icon) {
        this.iconItem = HBUtil.ItemUtil.itemNameToItem(icon);
        if(iconItem==null) iconItem=Items.EMERALD;
    }

    public void addTrade(TradeEntry entry) {
        if (entry != null) trades.add(entry);
    }

    public void addExclusive(String categoryId) {
        if (categoryId == null || categoryId.isBlank() || categoryId.equals(id)) return;
        mutuallyExclusiveCategories.add(categoryId.trim());
    }

    public void removeExclusive(String categoryId) {
        mutuallyExclusiveCategories.remove(categoryId);
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        String icon = HBUtil.ItemUtil.itemToString(iconItem);
        if (icon != null) obj.addProperty("icon", icon);
        obj.addProperty("weight", weight);
        if (markup != null) obj.addProperty("markup", markup);
        if (maxUses != null) obj.addProperty("maxUses", maxUses);
        if (tradeCount > 0) obj.addProperty("tradeCount", tradeCount);

        JsonArray tradeArr = new JsonArray();
        trades.forEach(t -> tradeArr.add(t.serialize()));
        obj.add("trades", tradeArr);

        obj.add("favorableBiomes", toBiomeResourceLocation(favorableBiomes));
        obj.add("unfavorableBiomes", toBiomeResourceLocation(unfavorableBiomes));
        if (!forceSpawnBiomes.isEmpty()) obj.add("forceSpawnBiomes", toBiomeResourceLocation(forceSpawnBiomes));

        JsonArray exclusive = new JsonArray();
        mutuallyExclusiveCategories.forEach(exclusive::add);
        obj.add("mutuallyExclusiveCategories", exclusive);
        return obj;
    }

    private static final String TRADE_ITEM_ERR = "Error parsing trade: category: %s, Item '%s'." +
     " Please input a valid minecraft item name, a comma and a number between 0.1 and 10.0 (e.g.'minecraft:dirt,0.5').";
    public static TradeCategory deserialize(JsonObject obj) {
        String id = obj.has("id") ? obj.get("id").getAsString() : "";
        TradeCategory category = new TradeCategory(id);
        if (category.id.isEmpty()) {
            LoggerProject.logError(CLASS_ID + "003", "Trade category is missing 'id' property, skipping");
            return category;
        }

        try {
            if (obj.has("weight")) category.setWeight(obj.get("weight").getAsInt());
            if (obj.has("markup")) category.setMarkup(obj.get("markup").getAsFloat());
            if (obj.has("tradeCount")) category.setTradeCount(obj.get("tradeCount").getAsInt());
            if (obj.has("icon")) category.setIcon(obj.get("icon").getAsString());
        } catch (Exception e) {
            LoggerProject.logError(CLASS_ID + "004", "Error parsing properties for category: " + id + ". " + e.getMessage());
        }

        if (obj.has("trades") && obj.get("trades").isJsonArray()) {
            for (JsonElement element : obj.getAsJsonArray("trades")) {
                TradeEntry entry = TradeEntry.parse(element.getAsString());
                if (entry != null)
                    category.addTrade(entry);
                else
                    LoggerProject.logError(CLASS_ID + "005", String.format(TRADE_ITEM_ERR, id, element.getAsString()));
            }
        }

        if(obj.has("favorableBiomes"))
            category.favorableBiomes = toBiomes(obj.getAsJsonObject("favorableBiomes"));
        if(obj.has("unfavorableBiomes"))
            category.unfavorableBiomes = toBiomes(obj.getAsJsonObject("unfavorableBiomes"));
        if(obj.has("forceSpawnBiomes"))
            category.forceSpawnBiomes = toBiomes(obj.getAsJsonObject("forceSpawnBiomes"));

        if (obj.has("mutuallyExclusiveCategories")) {
            JsonElement element = obj.get("mutuallyExclusiveCategories");
            if (element.isJsonArray()) {
                for (JsonElement other : element.getAsJsonArray()) category.addExclusive(other.getAsString());
            } else if (element.isJsonPrimitive()) {
                category.addExclusive(element.getAsString());
            }
        }

        return category;
    }

    private static JsonArray toBiomeResourceLocation(Collection<Biome> biomes) {
        JsonArray array = new JsonArray();
        if(biomes==null || biomes.isEmpty()) return array;
        for(Biome biome : biomes) {
            ResourceLocation loc = HBUtil.LevelUtil.toBiomeResourceLocation(biome);
            array.add(loc.toString());
        }
        return array;
    }

    private static Set<Biome> toBiomes(JsonObject biomes) {
        if(biomes==null || !biomes.isJsonArray() ) return Collections.emptySet();
        Set<Biome> biomeSet = new HashSet<>();
        for(JsonElement element : biomes.getAsJsonArray()) {
            var biome = HBUtil.LevelUtil.getBiome(element.getAsString());
            if(biome!=null) biomeSet.add(biome);
        }
        return biomeSet;
    }

}
