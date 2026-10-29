package com.holybuckets.enhancedtrader.core;

import com.holybuckets.enhancedtrader.EnhancedTraderMain;
import com.holybuckets.enhancedtrader.LoggerProject;
import com.holybuckets.enhancedtrader.config.EnhancedTraderConfig;
import com.holybuckets.enhancedtrader.config.EnhancedTraderJsonConfig;
import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.enhancedtrader.config.model.TradeCategory;
import com.holybuckets.enhancedtrader.config.model.TradeEntry;
import com.holybuckets.enhancedtrader.core.model.TraderOffer;
import com.holybuckets.enhancedtrader.core.model.TraderTab;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TradeManager {

    public static final String CLASS_ID = "006";
    private static GeneralConfig CONFIG;
    private static ModConfig MOD_CONFIG;

    private TradeManager() {
    }

    public static int quantity(float multiplier) {
        if (multiplier >= 1f) return 1;
        return Math.max(1, Math.round(1f / multiplier));
    }

    public static int sellPrice(float multiplier, int marketRate) {
        if (multiplier < 1f) return marketRate;
        return Math.max(1, Math.round(marketRate * multiplier));
    }

    public static int buyPrice(int sellPrice, float markup) {
        int marked = Math.round(sellPrice * markup);
        return Math.max(sellPrice, marked);
    }

    @Nullable
    public static TraderOffer createOffer(TradeCategory category, TradeEntry entry) {
        if (category == null || entry == null || entry.getItem() == null) return null;
        ModConfig config = ModConfig.getInstance();
        float multiplier = entry.getMultiplier();
        int sell = sellPrice(multiplier, EnhancedTraderMain.MOD_CONFIG.marketRate);
        int buy = buyPrice(sell, config.getMarkup(category));
        return new TraderOffer(category.getId(), entry.getItem(), quantity(multiplier), sell, buy, 0, config.getMaxUses(category));
    }

    @Nullable
    public static TraderOffer restoreOffer(String categoryId, Item item, int uses) {
        TradeCategory category = ModConfig.getInstance().getCategory(categoryId);
        if (category == null) return null;
        TraderOffer offer = createOffer(category, category.findTrade(item));
        if (offer != null) offer.setUses(uses);
        return offer;
    }

    public static List<TradeCategory> rollCategories(@Nullable Holder<Biome> biome, RandomSource random) {
        EnhancedTraderJsonConfig json = ModConfig.getInstance().getTraderConfig();
        List<TradeCategory> pool = new ArrayList<>(json.getCategories());
        List<TradeCategory> selected = new ArrayList<>();

        for (TradeCategory category : new ArrayList<>(pool)) {
            if (!pool.contains(category) || !category.isForcedIn(biome)) continue;
            select(category, pool, selected);
        }

        int min = json.getMinCategoriesPerTrader();
        int max = json.getMaxCategoriesPerTrader();
        int target = Math.max(selected.size(), min + random.nextInt(max - min + 1));

        while (selected.size() < target) {
            TradeCategory pick = weightedPick(pool, biome, random, json);
            if (pick == null) break;
            select(pick, pool, selected);
        }

        if (EnhancedTraderMain.MOD_CONFIG.devMode) {
            LoggerProject.logDebug("006001", "Rolled categories " + selected.stream().map(TradeCategory::getId).toList()
                + " for biome " + (biome == null ? "none" : biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown")));
        }
        return selected;
    }

    public static float weightFor(TradeCategory category, @Nullable Holder<Biome> biome, EnhancedTraderJsonConfig json) {
        float weight = category.getWeight();
        if (category.isFavorable(biome)) weight *= json.getFavorableWeightMultiplier();
        if (category.isUnfavorable(biome)) weight *= json.getUnfavorableWeightMultiplier();
        return Math.max(0f, weight);
    }

    @Nullable
    private static TradeCategory weightedPick(List<TradeCategory> pool, @Nullable Holder<Biome> biome,
                                              RandomSource random, EnhancedTraderJsonConfig json) {
        float total = 0f;
        float[] weights = new float[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            weights[i] = weightFor(pool.get(i), biome, json);
            total += weights[i];
        }
        if (total <= 0f) return null;

        float roll = random.nextFloat() * total;
        for (int i = 0; i < pool.size(); i++) {
            roll -= weights[i];
            if (roll < 0f && weights[i] > 0f) return pool.get(i);
        }
        for (int i = pool.size() - 1; i >= 0; i--) {
            if (weights[i] > 0f) return pool.get(i);
        }
        return null;
    }

    private static void select(TradeCategory category, List<TradeCategory> pool, List<TradeCategory> selected) {
        selected.add(category);
        pool.removeIf(other -> other == category
            || category.isExclusiveWith(other.getId())
            || other.isExclusiveWith(category.getId()));
    }

    public static List<TraderOffer> buildOffers(WanderingTrader trader)
    {
        Level level = trader.level();
        Holder<Biome> biome = level.getBiome(trader.blockPosition());
        int biomeId = HBUtil.LevelUtil.getBiomeSimpleName(biome).hashCode();
        RandomSource random = RandomSource.create(CONFIG.getTotalDays(level) *biomeId);

        List<TradeCategory> categories = MOD_CONFIG.getCategories(biome);
        List<TraderOffer> offers = new ArrayList<>();
        for (TradeCategory category : categories) {
            for (TradeEntry entry : pickTrades(category, random)) {
                TraderOffer offer = createOffer(category, entry);
                if (offer != null) offers.add(offer);
            }
        }
        return offers;
    }

    private static List<TradeEntry> pickTrades(TradeCategory category, RandomSource random) {
        List<TradeEntry> trades = category.getTrades();
        int count = category.getTradeCount();
        if (count <= 0 || count >= trades.size()) return trades;

        List<Integer> indices = new ArrayList<>(trades.size());
        for (int i = 0; i < trades.size(); i++) indices.add(i);
        for (int i = indices.size() - 1; i > 0; i--) {
            Collections.swap(indices, i, random.nextInt(i + 1));
        }

        List<Integer> chosen = new ArrayList<>(indices.subList(0, count));
        Collections.sort(chosen);
        List<TradeEntry> picked = new ArrayList<>(count);
        for (int index : chosen) picked.add(trades.get(index));
        return picked;
    }

    public static List<TraderTab> buildTabs(List<String> categoryIds, List<TraderOffer> offers) {
        Map<String, List<TraderOffer>> grouped = new LinkedHashMap<>();
        for (String id : categoryIds) grouped.put(id, new ArrayList<>());
        for (TraderOffer offer : offers) {
            grouped.computeIfAbsent(offer.getCategoryId(), k -> new ArrayList<>()).add(offer);
        }

        List<TraderTab> tabs = new ArrayList<>();
        for (Map.Entry<String, List<TraderOffer>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            TradeCategory category = ModConfig.getInstance().getCategory(entry.getKey());
            Item icon = (category != null) ? category.getIconItem() : entry.getValue().get(0).getItem();
            tabs.add(new TraderTab(entry.getKey(), new ItemStack(icon), entry.getValue()));
        }
        return tabs;
    }

    //** EVENTS

    public static void configureOnServerStart(EnhancedTraderConfig config) {
        CONFIG = GeneralConfig.getInstance();
        MOD_CONFIG = ModConfig.getInstance();
    }


}
