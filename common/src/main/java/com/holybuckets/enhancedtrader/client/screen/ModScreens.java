package com.holybuckets.enhancedtrader.client.screen;

import com.holybuckets.enhancedtrader.menu.ModMenus;
import net.blay09.mods.balm.api.client.screen.BalmScreens;

public class ModScreens {
    public static void clientInitialize(BalmScreens screens) {
        screens.registerScreen(
            ModMenus.enhancedTraderMenu::get,
            EnhancedTraderScreen::new
        );
    }

}
