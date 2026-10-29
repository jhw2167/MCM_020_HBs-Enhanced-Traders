package com.holybuckets.enhancedtrader.client.screen;

import com.holybuckets.enhancedtrader.Constants;
import com.holybuckets.enhancedtrader.core.model.TraderOffer;
import com.holybuckets.enhancedtrader.core.model.TraderTab;
import com.holybuckets.enhancedtrader.menu.EnhancedTraderMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EnhancedTraderScreen extends AbstractContainerScreen<EnhancedTraderMenu> {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(Constants.MOD_ID, "textures/gui/trader_screen_gui.png");
    private static final ResourceLocation VILLAGER_LOCATION =
        new ResourceLocation("textures/gui/container/villager2.png");
    private static final ResourceLocation TABS_LOCATION =
        new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    private static final int TEXTURE_WIDTH = 512;
    private static final int TEXTURE_HEIGHT = 256;

    private static final int ARROW_U = 15;
    private static final int OUT_OF_STOCK_U = 25;
    private static final int ARROW_V = 171;
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 9;

    private static final int OFFER_COST_X = 8;
    private static final int OFFER_ARROW_X = 34;
    private static final int OFFER_RESULT_X = 54;
    private static final int OFFER_ITEM_Y = 2;
    private static final int OFFER_ARROW_Y = 6;

    private static final int TAB_SELECTED_V = 32;
    private static final int TAB_ICON_X = 5;
    private static final int TAB_ICON_Y = 9;

    private static final int TITLE_Y = 6;
    private static final int TITLE_MARGIN = 8;

    private static final String REVERSE_SYMBOL = "↺";

    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_SELLING = 0x2E8B2E;
    private static final int COLOR_BUYING = 0x30509B;
    private static final int COLOR_ROW_SELECTED = 0xFFFFFFA0;
    private static final int COLOR_ROW_SOLD_OUT = 0x80000000;
    private static final int COLOR_SCROLL = 0xFFC6C6C6;

    private final TradeOfferButton[] tradeOfferButtons = new TradeOfferButton[EnhancedTraderMenu.VISIBLE_ROWS];
    private Button reverseButton;
    private int scrollOffset = 0;

    public EnhancedTraderScreen(EnhancedTraderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = EnhancedTraderMenu.GUI_WIDTH;
        this.imageHeight = EnhancedTraderMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        int rowY = this.topPos + EnhancedTraderMenu.TRADE_LIST_Y;
        for (int i = 0; i < this.tradeOfferButtons.length; i++) {
            this.tradeOfferButtons[i] = this.addRenderableWidget(new TradeOfferButton(
                this.leftPos + EnhancedTraderMenu.TRADE_LIST_X, rowY, i, b -> {
                    if (b instanceof TradeOfferButton offerButton) selectOffer(offerButton.getIndex() + scrollOffset);
                }));
            rowY += EnhancedTraderMenu.TRADE_ROW_HEIGHT;
        }

        this.reverseButton = Button.builder(Component.literal(REVERSE_SYMBOL), b -> toggleDirection())
            .bounds(this.leftPos + EnhancedTraderMenu.REVERSE_X, this.topPos + EnhancedTraderMenu.REVERSE_Y,
                EnhancedTraderMenu.REVERSE_SIZE, EnhancedTraderMenu.REVERSE_SIZE)
            .tooltip(Tooltip.create(reverseTooltip()))
            .build();
        this.addRenderableWidget(this.reverseButton);
    }


    private void selectOffer(int index) {
        if (index < 0 || index >= this.menu.getOffers().size()) return;
        this.menu.setSelectedOffer(index);
        sendButton(index);
    }

    private void selectTab(int tab) {
        if (tab == this.menu.getActiveTab() || tab < 0 || tab >= this.menu.getTabs().size()) return;
        this.menu.setActiveTab(tab);
        this.scrollOffset = 0;
        sendButton(EnhancedTraderMenu.BUTTON_TAB_BASE + tab);
    }

    private void toggleDirection() {
        this.menu.setSelling(!this.menu.isSelling());
        sendButton(EnhancedTraderMenu.BUTTON_TOGGLE_DIRECTION);
        this.reverseButton.setTooltip(Tooltip.create(reverseTooltip()));
    }

    private Component reverseTooltip() {
        return this.menu.isSelling()
            ? Component.translatable("screen.hbs_enhancedtrader.switch_to_buying")
            : Component.translatable("screen.hbs_enhancedtrader.switch_to_selling");
    }

    private Component directionLabel() {
        return this.menu.isSelling()
            ? Component.translatable("screen.hbs_enhancedtrader.selling")
            : Component.translatable("screen.hbs_enhancedtrader.buying");
    }

    private void sendButton(int id) {
        if (this.minecraft == null || this.minecraft.gameMode == null) return;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }


    private int tabX(int tab) {
        return this.leftPos + tab * EnhancedTraderMenu.TAB_SPACING;
    }

    private int tabY() {
        return this.topPos + EnhancedTraderMenu.TAB_Y;
    }

    private int hoveredTab(double mouseX, double mouseY) {
        int count = this.menu.getTabs().size();
        for (int i = 0; i < count; i++) {
            int x = tabX(i);
            int y = tabY();
            if (mouseX >= x && mouseX < x + EnhancedTraderMenu.TAB_WIDTH
                && mouseY >= y && mouseY < y + EnhancedTraderMenu.TAB_HEIGHT - 4) return i;
        }
        return -1;
    }

    private void renderTabs(GuiGraphics gui, boolean selectedPass) {
        List<TraderTab> tabs = this.menu.getTabs();
        for (int i = 0; i < tabs.size(); i++) {
            boolean selected = i == this.menu.getActiveTab();
            if (selected != selectedPass) continue;

            int x = tabX(i);
            int y = tabY();
            int u = (i == 0 ? 0 : 1) * EnhancedTraderMenu.TAB_WIDTH;
            int v = selected ? TAB_SELECTED_V : 0;
            gui.blit(TABS_LOCATION, x, y, u, v, EnhancedTraderMenu.TAB_WIDTH, EnhancedTraderMenu.TAB_HEIGHT);

            gui.pose().pushPose();
            gui.pose().translate(0.0F, 0.0F, 100.0F);
            gui.renderFakeItem(tabs.get(i).getIcon(), x + TAB_ICON_X, y + TAB_ICON_Y);
            gui.pose().popPose();
        }
    }


    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        renderTabs(gui, false);
        gui.blit(TEXTURE, this.leftPos, this.topPos, 0, 0.0F, 0.0F,
            this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        renderTabs(gui, true);
        renderScrollbar(gui);
    }

    private void renderOffers(GuiGraphics gui)
    {
        List<TraderOffer> offers = this.menu.getOffers();
        if (offers.isEmpty()) return;

        ItemStack currency = new ItemStack(this.menu.getBaseTradeItem());
        boolean selling = this.menu.isSelling();
        int rows = Math.min(this.tradeOfferButtons.length, Math.max(0, offers.size() - scrollOffset));

        for (int i = 0; i < rows; i++)
        {
            int index = i + scrollOffset;
            TraderOffer offer = offers.get(index);

            int x = this.leftPos + EnhancedTraderMenu.TRADE_LIST_X;
            int y = this.topPos + EnhancedTraderMenu.TRADE_LIST_Y + i * EnhancedTraderMenu.TRADE_ROW_HEIGHT;

            gui.pose().pushPose();
            gui.pose().translate(0.0F, 0.0F, 100.0F);

            if (index == this.menu.getSelectedOffer()) renderSelectionOutline(gui, x, y);

            if (selling) {
                renderCount(gui, offer.getIcon(), x + OFFER_COST_X, y + OFFER_ITEM_Y, offer.getQuantity());
                renderCount(gui, currency, x + OFFER_RESULT_X, y + OFFER_ITEM_Y, offer.getSellPrice());
            } else {
                renderCount(gui, currency, x + OFFER_COST_X, y + OFFER_ITEM_Y, offer.getBuyPrice());
                renderCount(gui, offer.getIcon(), x + OFFER_RESULT_X, y + OFFER_ITEM_Y, offer.getQuantity());
            }

            RenderSystem.enableBlend();
            int arrowU = offer.isOutOfStock() ? OUT_OF_STOCK_U : ARROW_U;
            gui.blit(VILLAGER_LOCATION, x + OFFER_ARROW_X, y + OFFER_ARROW_Y, 0,
                arrowU, ARROW_V, ARROW_WIDTH, ARROW_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

            gui.pose().popPose();

            if (offer.isOutOfStock()) {
                gui.pose().pushPose();
                gui.pose().translate(0.0F, 0.0F, 300.0F);
                gui.fill(x + 1, y + 1, x + EnhancedTraderMenu.TRADE_LIST_WIDTH - 1,
                    y + EnhancedTraderMenu.TRADE_ROW_HEIGHT - 1, COLOR_ROW_SOLD_OUT);
                gui.pose().popPose();
            }
        }
    }

    private void renderCount(GuiGraphics gui, ItemStack stack, int x, int y, int count) {
        if (stack.isEmpty()) return;
        gui.renderFakeItem(stack, x, y);
        gui.renderItemDecorations(this.font, stack, x, y, count > 1 ? String.valueOf(count) : null);
    }

    private void renderSelectionOutline(GuiGraphics gui, int x, int y) {
        int w = EnhancedTraderMenu.TRADE_LIST_WIDTH;
        int h = EnhancedTraderMenu.TRADE_ROW_HEIGHT;
        gui.fill(x, y, x + w, y + 1, COLOR_ROW_SELECTED);
        gui.fill(x, y + h - 1, x + w, y + h, COLOR_ROW_SELECTED);
        gui.fill(x, y, x + 1, y + h, COLOR_ROW_SELECTED);
        gui.fill(x + w - 1, y, x + w, y + h, COLOR_ROW_SELECTED);
    }

    private void renderScrollbar(GuiGraphics gui)
    {
        int total = this.menu.getOffers().size();
        if (total <= EnhancedTraderMenu.VISIBLE_ROWS) return;

        int trackX = this.leftPos + EnhancedTraderMenu.SCROLLBAR_X;
        int trackY = this.topPos + EnhancedTraderMenu.SCROLLBAR_Y;
        int handleHeight = Math.max(10, EnhancedTraderMenu.SCROLLBAR_HEIGHT * EnhancedTraderMenu.VISIBLE_ROWS / total);
        int maxScroll = total - EnhancedTraderMenu.VISIBLE_ROWS;
        int travel = EnhancedTraderMenu.SCROLLBAR_HEIGHT - handleHeight;
        int handleY = trackY + (maxScroll == 0 ? 0 : travel * scrollOffset / maxScroll);

        gui.fill(trackX, handleY, trackX + EnhancedTraderMenu.SCROLLBAR_WIDTH, handleY + handleHeight, COLOR_SCROLL);
    }

    private void renderTabTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        int tab = hoveredTab(mouseX, mouseY);
        if (tab < 0) return;
        gui.renderTooltip(this.font, this.menu.getTabs().get(tab).getDisplayName(), mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, EnhancedTraderMenu.TRADE_LIST_X + 2, TITLE_Y, COLOR_TEXT, false);

        TraderTab tab = this.menu.getActiveTabData();
        if (tab != null) {
            Component name = tab.getDisplayName();
            gui.drawString(this.font, name, this.imageWidth - TITLE_MARGIN - this.font.width(name), TITLE_Y, COLOR_TEXT, false);
        }

        Component direction = directionLabel();
        int color = this.menu.isSelling() ? COLOR_SELLING : COLOR_BUYING;
        gui.drawString(this.font, direction,
            EnhancedTraderMenu.REVERSE_X + EnhancedTraderMenu.REVERSE_SIZE + 4,
            EnhancedTraderMenu.REVERSE_Y + 4, color, false);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0) {
            int tab = hoveredTab(mouseX, mouseY);
            if (tab >= 0) {
                selectTab(tab);
                return true;
            }
        }

        if (this.menu.getOutputAmount() > 0
            && this.isHovering(EnhancedTraderMenu.OUTPUT_X, EnhancedTraderMenu.OUTPUT_Y, 16, 16, mouseX, mouseY)) {
            sendButton(EnhancedTraderMenu.BUTTON_CLAIM_OUTPUT);
            return true;
        }

        if (hasShiftDown()
            && this.isHovering(EnhancedTraderMenu.INPUT_B_X, EnhancedTraderMenu.INPUT_B_Y, 16, 16, mouseX, mouseY)) {
            sendButton(EnhancedTraderMenu.BUTTON_CANCEL_TRADE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        if (hoveredTab(mouseX, mouseY) >= 0) return false;
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        int total = this.menu.getOffers().size();
        int maxScroll = Math.max(0, total - EnhancedTraderMenu.VISIBLE_ROWS);
        if (maxScroll > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(delta), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);

        renderOffers(gui);

        for (TradeOfferButton button : this.tradeOfferButtons) {
            if (button == null) continue;
            button.visible = button.getIndex() + scrollOffset < this.menu.getOffers().size();
            if (button.visible && button.isHoveredOrFocused()) button.renderToolTip(gui, mouseX, mouseY);
        }

        RenderSystem.enableDepthTest();
        this.renderTooltip(gui, mouseX, mouseY);
        renderTabTooltip(gui, mouseX, mouseY);
    }


    class TradeOfferButton extends Button {

        private final int index;

        TradeOfferButton(int x, int y, int index, Button.OnPress onPress) {
            super(x, y, EnhancedTraderMenu.TRADE_LIST_WIDTH, EnhancedTraderMenu.TRADE_ROW_HEIGHT,
                CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.index = index;
            this.visible = false;
        }

        public int getIndex() {
            return this.index;
        }

        public void renderToolTip(GuiGraphics gui, int mouseX, int mouseY) {
            List<TraderOffer> offers = EnhancedTraderScreen.this.menu.getOffers();
            int offerIndex = this.index + EnhancedTraderScreen.this.scrollOffset;
            if (!this.isHovered || offerIndex >= offers.size()) return;

            TraderOffer offer = offers.get(offerIndex);
            boolean selling = EnhancedTraderScreen.this.menu.isSelling();
            boolean overResult = mouseX >= this.getX() + OFFER_ARROW_X;
            boolean showCurrency = selling == overResult;

            ItemStack stack = showCurrency
                ? new ItemStack(EnhancedTraderScreen.this.menu.getBaseTradeItem())
                : offer.getIcon();

            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(EnhancedTraderScreen.this.minecraft, stack));
            if (offer.isOutOfStock()) {
                lines.add(Component.translatable("screen.hbs_enhancedtrader.sold_out"));
            } else if (offer.isUnlimited()) {
                lines.add(Component.translatable("screen.hbs_enhancedtrader.unlimited"));
            } else {
                lines.add(Component.translatable("screen.hbs_enhancedtrader.uses", offer.getRemainingUses(), offer.getMaxUses()));
            }
            gui.renderComponentTooltip(EnhancedTraderScreen.this.font, lines, mouseX, mouseY);
        }
    }
}
