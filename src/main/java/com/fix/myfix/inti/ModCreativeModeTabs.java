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

    // 注册本模组的创造模式标签。
    // "tutorial" 为该标签的注册名，会作为内部 ID 使用。
    public static final RegistryObject<CreativeModeTab> TUTORIAL =
                CREATIVE_MODE_TABS.register("myfix",
                    () -> CreativeModeTab.builder()

                            .icon(() -> new ItemStack(Items.STONE))

                            .title(Component.translatable("tab.myfix"))

                            .displayItems((itemDisplayParameters, output) -> {

                            })

                            .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
