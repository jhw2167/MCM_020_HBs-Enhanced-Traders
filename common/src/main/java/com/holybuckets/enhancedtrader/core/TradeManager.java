package com.holybuckets.enhancedtrader.core;

import com.holybuckets.enhancedtrader.config.EnhancedTraderConfig;
import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.enhancedtrader.config.model.TradeCategory;
import com.holybuckets.enhancedtrader.config.model.TradeEntry;
import com.holybuckets.enhancedtrader.core.model.TraderOffer;
import com.holybuckets.enhancedtrader.core.model.TraderTab;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class TradeManager {

    public static final String CLASS_ID = "006";
    private static GeneralConfig CONFIG;
    private static ModConfig MOD_CONFIG;

    private TradeManager() {
    }


    public static List<TraderTab> buildTabs(List<TraderOffer> offers) {
        Map<String, List<TraderOffer>> grouped = new LinkedHashMap<>();
        for (TraderOffer offer : offers) {
            grouped.computeIfAbsent(offer.getCategoryId(), k -> new ArrayList<>()).add(offer);
        }

        List<TraderTab> tabs = new ArrayList<>();
        for (Map.Entry<String, List<TraderOffer>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            TradeCategory category = MOD_CONFIG.getCategory(entry.getKey());
            if(category==null) continue;
            Item icon = category.getIconItem();
            tabs.add(new TraderTab(entry.getKey(), new ItemStack(icon), entry.getValue()));
        }
        return tabs;
    }

    /**
     * Trades are based chiefly on Biome, and some randomness
     *  1. Determine biome we are in
     *  2. find all valid categories for biome
     *  3. Take weighted sample of categories, could be between 2 and 5 final categories for trading
     *  4. Build offer list from each category
     * @param trader
     * @return
     */
    public static List<TraderOffer> buildOffers(WanderingTrader trader)
    {
        Level level = trader.level();
        Holder<Biome> biome = level.getBiome(trader.blockPosition());
        int biomeId = HBUtil.LevelUtil.getBiomeSimpleName(biome).hashCode();
        Random random = new Random((long) CONFIG.getTotalDays(level) *biomeId);

        List<TradeCategory> categories = new ArrayList<>(64);
        List<TradeCategory> weighted = new ArrayList<>();
        int weightedCategories = 0;
        for (TradeCategory category : MOD_CONFIG.getShuffledCategories(random))
        {
            if(category.isUnfavorable(biome.value())) continue;
            if(category.isFavorable(biome.value())) {
                categories.add(category); continue;
            }
            if (isExclusiveWithAny(categories).test(category)) continue;
            //for weight of category, add it to weighted list multiple times
            for(int i=0; i<category.getWeight(); i++) weighted.add(category);
            weightedCategories++;
        }

        int maxCategories = MOD_CONFIG.getMaxCategories();
        int minCategories = MOD_CONFIG.getMinCategories();
        if(weightedCategories+categories.size() <= minCategories) {
            for (TradeCategory category : weighted) {
                if(!categories.contains(category)) categories.add(category);
            }
        }

        int numCategoriesToAdd = maxCategories - categories.size();
        if(numCategoriesToAdd > 0) {
            Collections.shuffle(weighted, random);
            for (int i = 0; i < numCategoriesToAdd && i < weighted.size(); i++) {
                TradeCategory category = weighted.get(i);
                if (!categories.contains(category)) categories.add(category);
                if (categories.size() >= maxCategories) break;
            }
        }

        List<TraderOffer> offers = new ArrayList<>();
        for (TradeCategory category : categories) {
            List<TradeEntry> trades = category.getTrades();
            Function<String,Random> _r = (item) -> tradeOfferRandom(biome.value(), category.getId(),
            item, CONFIG.getTotalDays(level));
            for (TradeEntry entry : trades) {
                TraderOffer offer = createOffer(category, entry, _r.apply(entry.getItemId()));
                if (offer != null) offers.add(offer);
            }
        }

        return offers;
    }

        private static Random tradeOfferRandom(Biome b, String cat, String item, int day) {
            int biomeId = HBUtil.LevelUtil.toBiomeResourceLocation(b).toString().hashCode();
            int itemId = item.hashCode();
            int categoryId = cat.hashCode();
            return new Random((long) biomeId *categoryId*itemId*(day+1));
        }

        private static Predicate<TradeCategory> isExclusiveWithAny(List<TradeCategory> selected) {
            return category -> selected.stream().anyMatch(c -> c.isExclusiveWith(category.getId()) || category.isExclusiveWith(c.getId()));
        }



    @Nullable
    public static TraderOffer createOffer(TradeCategory category, TradeEntry entry, Random r)
    {
        if (category == null || entry == null || entry.getItem() == null) return null;
        final int MRKT = MOD_CONFIG.getMarketRate();
        final float markup = MOD_CONFIG.getMarkup(category);

        float multiplier = entry.getMultiplier();
        int marketRate = normalize(MRKT, (float) MRKT/4, r);
        int markupRate = Math.round(marketRate*markup);

        int maxStock = normalize(MOD_CONFIG.getTradeStock(), MOD_CONFIG.getTradeStockVariance(), r);
        return new TraderOffer(category.getId(), entry.getItem(), marketRate,
            markupRate, quantity(multiplier), 0, maxStock);
    }

    @Nullable
    public static TraderOffer restoreOffer(Biome b, String categoryId, String itemId, int days, int uses)
    {
        Random r = tradeOfferRandom(b, categoryId, itemId, days);
        TradeCategory category = MOD_CONFIG.getCategory(categoryId);
        Item item = HBUtil.ItemUtil.itemNameToItem(itemId);
        if(category == null || item==null) return null;
        TradeEntry entry = category.findTrade(item);
        return createOffer(category, entry, r);
    }

    /**
     * Gaussian derived market price, mean=MRKT, std=MRT/4
     * @param mean
     * @param random
     * @return
     */
        private static int normalize(int mean, float variance, Random random) {
            float gaussian = (float) random.nextGaussian();
            return Math.round(mean + gaussian * variance);

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


    //** EVENTS
    public static void configureOnServerStart(EnhancedTraderConfig config) {
        CONFIG = GeneralConfig.getInstance();
        MOD_CONFIG = ModConfig.getInstance();
    }


}
