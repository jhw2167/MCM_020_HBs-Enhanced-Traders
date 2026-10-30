package com.holybuckets.enhancedtrader.menu;

import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.enhancedtrader.core.TradeManager;
import com.holybuckets.enhancedtrader.entity.EnhancedTraderData;
import com.holybuckets.enhancedtrader.core.model.TraderOffer;
import com.holybuckets.enhancedtrader.core.model.TraderTab;
import com.holybuckets.enhancedtrader.networking.TraderOffersSync;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnhancedTraderMenu extends AbstractContainerMenu {

    public static final int GUI_WIDTH = 276;
    public static final int GUI_HEIGHT = 166;

    public static final int TRADE_LIST_X = 5;
    public static final int TRADE_LIST_Y = 18;
    public static final int TRADE_LIST_WIDTH = 88;
    public static final int TRADE_LIST_HEIGHT = 140;
    public static final int TRADE_ROW_HEIGHT = 20;
    public static final int VISIBLE_ROWS = TRADE_LIST_HEIGHT / TRADE_ROW_HEIGHT;

    public static final int SCROLLBAR_X = 94;
    public static final int SCROLLBAR_Y = 18;
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int SCROLLBAR_HEIGHT = 139;

    public static final int INPUT_A_X = 136;
    public static final int INPUT_A_Y = 37;
    public static final int INPUT_B_X = 162;
    public static final int INPUT_B_Y = 37;
    public static final int OUTPUT_X = 220;
    public static final int OUTPUT_Y = 37;
    public static final int ARROW_X = 186;
    public static final int ARROW_Y = 38;

    public static final int REVERSE_WIDTH = 12;
    public static final int REVERSE_HEIGHT = 17;
    public static final int REVERSE_X = 108;
    public static final int REVERSE_Y = 18;

    public static final int TAB_WIDTH = 32;
    public static final int TAB_HEIGHT = 32;
    public static final int TAB_SPACING = 33;
    public static final int TAB_Y = -28;
    public static final int MAX_TABS = GUI_WIDTH / TAB_SPACING;

    public static final int INV_X = 108;
    public static final int INV_Y = 84;
    public static final int HOTBAR_Y = 142;

    public static final int BUTTON_TOGGLE_DIRECTION = 100;
    public static final int BUTTON_CLAIM_OUTPUT = 101;
    public static final int BUTTON_CANCEL_TRADE = 102;
    public static final int BUTTON_TAB_BASE = 200;

    public static final int MAX_OUTPUT_AMOUNT = 32767;

    public static final int INPUT_SLOT = 0;
    public static final int INPUT_SLOT2 = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int SLOT_COUNT = OUTPUT_SLOT + 1;
    private static final int[] INPUT_SLOTS = { INPUT_SLOT, INPUT_SLOT2 };

    private final Container tradeContainer;
    private final List<TraderTab> tabs = new ArrayList<>();
    private final Player player;
    private final Item baseTradeItem;

    @Nullable
    private final WanderingTrader trader;

    private final DataSlot outputAmount = DataSlot.standalone();

    private int activeTab = 0;
    private int selectedOffer = 0;
    private boolean merchantSelling = true;
    private boolean updatingOutput = false;

    public EnhancedTraderMenu(int syncId, Inventory playerInventory, @Nullable WanderingTrader trader, Item baseTradeItem)
    {
        this(syncId, playerInventory, trader, tabsFor(trader), baseTradeItem);
    }

    public EnhancedTraderMenu(int syncId, Inventory playerInventory, @Nullable WanderingTrader trader,
                              List<TraderTab> tabs, Item baseTradeItem)
    {
        super(ModMenus.enhancedTraderMenu.get(), syncId);
        this.player = playerInventory.player;
        this.trader = trader;
        this.baseTradeItem = ModConfig.getInstance().getBaseTradeItem();
        if(tabs != null) this.tabs.addAll(tabs.subList(0, Math.min(tabs.size(), MAX_TABS)));

        this.tradeContainer = new SimpleContainer(SLOT_COUNT) {
            @Override
            public int getMaxStackSize() {
                return ModConfig.getInstance().getMaxStackSize();
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (!EnhancedTraderMenu.this.updatingOutput)
                    EnhancedTraderMenu.this.updateOutput();
            }
        };

        this.addSlot(new InputSlot(tradeContainer, INPUT_SLOT, INPUT_A_X, INPUT_A_Y));
        this.addSlot(new InputSlot(tradeContainer, INPUT_SLOT2, INPUT_B_X, INPUT_B_Y));
        this.addSlot(new OutputSlot(tradeContainer, OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }

        this.addDataSlot(outputAmount);
    }

    public static EnhancedTraderMenu fromNetwork(int syncId, Inventory playerInventory, FriendlyByteBuf buf) {
        Item baseTradeItem = buf.readItem().getItem();
        int count = buf.readVarInt();
        List<TraderTab> tabs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) tabs.add(TraderTab.read(buf));
        return new EnhancedTraderMenu(syncId, playerInventory, null, tabs, baseTradeItem);
    }

    public static List<TraderTab> tabsFor(@Nullable WanderingTrader trader) {
        if (trader == null) return Collections.emptyList();
        return TradeManager.buildTabs(EnhancedTraderData.offersFor(trader));
    }

    public static void writeOpeningData(FriendlyByteBuf buf, WanderingTrader trader, Item baseTradeItem) {
        List<TraderTab> tabs = tabsFor(trader);
        buf.writeItem(new ItemStack(baseTradeItem));
        int count = Math.min(tabs.size(), MAX_TABS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) tabs.get(i).write(buf);
    }


    public List<TraderTab> getTabs() { return Collections.unmodifiableList(tabs); }

    public int getActiveTab() { return activeTab; }

    @Nullable
    public TraderTab getActiveTabData() {
        if (tabs.isEmpty()) return null;
        return tabs.get(Math.max(0, Math.min(activeTab, tabs.size() - 1)));
    }

    public List<TraderOffer> getOffers() {
        TraderTab tab = getActiveTabData();
        return (tab == null) ? Collections.emptyList() : tab.getOffers();
    }

    public int getSelectedOffer() { return selectedOffer; }

    @Nullable
    public TraderOffer getSelected() {
        List<TraderOffer> offers = getOffers();
        if (offers.isEmpty()) return null;
        return offers.get(Math.max(0, Math.min(selectedOffer, offers.size() - 1)));
    }

    public boolean isMerchantSelling() { return merchantSelling; }

    public Item getBaseTradeItem() { return baseTradeItem; }

    public Container getTradeContainer() { return tradeContainer; }

    public int getOutputAmount() { return outputAmount.get(); }

    @Nullable
    public Item getOutputItem() {
        ItemStack icon = tradeContainer.getItem(OUTPUT_SLOT);
        return icon.isEmpty() ? null : icon.getItem();
    }

    public void setActiveTab(int tab) {
        if (tab < 0 || tab >= tabs.size()) return;
        this.activeTab = tab;
        this.selectedOffer = 0;
    }

    public void setSelectedOffer(int index) {
        if (index < 0 || index >= getOffers().size()) return;
        this.selectedOffer = index;
    }

    public void setMerchantSelling(boolean merchantSelling) {
        this.merchantSelling = merchantSelling;
    }

    public void applyUses(List<Integer> uses) {
        if (uses == null) return;
        int i = 0;
        for (TraderTab tab : tabs) {
            for (TraderOffer offer : tab.getOffers()) {
                if (i >= uses.size()) return;
                offer.setWaresSold(uses.get(i++));
            }
        }
    }

    public List<Integer> collectUses() {
        List<Integer> uses = new ArrayList<>();
        for (TraderTab tab : tabs) {
            for (TraderOffer offer : tab.getOffers()) uses.add(offer.getUses());
        }
        return uses;
    }

    private void syncOffers() {
        if (trader == null || !(player instanceof ServerPlayer serverPlayer)) return;
        HBUtil.NetworkUtil.serverSendToPlayer(serverPlayer, new TraderOffersSync(this.containerId, collectUses()));
    }


    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == tradeContainer) updateOutput();
    }

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id == BUTTON_TOGGLE_DIRECTION) {
            cancelTrade(player);
            this.merchantSelling = !this.merchantSelling;
            updateOutput();
            return true;
        }
        if (id == BUTTON_CLAIM_OUTPUT) {
            takeTrade(player);
            return true;
        }
        if (id == BUTTON_CANCEL_TRADE) {
            cancelTrade(player);
            return true;
        }
        if (id >= BUTTON_TAB_BASE && id < BUTTON_TAB_BASE + tabs.size()) {
            cancelTrade(player);
            setActiveTab(id - BUTTON_TAB_BASE);
            updateOutput();
            return true;
        }
        if (id >= 0 && id < BUTTON_TOGGLE_DIRECTION && id < getOffers().size()) {
            cancelTrade(player);
            setSelectedOffer(id);
            updateOutput();
            return true;
        }
        return false;
    }


    @Nullable
    private Item expectedInputItem(@Nullable TraderOffer offer) {
        if (offer == null) return null;
        return merchantSelling ? baseTradeItem : offer.getItem();
    }

    private int inputCostPerTrade(TraderOffer offer) {
        return merchantSelling ? offer.getMarkupPrice() : offer.getQuantity();
    }

    private int payoutPerTrade(TraderOffer offer) {
        return merchantSelling ? offer.getQuantity() : offer.getMarketPrice();
    }

    private Item payoutItem(TraderOffer offer) {
        return merchantSelling ? offer.getItem() : baseTradeItem;
    }

    private int availableTrades(@Nullable TraderOffer offer)
    {
        if (offer == null) return 0;
        int cost = inputCostPerTrade(offer);
        int payout = payoutPerTrade(offer);
        if (cost <= 0 || payout <= 0) return 0;

        int trades = countInputs(expectedInputItem(offer)) / cost;
        if (!merchantSelling) return Math.min(trades, 1);

        if (offer.isOutOfStock()) return 0;
        trades = Math.min(trades, offer.getRemainingWaresCount());
        return Math.min(trades, MAX_OUTPUT_AMOUNT / payout);
    }

    private void updateOutput()
    {
        if (updatingOutput) return;
        updatingOutput = true;
        try {
            computeOutput();
        } finally {
            updatingOutput = false;
        }
    }

    private void computeOutput()
    {
        TraderOffer offer = getSelected();
        int trades = availableTrades(offer);
        if (trades <= 0) {
            clearOutput();
            return;
        }
        setOutput(payoutItem(offer), trades * payoutPerTrade(offer));
    }

    private void setOutput(@Nullable Item item, int amount)
    {
        ItemStack display = (item == null || amount <= 0) ? ItemStack.EMPTY : new ItemStack(item, amount);
        outputAmount.set(display.isEmpty() ? 0 : amount);
        tradeContainer.setItem(OUTPUT_SLOT, display);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                this.containerId, this.incrementStateId(), OUTPUT_SLOT, display.copy()));
        }
    }

    private void clearOutput() {
        setOutput(null, 0);
    }

    private void takeTrade(Player recipient)
    {
        TraderOffer offer = getSelected();
        if (offer == null || trader == null) return;

        int trades = availableTrades(offer);
        if (trades <= 0) return;

        if (merchantSelling) offer.waresBought(trades);
        consumeInputs(trades * inputCostPerTrade(offer));
        givePlayer(recipient, payoutItem(offer), trades * payoutPerTrade(offer));

        updateOutput();
        if (merchantSelling) syncOffers();
    }

    private void givePlayer(Player recipient, Item item, int amount)
    {
        if (item == null || amount <= 0) return;
        recipient.getInventory().placeItemBackInInventory(new ItemStack(item, amount));
    }

    private int countInputs(@Nullable Item item)
    {
        if (item == null) return 0;
        int count = 0;
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = tradeContainer.getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private void consumeInputs(int amount)
    {
        updatingOutput = true;
        try {
            for (int slot : INPUT_SLOTS) {
                if (amount <= 0) break;
                ItemStack stack = tradeContainer.getItem(slot);
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
                tradeContainer.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        } finally {
            updatingOutput = false;
        }
    }

    private boolean acceptsInput(ItemStack stack)
    {
        TraderOffer offer = getSelected();
        Item expected = expectedInputItem(offer);
        if (expected == null || stack.isEmpty() || !stack.is(expected)) return false;
        return !merchantSelling || !offer.isOutOfStock();
    }

    private void cancelTrade(Player p)
    {
        updatingOutput = true;
        try {
            for (int slot : INPUT_SLOTS) {
                ItemStack stack = tradeContainer.removeItemNoUpdate(slot);
                if (!stack.isEmpty()) p.getInventory().placeItemBackInInventory(stack);
            }
        } finally {
            updatingOutput = false;
        }
        clearOutput();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        if (index == OUTPUT_SLOT) {
            takeTrade(player);
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index < SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!acceptsInput(stack)) return ItemStack.EMPTY;
            if (!this.moveItemStackTo(stack, INPUT_SLOT, OUTPUT_SLOT, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        updateOutput();
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) return;
        if (trader != null && trader.getTradingPlayer() == player) trader.setTradingPlayer(null);

        updatingOutput = true;
        try {
            tradeContainer.setItem(OUTPUT_SLOT, ItemStack.EMPTY);
            this.clearContainer(player, tradeContainer);
        } finally {
            updatingOutput = false;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (trader == null) return true;
        return trader.isAlive() && trader.distanceToSqr(player) <= 64.0D;
    }


    private class InputSlot extends Slot {
        InputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EnhancedTraderMenu.this.acceptsInput(stack);
        }
    }

    private class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
