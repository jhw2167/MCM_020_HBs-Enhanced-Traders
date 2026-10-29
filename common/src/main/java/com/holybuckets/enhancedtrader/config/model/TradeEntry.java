package com.holybuckets.enhancedtrader.config.model;

import com.holybuckets.enhancedtrader.LoggerProject;
import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.math.BigDecimal;

public class TradeEntry {

    public static final String CLASS_ID = "005";
    public static final float DEF_MULTIPLIER = 1f;
    private static final String SEPARATOR = ",";

    private final String itemId;
    private final float multiplier;
    private Item item;

    public TradeEntry(String itemId, float multiplier) {
        this.itemId = itemId.trim();
        this.multiplier = multiplier;
    }

    public TradeEntry(String itemId, Item item, float multiplier) {
        this.itemId = itemId;
        this.multiplier = multiplier;
        this.item = item;
    }

    public String getItemId() { return itemId; }

    public Item getItem() { return item; }

    public float getMultiplier() { return multiplier; }

    public String serialize() {
        String value = BigDecimal.valueOf(multiplier).stripTrailingZeros().toPlainString();
        return itemId + SEPARATOR + value;
    }

    @Nullable
    public static TradeEntry parse(String raw)
    {
        if (raw == null || raw.isBlank()) return null;

        String[] parts = raw.split(SEPARATOR);
        String itemId = parts[0].trim();
        if (itemId.isEmpty()) return null;

        float multiplier = DEF_MULTIPLIER;
        if (parts.length > 1) {
            try {
                multiplier = Float.parseFloat(parts[1].trim());
            } catch (NumberFormatException e) {
                return null;
            }
            if (multiplier <= 0f || Float.isNaN(multiplier) || Float.isInfinite(multiplier))
                return null;
        }

        return new TradeEntry(itemId, HBUtil.ItemUtil.itemNameToItem(itemId), multiplier);
    }

}
