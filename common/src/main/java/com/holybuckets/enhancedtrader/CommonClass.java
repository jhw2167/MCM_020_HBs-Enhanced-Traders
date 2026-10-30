package com.holybuckets.enhancedtrader;

import com.holybuckets.enhancedtrader.config.EnhancedTraderConfig;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.enhancedtrader.menu.ModMenus;
import com.holybuckets.enhancedtrader.networking.TraderNetworking;
import com.holybuckets.enhancedtrader.platform.Services;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;


public class CommonClass {

    public static boolean isInitialized = false;
    public static void init()
    {
        if (isInitialized)
            return;

        Constants.LOG.info("Hello from Common init on {}! we are currently in a {} environment!", com.holybuckets.enhancedtrader.platform.Services.PLATFORM.getPlatformName(), com.holybuckets.enhancedtrader.platform.Services.PLATFORM.getEnvironmentName());
        Constants.LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        //Initialize Foundations
        com.holybuckets.foundation.FoundationInitializers.commonInitialize();

        if (Services.PLATFORM.isModLoaded(Constants.MOD_ID)) {
            Constants.LOG.info("Hello to " + Constants.MOD_NAME + "!");
        }

        //RegisterConfigs
        //Balm.getConfig().registerConfig(ChallengeTempleConfig.class);
        EnhancedTraderMain.INSTANCE = new EnhancedTraderMain();
        Balm.getConfig().registerConfig(EnhancedTraderConfig.class);
        BalmEventRegister.registerEvents();
        BalmEventRegister.registerCommands();
        ModMenus.initialize(Balm.getMenus());
        TraderNetworking.initialize(Balm.getNetworking());
        
        isInitialized = true;
    }

    /**
     * Description: Run sample tests methods
     */
    public static void sample()
    {

    }
}