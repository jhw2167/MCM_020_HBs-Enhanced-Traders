package com.holybuckets.enhancedtrader.entity;

import com.holybuckets.enhancedtrader.core.TradeManager;
import com.holybuckets.enhancedtrader.core.model.TraderOffer;
import net.minecraft.world.entity.npc.WanderingTrader;

import java.util.List;

public interface EnhancedTraderData {

    List<TraderOffer> getEnhancedTraderOffers();

    static List<TraderOffer> offersFor(WanderingTrader trader) {
        List<TraderOffer> offers = ((EnhancedTraderData) trader).getEnhancedTraderOffers();
        if (offers.isEmpty() && !trader.level().isClientSide())
            offers.addAll(TradeManager.buildOffers(trader));
        return offers;
    }
}
