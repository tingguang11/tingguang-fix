package com.fix.myfix.inti.blocks;

import com.fix.myfix.config.HarderBeginningsConfig;
import com.fix.myfix.inti.blocks.entity.BurningTorchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class BurningTorchLogic {
    private BurningTorchLogic() {
    }

    public static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hitResult) {
        if (!HarderBeginningsConfig.torchEnabled()) {
            return InteractionResult.PASS;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BurningTorchBlockEntity torchBlockEntity)) {
            return InteractionResult.PASS;
        }

        boolean lit = state.getValue(BlockStateProperties.LIT);
        ItemStack heldItem = player.getItemInHand(hand);
        if (lit) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!torchBlockEntity.hasRemainingFuel() || !isIgnitionItem(heldItem)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                    level.random.nextFloat() * 0.4F + 0.8F);
            consumeIgnitionItem(player, hand, heldItem);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static List<ItemStack> getDrops(LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof BurningTorchBlockEntity torchBlockEntity && !torchBlockEntity.hasRemainingFuel()) {
            return List.of(new ItemStack(Items.STICK));
        }
        return List.of(new ItemStack(Items.TORCH));
    }

    public static ItemStack getCloneStack(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BurningTorchBlockEntity torchBlockEntity && !torchBlockEntity.hasRemainingFuel()) {
            return new ItemStack(Items.STICK);
        }
        return new ItemStack(Items.TORCH);
    }

    private static boolean isIgnitionItem(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    private static void consumeIgnitionItem(Player player, InteractionHand hand, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return;
        }

        if (stack.is(Items.FLINT_AND_STEEL)) {
            stack.hurtAndBreak(1, player, livingEntity -> livingEntity.broadcastBreakEvent(hand));
            return;
        }

        if (stack.is(Items.FIRE_CHARGE)) {
            stack.shrink(1);
        }
    }
}
