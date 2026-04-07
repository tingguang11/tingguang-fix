package com.fix.myfix.system;

import com.fix.myfix.inti.ModBlocks;
import com.fix.myfix.inti.blocks.ClayKilnPortType;
import com.fix.myfix.inti.blocks.entity.ClayKilnIgnitionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record ClayKilnStructure(BlockPos centerPos, BlockPos inputPortPos, BlockPos fuelPortPos,
                                BlockPos ignitionPortPos, BlockPos outputPortPos) {
    public static ClayKilnStructure findByPort(Level level, BlockPos portPos) {
        BlockPos min = portPos.offset(-2, -2, -2);
        BlockPos max = portPos.offset(2, 2, 2);

        for (BlockPos candidate : BlockPos.betweenClosed(min, max)) {
            BlockState candidateState = level.getBlockState(candidate);
            if (!candidateState.is(ModBlocks.CLAY_KILN_IGNITION_PORT.get())) {
                continue;
            }

            ClayKilnStructure structure = findByIgnition(level, candidate.immutable());
            if (structure != null && structure.containsPort(portPos)) {
                return structure;
            }
        }

        return null;
    }

    public static ClayKilnStructure findByIgnition(Level level, BlockPos ignitionPos) {
        BlockState ignitionState = level.getBlockState(ignitionPos);
        if (!ignitionState.is(ModBlocks.CLAY_KILN_IGNITION_PORT.get())) {
            return null;
        }

        Direction outward = ignitionState.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos center = ignitionPos.relative(outward.getOpposite());

        if (!level.getBlockState(center).isAir()) {
            return null;
        }

        BlockPos inputPos = null;
        BlockPos fuelPos = null;
        BlockPos outputPos = null;

        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos currentPos = center.offset(x, y, z);
                    if (currentPos.equals(center)) {
                        continue;
                    }

                    BlockState currentState = level.getBlockState(currentPos);
                    if (y == 0 && Math.abs(x) + Math.abs(z) == 1) {
                        Direction side = directionFromOffset(x, z);
                        if (side == outward) {
                            if (!matchesPort(currentState, ModBlocks.CLAY_KILN_IGNITION_PORT.get(), side)) {
                                return null;
                            }
                        } else if (currentState.is(ModBlocks.CLAY_KILN_INPUT_PORT.get())) {
                            if (!matchesPort(currentState, ModBlocks.CLAY_KILN_INPUT_PORT.get(), side) || inputPos != null) {
                                return null;
                            }
                            inputPos = currentPos.immutable();
                        } else if (currentState.is(ModBlocks.CLAY_KILN_FUEL_PORT.get())) {
                            if (!matchesPort(currentState, ModBlocks.CLAY_KILN_FUEL_PORT.get(), side) || fuelPos != null) {
                                return null;
                            }
                            fuelPos = currentPos.immutable();
                        } else if (currentState.is(ModBlocks.CLAY_KILN_OUTPUT_PORT.get())) {
                            if (!matchesPort(currentState, ModBlocks.CLAY_KILN_OUTPUT_PORT.get(), side) || outputPos != null) {
                                return null;
                            }
                            outputPos = currentPos.immutable();
                        } else {
                            return null;
                        }
                    } else if (!currentState.is(ModBlocks.CLAY_BRICK_BLOCK.get())) {
                        return null;
                    }
                }
            }
        }

        if (inputPos == null || fuelPos == null || outputPos == null) {
            return null;
        }

        return new ClayKilnStructure(center, inputPos, fuelPos, ignitionPos.immutable(), outputPos);
    }

    public ClayKilnIgnitionBlockEntity getMaster(Level level) {
        BlockEntity blockEntity = level.getBlockEntity(ignitionPortPos);
        if (blockEntity instanceof ClayKilnIgnitionBlockEntity ignitionBlockEntity) {
            return ignitionBlockEntity;
        }
        return null;
    }

    public boolean containsPort(BlockPos pos) {
        return inputPortPos.equals(pos) || fuelPortPos.equals(pos) || ignitionPortPos.equals(pos) || outputPortPos.equals(pos);
    }

    public BlockPos getPortPos(ClayKilnPortType portType) {
        return switch (portType) {
            case INPUT -> inputPortPos;
            case FUEL -> fuelPortPos;
            case IGNITION -> ignitionPortPos;
            case OUTPUT -> outputPortPos;
        };
    }

    private static boolean matchesPort(BlockState state, net.minecraft.world.level.block.Block expectedBlock, Direction outward) {
        return state.is(expectedBlock) && state.getValue(HorizontalDirectionalBlock.FACING) == outward;
    }

    private static Direction directionFromOffset(int x, int z) {
        if (x == 1) {
            return Direction.EAST;
        }
        if (x == -1) {
            return Direction.WEST;
        }
        if (z == 1) {
            return Direction.SOUTH;
        }
        return Direction.NORTH;
    }
}
