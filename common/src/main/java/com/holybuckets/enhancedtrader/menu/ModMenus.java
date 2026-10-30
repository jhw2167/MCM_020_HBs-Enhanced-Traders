package com.holybuckets.enhancedtrader.menu;

import com.holybuckets.enhancedtrader.Constants;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.menu.BalmMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {

    public static DeferredObject<MenuType<EnhancedTraderMenu>> enhancedTraderMenu;

    public static void initialize(BalmMenus menus) {
        enhancedTraderMenu = menus.registerMenu(id("enhanced_trader_menu"),
            (syncId, inventory, buf) -> EnhancedTraderMenu.fromNetwork(syncId, inventory, buf));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
