package com.holybuckets.enhancedtrader.networking;

import com.holybuckets.enhancedtrader.Constants;
import com.holybuckets.enhancedtrader.menu.EnhancedTraderMenu;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class TraderNetworking {

    public static void initialize(BalmNetworking networking) {
        networking.registerClientboundPacket(
            id(TraderOffersSync.LOCATION),
            TraderOffersSync.class,
            TraderOffersSync::encode,
            TraderOffersSync::decode,
            TraderNetworking::handleTraderOffersSync);
    }

    public static void handleTraderOffersSync(Player p, TraderOffersSync m) {
        if (p == null || m == null) return;
        if (p.containerMenu instanceof EnhancedTraderMenu menu && menu.containerId == m.getContainerId())
            menu.applyUses(m.getUses());
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }
}
