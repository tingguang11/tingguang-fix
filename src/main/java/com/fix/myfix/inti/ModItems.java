package com.fix.myfix.inti;

import com.fix.myfix.MyFix;
import com.fix.myfix.item.FlintKnifeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MyFix.MODID);

    public static final RegistryObject<Item> WOOD_WORKBENCH_ITEM =
            ITEMS.register("wood_workbench",
                    () -> new BlockItem(ModBlocks.WOOD_WORKBENCH.get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> CLAY_BRICK_BLOCK_ITEM =
            ITEMS.register("clay_brick_block",
                    () -> new BlockItem(ModBlocks.CLAY_BRICK_BLOCK.get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> FIBER = ITEMS.register("fiber",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HEMP_STRING = ITEMS.register("hemp_string",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CLAY_BRICK = ITEMS.register("clay_brick",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FIRED_CLAY_BRICK = ITEMS.register("fired_clay_brick",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FLINT_PICKAXE = ITEMS.register("flint_pickaxe",
            () -> new PickaxeItem(ModTiers.FLINT, 1, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> FLINT_AXE = ITEMS.register("flint_axe",
            () -> new AxeItem(ModTiers.FLINT, 7.0F, -3.2F, new Item.Properties()));

    public static final RegistryObject<Item> FLINT_SHOVEL = ITEMS.register("flint_shovel",
            () -> new ShovelItem(ModTiers.FLINT, 1.5F, -3.0F, new Item.Properties()));

    public static final RegistryObject<Item> FLINT_KNIFE = ITEMS.register("flint_knife",
            () -> new FlintKnifeItem(ModTiers.FLINT, 3, -2.4F, new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
