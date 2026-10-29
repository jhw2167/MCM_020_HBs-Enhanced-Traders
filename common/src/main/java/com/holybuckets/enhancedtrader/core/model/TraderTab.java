package com.holybuckets.enhancedtrader.core.model;

import com.holybuckets.enhancedtrader.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TraderTab {

    private final String categoryId;
    private final ItemStack icon;
    private final List<TraderOffer> offers;

    public TraderTab(String categoryId, ItemStack icon, List<TraderOffer> offers) {
        this.categoryId = (categoryId == null) ? "" : categoryId;
        this.icon = (icon == null) ? ItemStack.EMPTY : icon;
        this.offers = (offers == null) ? new ArrayList<>() : offers;
    }

    public String getCategoryId() { return categoryId; }

    public ItemStack getIcon() { return icon; }

    public List<TraderOffer> getOffers() { return Collections.unmodifiableList(offers); }

    public Component getDisplayName() {
        String key = "trader_category." + Constants.MOD_ID + "."
            + categoryId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return Component.translatableWithFallback(key, categoryId);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(categoryId);
        buf.writeItem(icon);
        buf.writeVarInt(offers.size());
        for (TraderOffer offer : offers) offer.write(buf);
    }

    public static TraderTab read(FriendlyByteBuf buf) {
        String categoryId = buf.readUtf();
        ItemStack icon = buf.readItem();
        int count = buf.readVarInt();
        List<TraderOffer> offers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) offers.add(TraderOffer.read(buf));
        return new TraderTab(categoryId, icon, offers);
    }
}
