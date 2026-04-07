package com.fix.myfix.inti;

import com.fix.myfix.MyFix;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MyFix.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register(MyFix.MODID,
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(Items.CRAFTING_TABLE))
                            .title(Component.translatable("tab.harder_beginnings"))
                            .displayItems((parameters, output) -> {
                                output.accept(new ItemStack(ModBlocks.WOOD_WORKBENCH.get()));
                                output.accept(new ItemStack(ModItems.CLAY_BRICK.get()));
                                output.accept(new ItemStack(ModItems.FIRED_CLAY_BRICK.get()));
                                output.accept(new ItemStack(ModBlocks.CLAY_BRICK_BLOCK.get()));
                                output.accept(new ItemStack(ModBlocks.CLAY_KILN_INPUT_PORT.get()));
                                output.accept(new ItemStack(ModBlocks.CLAY_KILN_FUEL_PORT.get()));
                                output.accept(new ItemStack(ModBlocks.CLAY_KILN_IGNITION_PORT.get()));
                                output.accept(new ItemStack(ModBlocks.CLAY_KILN_OUTPUT_PORT.get()));
                                output.accept(new ItemStack(ModItems.COPPER_DUST.get()));
                                output.accept(new ItemStack(ModItems.FIBER.get()));
                                output.accept(new ItemStack(ModItems.HEMP_STRING.get()));
                                output.accept(new ItemStack(ModItems.FLINT_PICKAXE.get()));
                                output.accept(new ItemStack(ModItems.FLINT_AXE.get()));
                                output.accept(new ItemStack(ModItems.FLINT_SHOVEL.get()));
                                output.accept(new ItemStack(ModItems.FLINT_KNIFE.get()));
                            })
                            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
