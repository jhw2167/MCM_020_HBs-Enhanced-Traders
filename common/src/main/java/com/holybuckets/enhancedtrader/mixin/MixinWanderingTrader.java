package com.holybuckets.enhancedtrader.mixin;

import com.holybuckets.enhancedtrader.core.model.TraderOffer;
import com.holybuckets.enhancedtrader.entity.EnhancedTraderData;
import com.holybuckets.enhancedtrader.menu.EnhancedTraderMenuProvider;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.biome.BiomeAPI;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(WanderingTrader.class)
public abstract class MixinWanderingTrader implements EnhancedTraderData {

    @Unique
    private static final String NBT_OFFERS = "hbsEnhancedOffers";
    private static final String NBT_BIOME = "biome";

    @Unique
    private final List<TraderOffer> offers = new ArrayList<>();

    @Override
    public List<TraderOffer> getEnhancedTraderOffers() {
        return offers;
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void openTraderMenu(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        WanderingTrader trader = (WanderingTrader) (Object) this;
        if (player.getItemInHand(hand).is(Items.VILLAGER_SPAWN_EGG)) return;
        if (!trader.isAlive() || trader.isTrading() || trader.isBaby()) return;

        if (trader.level().isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (EnhancedTraderData.offersFor(trader).isEmpty()) return;

        if (hand == InteractionHand.MAIN_HAND) player.awardStat(Stats.TALKED_TO_VILLAGER);
        trader.setTradingPlayer(player);
        Balm.getNetworking().openMenu(player, new EnhancedTraderMenuProvider(trader));
        cir.setReturnValue(InteractionResult.CONSUME);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveOffers(CompoundTag tag, CallbackInfo ci) {
        WanderingTrader trader = (WanderingTrader) (Object) this;
        Holder<Biome> biomeHolder = trader.level().getBiome(trader.blockPosition());
        String biomeId = HBUtil.LevelUtil.toBiomeResourceLocation(biomeHolder).toString();
        tag.putString(NBT_BIOME, biomeId);
        ListTag list = new ListTag();
        for (TraderOffer offer : offers) list.add(offer.save());
        tag.put(NBT_OFFERS, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadOffers(CompoundTag tag, CallbackInfo ci) {
        offers.clear();
        String biomeId = tag.getString(NBT_BIOME);
        Biome biome = HBUtil.LevelUtil.getBiome(biomeId);
        int days = GeneralConfig.getInstance().getTotalDays(((WanderingTrader) (Object) this).level());
        ListTag list = tag.getList(NBT_OFFERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            TraderOffer offer = TraderOffer.load(biome, list.getCompound(i), days);
            if (offer != null) offers.add(offer);
        }
    }
}
