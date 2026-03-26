package com.fix.myfix.inti;

import com.fix.myfix.inti.blocks.SimpleStoneWorkbenchBlock;
import com.fix.myfix.myfix;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, myfix.MODID);
    public static final RegistryObject<Block> SIMPLE_WORKBENCH =
            BLOCKS.register("simple_stone_workbench",
                    SimpleStoneWorkbenchBlock::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
