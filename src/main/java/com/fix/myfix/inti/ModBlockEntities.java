package com.fix.myfix.inti;

import com.fix.myfix.inti.blocks.entity.SimpleWorkbenchBlockEntity;
import com.fix.myfix.myfix;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, myfix.MODID);

    public static final RegistryObject<BlockEntityType<SimpleWorkbenchBlockEntity>> SIMPLE_WORKBENCH =
            BLOCK_ENTITIES.register("simple_workbench",
                    () -> BlockEntityType.Builder.of(
                            SimpleWorkbenchBlockEntity::new,
                            ModBlocks.SIMPLE_WORKBENCH.get()
                    ).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}