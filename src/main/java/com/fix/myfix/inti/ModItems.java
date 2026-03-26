package com.fix.myfix.inti;

import com.fix.myfix.myfix;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, myfix.MODID);

    public static final RegistryObject<Item> SIMPLE_WORKBENCH_ITEM =
            ITEMS.register("simple_stone_workbench",
                    () -> new BlockItem(ModBlocks.SIMPLE_WORKBENCH.get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> HAMMER =
            ITEMS.register("hammer",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}