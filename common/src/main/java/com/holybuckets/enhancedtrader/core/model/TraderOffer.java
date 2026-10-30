package com.holybuckets.enhancedtrader.core.model;

import com.holybuckets.enhancedtrader.core.TradeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;

public class TraderOffer {

    private static final String NBT_CATEGORY = "category";
    private static final String NBT_ITEM = "item";
    private static final String NBT_USES = "uses";

    private final String categoryId;
    private final Item item;
    private final int quantity;
    private final int marketPrice;
    private final int markupPrice;
    private final int tradeStock;
    private int waresBought;

    /**
     * Merchant BUYS at the market price merchant SELLS at the markup price (market price + markup)
     * @param categoryId
     * @param item
     * @param quantity
     * @param marketPrice
     * @param markupPrice
     * @param uses
     * @param tradeStock
     */
    public TraderOffer(String categoryId, Item item, int marketPrice, int markupPrice, int quantity, int uses, int tradeStock)
    {
        this.categoryId = (categoryId == null) ? "" : categoryId;
        this.item = item;
        this.quantity = Math.max(1, quantity);
        this.marketPrice = Math.max(1, marketPrice);
        this.markupPrice = Math.max(this.marketPrice, markupPrice);
        this.waresBought = Math.max(0, uses);
        this.tradeStock = tradeStock;
    }

    public String getCategoryId() { return categoryId; }

    public Item getItem() { return item; }

    public ItemStack getIcon() { return (item != null) ? new ItemStack(item) : ItemStack.EMPTY; }

    public int getQuantity() { return quantity; }

    public int getMarketPrice() { return marketPrice; }

    public int getMarkupPrice() { return markupPrice; }

    public int getUses() { return waresBought; }

    public int getTradeStock() { return tradeStock; }

    public boolean isUnlimited() { return tradeStock <= 0; }

    public int getRemainingWaresCount() {
        return isUnlimited() ? Integer.MAX_VALUE : Math.max(0, tradeStock - waresBought);
    }

    public boolean isOutOfStock() { return getRemainingWaresCount() <= 0; }

    public void setWaresSold(int waresBought) { this.waresBought = Math.max(0, waresBought); }

    public void waresBought(int count) { setWaresSold(this.waresBought + count); }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(categoryId);
        buf.writeItem(getIcon());
        buf.writeVarInt(quantity);
        buf.writeVarInt(marketPrice);
        buf.writeVarInt(markupPrice);
        buf.writeVarInt(waresBought);
        buf.writeInt(tradeStock);
    }

    public static TraderOffer read(FriendlyByteBuf buf) {
        String categoryId = buf.readUtf();
        ItemStack icon = buf.readItem();
        int quantity = buf.readVarInt();
        int marketPrice = buf.readVarInt();
        int markupPrice = buf.readVarInt();
        int waresBought = buf.readVarInt();
        int tradeStock = buf.readInt();
        return new TraderOffer(categoryId, icon.getItem(), marketPrice, markupPrice, quantity, waresBought, tradeStock);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_CATEGORY, categoryId);
        tag.putString(NBT_ITEM, BuiltInRegistries.ITEM.getKey(item).toString());
        tag.putInt(NBT_USES, waresBought);
        return tag;
    }

    @Nullable
    public static TraderOffer load(Biome biome, CompoundTag tag, int days) {
        String categoryId = tag.getString(NBT_CATEGORY);
        String itemId = tag.getString(NBT_ITEM);
        return TradeManager.restoreOffer(biome, categoryId, itemId, days, tag.getInt(NBT_USES));
    }
}
