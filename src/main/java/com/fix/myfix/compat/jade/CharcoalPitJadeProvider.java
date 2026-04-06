package com.fix.myfix.compat.jade;

import com.fix.myfix.client.CharcoalPitClientCache;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CharcoalPitJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("harder_beginnings", "charcoal_pit");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getBlockState().is(BlockTags.LOGS)) {
            return;
        }

        CharcoalPitClientCache.ClientPitInfo info = CharcoalPitClientCache.getInfo(accessor.getLevel(), accessor.getPosition());
        if (info == null) {
            return;
        }

        tooltip.add(Component.translatable("jade.harder_beginnings.charcoal_pit"));
        tooltip.add(Component.translatable("jade.harder_beginnings.charcoal_pit.time", formatTicks(info.remainingTicks()), formatTicks(info.totalTicks())));
        tooltip.add(Component.translatable("jade.harder_beginnings.charcoal_pit.sealed", formatPercent(info.currentSealedRatio())));
        tooltip.add(Component.translatable("jade.harder_beginnings.charcoal_pit.qualified", formatPercent(info.qualifyingRatio())));
        tooltip.add(Component.translatable(
                info.projectedToSucceed()
                        ? "jade.harder_beginnings.charcoal_pit.result.charcoal"
                        : "jade.harder_beginnings.charcoal_pit.result.burned"
        ));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static String formatTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static String formatPercent(double value) {
        return Mth.floor(value * 100.0D + 0.5D) + "%";
    }
}
