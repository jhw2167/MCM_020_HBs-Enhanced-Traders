package com.holybuckets.enhancedtrader.core.model;

import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.enhancedtrader.core.TradeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class TraderOffer {

    private static final String NBT_CATEGORY = "category";
    private static final String NBT_ITEM = "item";
    private static final String NBT_USES = "uses";

    private final String categoryId;
    private final Item item;
    private final int quantity;
    private final int sellPrice;
    private final int buyPrice;
    private final int maxUses;
    private int uses;

    public TraderOffer(String categoryId, Item item, int quantity, int sellPrice, int buyPrice, int uses, int maxUses) {
        this.categoryId = (categoryId == null) ? "" : categoryId;
        this.item = item;
        this.quantity = Math.max(1, quantity);
        this.sellPrice = Math.max(1, sellPrice);
        this.buyPrice = Math.max(this.sellPrice, buyPrice);
        this.uses = Math.max(0, uses);
        this.maxUses = maxUses;
    }

    public String getCategoryId() { return categoryId; }

    public Item getItem() { return item; }

    public ItemStack getIcon() { return (item != null) ? new ItemStack(item) : ItemStack.EMPTY; }

    public int getQuantity() { return quantity; }

    public int getSellPrice() { return sellPrice; }

    public int getBuyPrice() { return buyPrice; }

    public int getPrice(boolean selling) { return selling ? sellPrice : buyPrice; }

    public int getUses() { return uses; }

    public int getMaxUses() { return maxUses; }

    public boolean isUnlimited() { return maxUses <= 0; }

    public int getRemainingUses() {
        return isUnlimited() ? Integer.MAX_VALUE : Math.max(0, maxUses - uses);
    }

    public boolean isOutOfStock() { return getRemainingUses() <= 0; }

    public void setUses(int uses) { this.uses = Math.max(0, uses); }

    public void addUses(int count) { setUses(this.uses + count); }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(categoryId);
        buf.writeItem(getIcon());
        buf.writeVarInt(quantity);
        buf.writeVarInt(sellPrice);
        buf.writeVarInt(buyPrice);
        buf.writeVarInt(uses);
        buf.writeInt(maxUses);
    }

    public static TraderOffer read(FriendlyByteBuf buf) {
        String categoryId = buf.readUtf();
        ItemStack icon = buf.readItem();
        int quantity = buf.readVarInt();
        int sellPrice = buf.readVarInt();
        int buyPrice = buf.readVarInt();
        int uses = buf.readVarInt();
        int maxUses = buf.readInt();
        return new TraderOffer(categoryId, icon.getItem(), quantity, sellPrice, buyPrice, uses, maxUses);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_CATEGORY, categoryId);
        tag.putString(NBT_ITEM, BuiltInRegistries.ITEM.getKey(item).toString());
        tag.putInt(NBT_USES, uses);
        return tag;
    }

    @Nullable
    public static TraderOffer load(CompoundTag tag) {
        Item item = ModConfig.resolveItem(tag.getString(NBT_ITEM));
        if (item == null) return null;
        return TradeManager.restoreOffer(tag.getString(NBT_CATEGORY), item, tag.getInt(NBT_USES));
    }
}
