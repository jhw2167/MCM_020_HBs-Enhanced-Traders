package com.holybuckets.enhancedtrader;

import com.holybuckets.enhancedtrader.config.EnhancedTraderConfig;
import com.holybuckets.enhancedtrader.config.ModConfig;
import com.holybuckets.enhancedtrader.core.TradeManager;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;

public class EnhancedTraderMain {
    private static boolean DEV_MODE = false;

    public static ModConfig MOD_CONFIG;
    public static EnhancedTraderMain INSTANCE;

    public EnhancedTraderMain()
    {
        super();
        INSTANCE = this;
        init();
    }

    private void init()
    {

        /*
        Proxy for external APIs which are platform dependent
        this.portalApi = (PortalApi) Balm.platformProxy()
            .withFabric("com.holybuckets.challengetemple.externalapi.FabricPortalApi")
            .withForge("com.holybuckets.challengetemple.externalapi.ForgePortalApi")
            .build();
            */

        //Events
        EventRegistrar registrar = EventRegistrar.getInstance();
        //ChallengeBlockBehavior.init(registrar);


        //register local events
        registrar.registerOnBeforeServerStarted(this::onServerStarting);

    }

    private void onServerStarting(ServerStartingEvent e) {
        //this.DEV_MODE = CONFIG.devMode;
        EnhancedTraderConfig config = Balm.getConfig().getActiveConfig(EnhancedTraderConfig.class);
        MOD_CONFIG = ModConfig.getInstance();
        ModConfig.configureOnServerStart(config);
        TradeManager.configureOnServerStart(config);
    }


}
