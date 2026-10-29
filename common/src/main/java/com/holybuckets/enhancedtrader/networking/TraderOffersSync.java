package com.holybuckets.enhancedtrader.networking;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TraderOffersSync {

    public static final String LOCATION = "trader_offers_sync";

    private final int containerId;
    private final List<Integer> uses;

    public TraderOffersSync(int containerId, List<Integer> uses) {
        this.containerId = containerId;
        this.uses = (uses == null) ? new ArrayList<>() : new ArrayList<>(uses);
    }

    public int getContainerId() { return containerId; }

    public List<Integer> getUses() { return Collections.unmodifiableList(uses); }

    public static FriendlyByteBuf encode(TraderOffersSync object, FriendlyByteBuf buf) {
        buf.writeVarInt(object.containerId);
        buf.writeVarInt(object.uses.size());
        for (Integer use : object.uses) buf.writeVarInt(use);
        return buf;
    }

    public static TraderOffersSync decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int size = buf.readVarInt();
        List<Integer> uses = new ArrayList<>(size);
        for (int i = 0; i < size; i++) uses.add(buf.readVarInt());
        return new TraderOffersSync(containerId, uses);
    }
}
