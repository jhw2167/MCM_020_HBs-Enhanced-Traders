package com.holybuckets.enhancedtrader;

import com.holybuckets.enhancedtrader.client.CommonClassClient;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod( Constants.MOD_ID)
public class EnhancedWanderingTradersMainForge {

    public EnhancedWanderingTradersMainForge() {
        super();
        Balm.initialize(Constants.MOD_ID, CommonClass::init);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> BalmClient.initialize(Constants.MOD_ID, CommonClassClient::initClient));
    }


}
