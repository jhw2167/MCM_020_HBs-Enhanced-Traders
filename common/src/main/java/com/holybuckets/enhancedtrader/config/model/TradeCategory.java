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
import javax.swing.*;
import java.util.*;
import java.util.function.Predicate;

public class TradeCategory {

    public static final String CLASS_ID = "004";
    public static final int DEF_WEIGHT = 10;

    private final String id;
    private int weight = DEF_WEIGHT;
    private Float markup;
    private Item iconItem;
    private final List<TradeEntry> trades = new ArrayList<>();
    private Set<Biome> favorableBiomes = new HashSet<>();
    private Set<Biome> unfavorableBiomes = new HashSet<>();

    private final Set<String> mutuallyExclusiveCategories = new LinkedHashSet<>();

    public TradeCategory(String id) {
        this.id = (id == null) ? "" : id.trim();
    }

    public String getId() { return id; }

    public int getWeight() { return weight; }

    public Float getMarkup() { return markup; }


    public List<TradeEntry> getTrades() { return Collections.unmodifiableList(trades); }

    public Item getIconItem() { return iconItem; }

    public Set<String> getMutuallyExclusiveCategories() { return mutuallyExclusiveCategories; }

    public boolean isFavorable(@Nullable Biome biome) { return favorableBiomes.contains(biome); }

    public boolean isUnfavorable(@Nullable Biome biome) { return unfavorableBiomes.contains(biome); }

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

    public void setIcon(String icon) {
        this.iconItem = HBUtil.ItemUtil.itemNameToItem(icon);
        if(iconItem==null || iconItem==Items.AIR) iconItem=Items.EMERALD;
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

        JsonArray tradeArr = new JsonArray();
        trades.forEach(t -> tradeArr.add(t.serialize()));
        obj.add("trades", tradeArr);

        obj.add("favorableBiomes", toBiomeResourceLocation(favorableBiomes));
        obj.add("unfavorableBiomes", toBiomeResourceLocation(unfavorableBiomes));

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
            else category.setMarkup(ModConfig.getInstance().getDefaultMarkup());
            if (obj.has("icon")) category.setIcon(obj.get("icon").getAsString());
            else category.setIcon(null);
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

        if(HBUtil.JCHECK(obj, "favorableBiomes", HBUtil.JTYPE.ARRAY))
            category.favorableBiomes = toBiomes(obj.getAsJsonArray("favorableBiomes"));
        if(HBUtil.JCHECK(obj, "unfavorableBiomes", HBUtil.JTYPE.ARRAY))
            category.unfavorableBiomes = toBiomes(obj.getAsJsonArray("unfavorableBiomes"));

        if (HBUtil.JCHECK(obj, "mutuallyExclusiveCategories", HBUtil.JTYPE.ARRAY)) {
            JsonArray array = obj.getAsJsonArray("mutuallyExclusiveCategories");
            for (JsonElement element : array) {
                if(HBUtil.JCHECK(element, HBUtil.JTYPE.STRING))
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

    private static final Predicate<JsonElement> checkString = s -> HBUtil.JCHECK(s, HBUtil.JTYPE.STRING);
    private static Set<Biome> toBiomes(JsonArray biomes) {
        if(biomes==null || !biomes.isJsonArray() ) return Collections.emptySet();
        Set<Biome> biomeSet = new HashSet<>();
        Collection<String> biomeNames = biomes.asList().stream()
            .filter(checkString).map(JsonElement::getAsString).toList();
        biomeSet.addAll((HBUtil.LevelUtil.getBiomesFromNamesOrTag(biomeNames)));
        return biomeSet;
    }

}
