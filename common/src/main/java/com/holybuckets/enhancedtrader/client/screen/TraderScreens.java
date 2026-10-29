package com.holybuckets.enhancedtrader.client.screen;

import com.holybuckets.enhancedtrader.menu.TraderMenus;
import net.blay09.mods.balm.api.client.screen.BalmScreens;

public class TraderScreens {

    public static void clientInitialize(BalmScreens screens) {
        screens.registerScreen(
            TraderMenus.enhancedTraderMenu::get,
            EnhancedTraderScreen::new
        );
    }
}
