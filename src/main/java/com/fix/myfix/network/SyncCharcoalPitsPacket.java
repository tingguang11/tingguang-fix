package com.fix.myfix.network;

import com.fix.myfix.client.CharcoalPitClientCache;
import com.fix.myfix.system.CharcoalPitSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record SyncCharcoalPitsPacket(ResourceLocation dimensionId, List<StructureEntry> structures) {
    public static SyncCharcoalPitsPacket fromLevel(ServerLevel level, CharcoalPitSavedData data) {
        List<StructureEntry> entries = new ArrayList<>();
        for (CharcoalPitSavedData.CharcoalPitSyncEntry entry : data.getSyncEntries(level)) {
            entries.add(new StructureEntry(
                    entry.masterPos(),
                    new ArrayList<>(entry.members()),
                    entry.endGameTime(),
                    entry.totalTicks(),
                    entry.currentSealedRatio(),
                    entry.qualifyingRatio(),
                    entry.projectedToSucceed()
            ));
        }

        return new SyncCharcoalPitsPacket(level.dimension().location(), entries);
    }

    public static void encode(SyncCharcoalPitsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimensionId);
        buffer.writeVarInt(packet.structures.size());
        for (StructureEntry entry : packet.structures) {
            buffer.writeLong(entry.masterPos);
            buffer.writeVarInt(entry.members.size());
            for (Long member : entry.members) {
                buffer.writeLong(member);
            }
            buffer.writeLong(entry.endGameTime);
            buffer.writeVarInt(entry.totalTicks);
            buffer.writeDouble(entry.currentSealedRatio);
            buffer.writeDouble(entry.qualifyingRatio);
            buffer.writeBoolean(entry.projectedToSucceed);
        }
    }

    public static SyncCharcoalPitsPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimensionId = buffer.readResourceLocation();
        int structureCount = buffer.readVarInt();
        List<StructureEntry> structures = new ArrayList<>(structureCount);

        for (int i = 0; i < structureCount; i++) {
            long masterPos = buffer.readLong();
            int memberCount = buffer.readVarInt();
            List<Long> members = new ArrayList<>(memberCount);
            for (int j = 0; j < memberCount; j++) {
                members.add(buffer.readLong());
            }
            long endGameTime = buffer.readLong();
            int totalTicks = buffer.readVarInt();
            double currentSealedRatio = buffer.readDouble();
            double qualifyingRatio = buffer.readDouble();
            boolean projectedToSucceed = buffer.readBoolean();
            structures.add(new StructureEntry(masterPos, members, endGameTime, totalTicks, currentSealedRatio, qualifyingRatio, projectedToSucceed));
        }

        return new SyncCharcoalPitsPacket(dimensionId, structures);
    }

    public static void handle(SyncCharcoalPitsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CharcoalPitClientCache.apply(packet));
        context.setPacketHandled(true);
    }

    public record StructureEntry(long masterPos, List<Long> members, long endGameTime, int totalTicks,
                                 double currentSealedRatio, double qualifyingRatio, boolean projectedToSucceed) {
        public Set<Long> memberSet() {
            return new HashSet<>(members);
        }
    }
}
