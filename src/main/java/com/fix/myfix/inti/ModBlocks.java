package com.fix.myfix.inti;

import com.fix.myfix.MyFix;
import com.fix.myfix.inti.blocks.ClayKilnPortBlock;
import com.fix.myfix.inti.blocks.ClayKilnPortType;
import com.fix.myfix.inti.blocks.WoodWorkbenchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
    public static final RegistryObject<Block> CLAY_BRICK_BLOCK =
            BLOCKS.register("clay_brick_block",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.BRICKS)));
    public static final RegistryObject<Block> CLAY_KILN_INPUT_PORT =
            BLOCKS.register("clay_kiln_input_port",
                    () -> new ClayKilnPortBlock(ClayKilnPortType.INPUT, BlockBehaviour.Properties.copy(Blocks.BRICKS)));
    public static final RegistryObject<Block> CLAY_KILN_FUEL_PORT =
            BLOCKS.register("clay_kiln_fuel_port",
                    () -> new ClayKilnPortBlock(ClayKilnPortType.FUEL, BlockBehaviour.Properties.copy(Blocks.BRICKS)));
    public static final RegistryObject<Block> CLAY_KILN_IGNITION_PORT =
            BLOCKS.register("clay_kiln_ignition_port",
                    () -> new ClayKilnPortBlock(ClayKilnPortType.IGNITION, BlockBehaviour.Properties.copy(Blocks.BRICKS)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
