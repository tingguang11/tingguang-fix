package com.fix.myfix.inti;

import com.fix.myfix.MyFix;
import com.fix.myfix.inti.blocks.entity.WoodWorkbenchBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MyFix.MODID);

    public static final RegistryObject<BlockEntityType<WoodWorkbenchBlockEntity>> WOOD_WORKBENCH =
            BLOCK_ENTITIES.register("wood_workbench",
                    () -> BlockEntityType.Builder.of(
                            WoodWorkbenchBlockEntity::new,
                            ModBlocks.WOOD_WORKBENCH.get()
                    ).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
