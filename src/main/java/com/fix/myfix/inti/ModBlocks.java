package com.fix.myfix.inti;

import com.fix.myfix.myfix;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, myfix.MODID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
