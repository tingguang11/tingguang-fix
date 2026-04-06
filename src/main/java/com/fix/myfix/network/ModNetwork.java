package com.fix.myfix.network;

import com.fix.myfix.MyFix;
import com.fix.myfix.system.CharcoalPitSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MyFix.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(SyncCharcoalPitsPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCharcoalPitsPacket::encode)
                .decoder(SyncCharcoalPitsPacket::decode)
                .consumerMainThread(SyncCharcoalPitsPacket::handle)
                .add();
    }

    public static void syncCharcoalPits(ServerLevel level, CharcoalPitSavedData data) {
        SyncCharcoalPitsPacket packet = SyncCharcoalPitsPacket.fromLevel(level, data);
        for (ServerPlayer player : level.players()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void syncCharcoalPits(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                SyncCharcoalPitsPacket.fromLevel(level, CharcoalPitSavedData.get(level)));
    }
}
