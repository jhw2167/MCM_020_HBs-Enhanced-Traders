package com.holybuckets.enhancedtrader.menu;

import com.holybuckets.enhancedtrader.config.ModConfig;
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
import net.minecraft.world.item.Items;

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
    public static final int TRADE_LIST_HEIGHT = 120;
    public static final int TRADE_ROW_HEIGHT = 20;
    public static final int VISIBLE_ROWS = TRADE_LIST_HEIGHT / TRADE_ROW_HEIGHT;

    public static final int SCROLLBAR_X = 94;
    public static final int SCROLLBAR_Y = 18;
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int SCROLLBAR_HEIGHT = 120;

    public static final int INPUT_A_X = 136;
    public static final int INPUT_A_Y = 37;
    public static final int INPUT_B_X = 162;
    public static final int INPUT_B_Y = 37;
    public static final int OUTPUT_X = 220;
    public static final int OUTPUT_Y = 37;
    public static final int ARROW_X = 186;
    public static final int ARROW_Y = 38;

    public static final int REVERSE_SIZE = 16;
    public static final int REVERSE_X = TRADE_LIST_X + 1;
    public static final int REVERSE_Y = TRADE_LIST_Y + TRADE_LIST_HEIGHT + 3;

    public static final int TAB_WIDTH = 26;
    public static final int TAB_HEIGHT = 32;
    public static final int TAB_SPACING = 27;
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
    public static final int MAX_SYNC_COUNT = 127;

    public static final int INPUT_SLOT = 0;
    public static final int STACK_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int SLOT_COUNT = OUTPUT_SLOT + 1;

    private final Container tradeContainer;
    private final List<TraderTab> tabs = new ArrayList<>();
    private final Player player;
    private final Item baseTradeItem;

    @Nullable
    private final WanderingTrader trader;

    private final DataSlot outputAmount = DataSlot.standalone();

    private int activeTab = 0;
    private int selectedOffer = 0;
    private boolean selling = false;
    private boolean updatingOutput = false;

    public EnhancedTraderMenu(int syncId, Inventory playerInventory, @Nullable WanderingTrader trader, Item baseTradeItem)
    {
        super(TraderMenus.enhancedTraderMenu.get(), syncId);
        this.player = playerInventory.player;
        this.trader = trader;
        this.baseTradeItem = (baseTradeItem == null) ? Items.IRON_NUGGET : baseTradeItem;
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
        this.addSlot(new StackCounterSlot(tradeContainer, STACK_SLOT, INPUT_B_X, INPUT_B_Y));
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

    public static void writeOpeningData(FriendlyByteBuf buf, List<TraderTab> tabs, Item baseTradeItem) {
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

    public boolean isSelling() { return selling; }

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

    public void setSelling(boolean selling) {
        this.selling = selling;
    }

    public void applyUses(List<Integer> uses) {
        if (uses == null) return;
        int i = 0;
        for (TraderTab tab : tabs) {
            for (TraderOffer offer : tab.getOffers()) {
                if (i >= uses.size()) return;
                offer.setUses(uses.get(i++));
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
            this.selling = !this.selling;
            updateOutput();
            return true;
        }
        if (id == BUTTON_CLAIM_OUTPUT) {
            claimOutput(player);
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
        return selling ? offer.getItem() : baseTradeItem;
    }

    private Item payoutItem(TraderOffer offer) {
        return selling ? baseTradeItem : offer.getItem();
    }

    private int unitCost(TraderOffer offer) {
        return selling ? offer.getQuantity() : offer.getBuyPrice();
    }

    private int unitPayout(TraderOffer offer) {
        return selling ? offer.getSellPrice() : offer.getQuantity();
    }

    private int tradeCount(@Nullable TraderOffer offer) {
        if (offer == null || offer.isOutOfStock()) return 0;
        int cost = unitCost(offer);
        int payout = unitPayout(offer);
        if (cost <= 0 || payout <= 0) return 0;

        int trades = countInput(expectedInputItem(offer)) / cost;
        trades = Math.min(trades, offer.getRemainingUses());
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
        if (offer == null) {
            clearOutput();
            return;
        }

        normalizeInput();
        int trades = tradeCount(offer);
        if (trades <= 0) {
            clearOutput();
            return;
        }
        setOutput(payoutItem(offer), trades * unitPayout(offer));
    }

    private void setOutput(@Nullable Item item, int amount)
    {
        int clamped = Math.max(0, Math.min(amount, MAX_OUTPUT_AMOUNT));
        ItemStack icon = (item == null || clamped <= 0) ? ItemStack.EMPTY : new ItemStack(item, 1);

        outputAmount.set(clamped);
        tradeContainer.setItem(OUTPUT_SLOT, icon);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                this.containerId, this.incrementStateId(), OUTPUT_SLOT, icon));
        }
    }

    private void clearOutput() {
        setOutput(null, 0);
    }

    private void claimOutput(Player recipient)
    {
        TraderOffer offer = getSelected();
        if (offer == null || trader == null) return;

        normalizeInput();
        int trades = tradeCount(offer);
        if (trades <= 0) return;

        Item inputItem = expectedInputItem(offer);
        int leftover = Math.max(0, countInput(inputItem) - trades * unitCost(offer));

        offer.addUses(trades);
        givePlayer(recipient, payoutItem(offer), trades * unitPayout(offer));
        setInputRaw(inputItem, leftover);
        updateOutput();
        syncOffers();
    }

    private void givePlayer(Player recipient, Item item, int amount)
    {
        int unit = stackUnit(item);
        int remaining = amount;
        while (remaining > 0) {
            int give = Math.min(unit, remaining);
            recipient.getInventory().placeItemBackInInventory(new ItemStack(item, give));
            remaining -= give;
        }
    }

    private static int stackUnit(@Nullable Item item) {
        if (item == null) return 64;
        return Math.max(1, item.getMaxStackSize());
    }

    private int countInput(@Nullable Item item) {
        if (item == null) return 0;
        int total = 0;
        ItemStack loose = tradeContainer.getItem(INPUT_SLOT);
        if (loose.is(item)) total += loose.getCount();

        ItemStack counted = tradeContainer.getItem(STACK_SLOT);
        if (counted.is(item)) total += counted.getCount() * stackUnit(item);
        return total;
    }

    private boolean acceptsInput(ItemStack stack) {
        Item expected = expectedInputItem(getSelected());
        return expected != null && !stack.isEmpty() && stack.is(expected) && !getSelected().isOutOfStock();
    }

    private void normalizeInput()
    {
        ItemStack loose = tradeContainer.getItem(INPUT_SLOT);
        if (loose.isEmpty()) return;

        int unit = stackUnit(loose.getItem());
        if (loose.getCount() < unit) return;

        ItemStack counted = tradeContainer.getItem(STACK_SLOT);
        if (!counted.isEmpty() && !counted.is(loose.getItem())) return;

        int held = counted.isEmpty() ? 0 : counted.getCount();
        int drained = Math.min(loose.getCount() / unit, MAX_SYNC_COUNT - held);
        if (drained <= 0) return;

        int remainder = loose.getCount() - drained * unit;
        Item item = loose.getItem();

        boolean wasUpdating = updatingOutput;
        updatingOutput = true;
        try {
            tradeContainer.setItem(STACK_SLOT, new ItemStack(item, held + drained));
            tradeContainer.setItem(INPUT_SLOT, remainder > 0 ? new ItemStack(item, remainder) : ItemStack.EMPTY);
        } finally {
            updatingOutput = wasUpdating;
        }
    }

    private boolean mergeIntoPartialInputSlot(ItemStack incoming)
    {
        if (!acceptsInput(incoming)) return false;

        ItemStack loose = tradeContainer.getItem(INPUT_SLOT);
        int stackSize = stackUnit(incoming.getItem());

        if (loose.isEmpty()) {
            int move = Math.min(incoming.getCount(), stackSize);
            tradeContainer.setItem(INPUT_SLOT, incoming.split(move));
            return true;
        }

        int room = stackSize - loose.getCount();
        if (room <= 0) return false;

        int move = Math.min(room, incoming.getCount());
        loose.grow(move);
        incoming.shrink(move);
        tradeContainer.setChanged();
        return true;
    }

    private void cancelTrade(Player p)
    {
        updatingOutput = true;
        try {
            ItemStack loose = tradeContainer.getItem(INPUT_SLOT);
            if (!loose.isEmpty()) givePlayer(p, loose.getItem(), loose.getCount());

            ItemStack counted = tradeContainer.getItem(STACK_SLOT);
            if (!counted.isEmpty())
                givePlayer(p, counted.getItem(), counted.getCount() * stackUnit(counted.getItem()));

            tradeContainer.setItem(INPUT_SLOT, ItemStack.EMPTY);
            tradeContainer.setItem(STACK_SLOT, ItemStack.EMPTY);
        } finally {
            updatingOutput = false;
        }
        clearOutput();
    }

    private void setInputRaw(@Nullable Item item, int rawCount)
    {
        updatingOutput = true;
        try {
            if (item == null || rawCount <= 0) {
                tradeContainer.setItem(INPUT_SLOT, ItemStack.EMPTY);
                tradeContainer.setItem(STACK_SLOT, ItemStack.EMPTY);
            } else {
                int unit = stackUnit(item);
                int stacks = Math.min(rawCount / unit, MAX_SYNC_COUNT);
                int remainder = rawCount - stacks * unit;
                tradeContainer.setItem(STACK_SLOT, stacks > 0 ? new ItemStack(item, stacks) : ItemStack.EMPTY);
                tradeContainer.setItem(INPUT_SLOT, remainder > 0 ? new ItemStack(item, remainder) : ItemStack.EMPTY);
            }
        } finally {
            updatingOutput = false;
        }
        clearOutput();
    }

    private boolean moveEntireStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection)
    {
        boolean moved = false;
        while (!stack.isEmpty()) {
            if (!this.moveItemStackTo(stack, startIndex, endIndex, reverseDirection)) break;
            moved = true;
        }
        return moved;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        final int invStart = SLOT_COUNT;
        final int invEnd = this.slots.size();

        if (index == OUTPUT_SLOT) {
            claimOutput(player);
            return ItemStack.EMPTY;
        } else if (index == STACK_SLOT) {
            cancelTrade(player);
            return ItemStack.EMPTY;
        } else if (index < invStart) {
            if (!moveEntireStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!mergeIntoPartialInputSlot(stack)) return ItemStack.EMPTY;
            normalizeInput();
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
            ItemStack loose = tradeContainer.getItem(INPUT_SLOT);
            if (!loose.isEmpty()) player.getInventory().placeItemBackInInventory(loose);

            ItemStack counted = tradeContainer.getItem(STACK_SLOT);
            if (!counted.isEmpty()) {
                int unit = stackUnit(counted.getItem());
                for (int i = 0; i < counted.getCount(); i++)
                    player.getInventory().placeItemBackInInventory(new ItemStack(counted.getItem(), unit));
            }

            tradeContainer.setItem(INPUT_SLOT, ItemStack.EMPTY);
            tradeContainer.setItem(STACK_SLOT, ItemStack.EMPTY);
            tradeContainer.setItem(OUTPUT_SLOT, ItemStack.EMPTY);
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

    private class StackCounterSlot extends Slot {
        StackCounterSlot(Container container, int index, int x, int y) {
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

        @Override
        public int getMaxStackSize() {
            return MAX_SYNC_COUNT;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return MAX_SYNC_COUNT;
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

        @Override
        public int getMaxStackSize() {
            return ModConfig.getInstance().getMaxStackSize();
        }
    }
}
