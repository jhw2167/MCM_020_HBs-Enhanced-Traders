package com.holybuckets.enhancedtrader.menu;

import com.holybuckets.enhancedtrader.config.ModConfig;
import net.blay09.mods.balm.api.menu.BalmMenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

public class EnhancedTraderMenuProvider implements BalmMenuProvider {

    private final WanderingTrader trader;

    public EnhancedTraderMenuProvider(WanderingTrader trader) {
        this.trader = trader;
    }

    @Override
    public Component getDisplayName() {
        if (trader != null && trader.hasCustomName()) return trader.getCustomName();
        return Component.translatable("menu.hbs_enhancedtrader.trader");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        return new EnhancedTraderMenu(syncId, inventory, trader,
            ModConfig.getInstance().getBaseTradeItem());
    }

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        EnhancedTraderMenu.writeOpeningData(buf, ModConfig.getInstance().getBaseTradeItem());
    }
}
