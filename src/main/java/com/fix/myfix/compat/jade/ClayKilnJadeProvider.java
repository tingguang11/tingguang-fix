package com.fix.myfix.compat.jade;

import com.fix.myfix.inti.ModBlocks;
import com.fix.myfix.inti.blocks.ClayKilnPortBlock;
import com.fix.myfix.inti.blocks.ClayKilnPortType;
import com.fix.myfix.inti.blocks.entity.ClayKilnIgnitionBlockEntity;
import com.fix.myfix.recipe.ClayKilnRecipe;
import com.fix.myfix.system.ClayKilnStructure;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ClayKilnJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("harder_beginnings", "clay_kiln");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        if (!(state.getBlock() instanceof ClayKilnPortBlock)) {
            return;
        }

        tooltip.add(Component.translatable("jade.harder_beginnings.clay_kiln"));

        ClayKilnStructure structure = ClayKilnStructure.findByPort(accessor.getLevel(), accessor.getPosition());
        if (structure == null) {
            tooltip.add(Component.translatable("jade.harder_beginnings.clay_kiln.structure_incomplete"));
            return;
        }

        tooltip.add(Component.translatable("jade.harder_beginnings.clay_kiln.structure_complete"));
        ClayKilnIgnitionBlockEntity master = structure.getMaster(accessor.getLevel());
        if (master == null) {
            return;
        }

        ClayKilnPortType portType = getPortType(state);
        if (portType == ClayKilnPortType.INPUT) {
            appendInputInfo(tooltip, master);
            if (master.isActive()) {
                ClayKilnRecipe recipe = ClayKilnRecipe.findMatch(accessor.getLevel(), master.getInputItems());
                if (recipe != null) {
                    tooltip.add(Component.translatable(
                            "jade.harder_beginnings.clay_kiln.progress",
                            formatPercent((double) master.getProgressTicks() / (double) recipe.getCookTime())
                    ));
                }
            }
            return;
        }

        if (portType == ClayKilnPortType.OUTPUT) {
            ItemStack output = master.getOutputItem();
            if (output.isEmpty()) {
                tooltip.add(Component.translatable("jade.harder_beginnings.clay_kiln.empty_output"));
            } else {
                tooltip.add(Component.translatable(
                        "jade.harder_beginnings.clay_kiln.output",
                        output.getHoverName(),
                        output.getCount()
                ));
            }
            return;
        }

        if (portType == ClayKilnPortType.FUEL) {
            tooltip.add(Component.translatable(
                    "jade.harder_beginnings.clay_kiln.fuel",
                    String.format("%.1f", master.getStoredFuelTicks() / 20.0D)
            ));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static ClayKilnPortType getPortType(BlockState state) {
        if (state.is(ModBlocks.CLAY_KILN_INPUT_PORT.get())) {
            return ClayKilnPortType.INPUT;
        }
        if (state.is(ModBlocks.CLAY_KILN_FUEL_PORT.get())) {
            return ClayKilnPortType.FUEL;
        }
        if (state.is(ModBlocks.CLAY_KILN_OUTPUT_PORT.get())) {
            return ClayKilnPortType.OUTPUT;
        }
        return ClayKilnPortType.IGNITION;
    }

    private static void appendInputInfo(ITooltip tooltip, ClayKilnIgnitionBlockEntity master) {
        boolean hasInput = false;
        for (ItemStack inputItem : master.getInputItems()) {
            if (inputItem.isEmpty()) {
                continue;
            }

            hasInput = true;
            tooltip.add(Component.translatable(
                    "jade.harder_beginnings.clay_kiln.input",
                    inputItem.getHoverName(),
                    inputItem.getCount()
            ));
        }

        if (!hasInput) {
            tooltip.add(Component.translatable("message.harder_beginnings.clay_kiln.no_input"));
        }
    }

    private static String formatPercent(double value) {
        return Mth.floor(Mth.clamp(value, 0.0D, 1.0D) * 100.0D + 0.5D) + "%";
    }
}
