package com.fix.myfix.inti;

import com.fix.myfix.myfix;
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
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, myfix.MODID);

    public static final RegistryObject<CreativeModeTab> TUTORIAL =
            CREATIVE_MODE_TABS.register("myfix",
                    () -> CreativeModeTab.builder()

                            .icon(() -> new ItemStack(Items.CRAFTING_TABLE))

                            .title(Component.translatable("tab.myfix"))

                            .displayItems((itemDisplayParameters, output) -> {

                                output.accept(new ItemStack(ModBlocks.SIMPLE_WORKBENCH.get()));
                                output.accept(new ItemStack(ModItems.HAMMER.get()));
                            })

                            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
