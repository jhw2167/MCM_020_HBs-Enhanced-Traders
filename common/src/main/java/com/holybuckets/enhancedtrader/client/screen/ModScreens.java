package com.holybuckets.template.client.screen;

import com.holybuckets.template.menu.ModMenus;
import net.blay09.mods.balm.api.client.screen.BalmScreens;

public class ModScreens {
    public static void clientInitialize(BalmScreens screens) {
        screens.registerScreen(
            ModMenus.countingChestMenu::get,
            CountingChestScreen::new
        );
    }

}
