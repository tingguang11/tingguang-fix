package com.fix.myfix.client;

import com.fix.myfix.network.SyncCharcoalPitsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public final class CharcoalPitClientCache {
    private static final Map<ResourceLocation, Map<Long, ClientPitInfo>> PIT_INFOS = new HashMap<>();

    private CharcoalPitClientCache() {
    }

    public static void apply(SyncCharcoalPitsPacket packet) {
        Map<Long, ClientPitInfo> dimensionEntries = new HashMap<>();
        for (SyncCharcoalPitsPacket.StructureEntry structure : packet.structures()) {
            ClientPitInfo info = new ClientPitInfo(
                    BlockPos.of(structure.masterPos()),
                    structure.endGameTime(),
                    structure.totalTicks(),
                    structure.currentSealedRatio(),
                    structure.qualifyingRatio(),
                    structure.projectedToSucceed()
            );

            for (Long memberPos : structure.members()) {
                dimensionEntries.put(memberPos, info);
            }
        }

        PIT_INFOS.put(packet.dimensionId(), dimensionEntries);
    }

    public static ClientPitInfo getInfo(Level level, BlockPos pos) {
        ResourceLocation dimensionId = level.dimension().location();
        Map<Long, ClientPitInfo> dimensionEntries = PIT_INFOS.get(dimensionId);
        if (dimensionEntries == null) {
            return null;
        }

        ClientPitInfo info = dimensionEntries.get(pos.asLong());
        if (info == null) {
            return null;
        }

        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        return info.withRemainingTicks((int) Math.max(0L, info.endGameTime() - gameTime));
    }

    public record ClientPitInfo(BlockPos masterPos, long endGameTime, int totalTicks, double currentSealedRatio,
                                double qualifyingRatio, boolean projectedToSucceed, int remainingTicks) {
        public ClientPitInfo(BlockPos masterPos, long endGameTime, int totalTicks, double currentSealedRatio,
                             double qualifyingRatio, boolean projectedToSucceed) {
            this(masterPos, endGameTime, totalTicks, currentSealedRatio, qualifyingRatio, projectedToSucceed, totalTicks);
        }

        public ClientPitInfo withRemainingTicks(int remainingTicks) {
            return new ClientPitInfo(masterPos, endGameTime, totalTicks, currentSealedRatio, qualifyingRatio, projectedToSucceed, remainingTicks);
        }
    }
}
