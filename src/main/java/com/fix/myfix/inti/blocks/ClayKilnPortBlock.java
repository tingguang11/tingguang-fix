package com.fix.myfix.inti.blocks;

import com.fix.myfix.inti.ModBlockEntities;
import com.fix.myfix.inti.blocks.entity.ClayKilnIgnitionBlockEntity;
import com.fix.myfix.system.ClayKilnStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ClayKilnPortBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final int FUEL_TICKS_PER_CHARCOAL = 1200;

    private final ClayKilnPortType portType;

    public ClayKilnPortBlock(ClayKilnPortType portType, Properties properties) {
        super(properties);
        this.portType = portType;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        return switch (portType) {
            case INPUT -> handleInput((ServerLevel) level, pos, player, hand);
            case FUEL -> handleFuel((ServerLevel) level, pos, player, hand);
            case IGNITION -> handleIgnition((ServerLevel) level, pos, player, hand);
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && portType == ClayKilnPortType.IGNITION) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ClayKilnIgnitionBlockEntity ignitionBlockEntity && !level.isClientSide) {
                ignitionBlockEntity.dropContents(level);
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (portType == ClayKilnPortType.IGNITION) {
            return new ClayKilnIgnitionBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (portType != ClayKilnPortType.IGNITION || level.isClientSide) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntities.CLAY_KILN_IGNITION_PORT.get(),
                ClayKilnIgnitionBlockEntity::serverTick);
    }

    private InteractionResult handleInput(ServerLevel level, BlockPos pos, Player player, InteractionHand hand) {
        ClayKilnStructure structure = ClayKilnStructure.findByPort(level, pos);
        if (structure == null) {
            fail(player, "message.harder_beginnings.clay_kiln.invalid_structure");
            return InteractionResult.CONSUME;
        }

        ClayKilnIgnitionBlockEntity master = structure.getMaster(level);
        if (master == null) {
            fail(player, "message.harder_beginnings.clay_kiln.invalid_structure");
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            ItemStack extracted = master.removeOneInput();
            if (extracted.isEmpty()) {
                fail(player, "message.harder_beginnings.clay_kiln.no_input");
                return InteractionResult.CONSUME;
            }

            if (!player.addItem(extracted)) {
                player.drop(extracted, false);
            }
            return InteractionResult.CONSUME;
        }

        if (!master.addInput(held)) {
            fail(player, "message.harder_beginnings.clay_kiln.input_full");
            return InteractionResult.CONSUME;
        }

        held.shrink(1);
        return InteractionResult.CONSUME;
    }

    private InteractionResult handleFuel(ServerLevel level, BlockPos pos, Player player, InteractionHand hand) {
        ClayKilnStructure structure = ClayKilnStructure.findByPort(level, pos);
        if (structure == null) {
            fail(player, "message.harder_beginnings.clay_kiln.invalid_structure");
            return InteractionResult.CONSUME;
        }

        ClayKilnIgnitionBlockEntity master = structure.getMaster(level);
        if (master == null) {
            fail(player, "message.harder_beginnings.clay_kiln.invalid_structure");
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.harder_beginnings.clay_kiln.fuel_time",
                    String.format("%.1f", master.getStoredFuelTicks() / 20.0D)
            ).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        if (!held.is(Items.CHARCOAL)) {
            fail(player, "message.harder_beginnings.clay_kiln.needs_charcoal");
            return InteractionResult.CONSUME;
        }

        master.addFuel(FUEL_TICKS_PER_CHARCOAL);
        held.shrink(1);
        return InteractionResult.CONSUME;
    }

    private InteractionResult handleIgnition(ServerLevel level, BlockPos pos, Player player, InteractionHand hand) {
        ClayKilnStructure structure = ClayKilnStructure.findByIgnition(level, pos);
        if (structure == null) {
            fail(player, "message.harder_beginnings.clay_kiln.invalid_structure");
            return InteractionResult.CONSUME;
        }

        ClayKilnIgnitionBlockEntity master = structure.getMaster(level);
        if (master == null) {
            fail(player, "message.harder_beginnings.clay_kiln.invalid_structure");
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
            if (!master.startProcessing(level)) {
                reportIgnitionFailure(player, master);
                return InteractionResult.CONSUME;
            }

            consumeIgnitionItem(player, hand, held);
            playIgnitionSound(level, pos);
            return InteractionResult.CONSUME;
        }

        if (held.is(Items.FLINT) && player.getOffhandItem().is(Items.FLINT) && hand == InteractionHand.MAIN_HAND) {
            consumePrimitiveFlint(player);
            if (level.random.nextBoolean() && master.startProcessing(level)) {
                playIgnitionSound(level, pos);
            } else {
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.6F, 0.6F);
            }
            return InteractionResult.CONSUME;
        }

        reportIgnitionFailure(player, master);
        return InteractionResult.CONSUME;
    }

    private void reportIgnitionFailure(Player player, ClayKilnIgnitionBlockEntity master) {
        if (master.getStoredFuelTicks() <= 0) {
            fail(player, "message.harder_beginnings.clay_kiln.no_fuel");
            return;
        }

        fail(player, "message.harder_beginnings.clay_kiln.no_recipe");
    }

    private void playIgnitionSound(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                level.random.nextFloat() * 0.4F + 0.8F);
    }

    private void consumePrimitiveFlint(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }

        player.getMainHandItem().shrink(1);
        player.getOffhandItem().shrink(1);
    }

    private void consumeIgnitionItem(Player player, InteractionHand hand, ItemStack held) {
        if (player.getAbilities().instabuild) {
            return;
        }

        if (held.is(Items.FLINT_AND_STEEL)) {
            held.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
        } else if (held.is(Items.FIRE_CHARGE)) {
            held.shrink(1);
        }
    }

    private void fail(Player player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey).withStyle(ChatFormatting.RED), true);
    }
}
