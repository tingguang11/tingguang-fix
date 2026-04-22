package com.fix.myfix.inti;

import com.fix.myfix.MyFix;
import com.fix.myfix.inti.blocks.BurningTorchBlock;
import com.fix.myfix.inti.blocks.BurningWallTorchBlock;
import com.fix.myfix.inti.blocks.WoodWorkbenchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MyFix.MODID);
    public static final RegistryObject<Block> WOOD_WORKBENCH =
            BLOCKS.register("wood_workbench",
                    WoodWorkbenchBlock::new);
    public static final RegistryObject<Block> BURNING_TORCH =
            BLOCKS.register("burning_torch", BurningTorchBlock::new);
    public static final RegistryObject<Block> BURNING_WALL_TORCH =
            BLOCKS.register("burning_wall_torch", BurningWallTorchBlock::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
