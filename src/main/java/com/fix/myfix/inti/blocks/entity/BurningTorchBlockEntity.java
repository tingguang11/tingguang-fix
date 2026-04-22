package com.fix.myfix.inti.blocks.entity;

import com.fix.myfix.config.HarderBeginningsConfig;
import com.fix.myfix.inti.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BurningTorchBlockEntity extends BlockEntity {
    private static final String REMAINING_TICKS_TAG = "RemainingTicks";

    private int remainingTicks = HarderBeginningsConfig.torchBurnTicks();

    public BurningTorchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BURNING_TORCH.get(), pos, state);
    }

    public boolean hasRemainingFuel() {
        return remainingTicks > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BurningTorchBlockEntity blockEntity) {
        if (!HarderBeginningsConfig.torchEnabled()) {
            if (!state.getValue(BlockStateProperties.LIT)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
            }
            return;
        }

        if (!state.getValue(BlockStateProperties.LIT)) {
            return;
        }

        if (level.isRainingAt(pos)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.0F);
            return;
        }

        if (blockEntity.remainingTicks > 0) {
            blockEntity.remainingTicks--;
            blockEntity.setChanged();
        }

        if (blockEntity.remainingTicks <= 0) {
            blockEntity.remainingTicks = 0;
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.8F);
            blockEntity.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(REMAINING_TICKS_TAG, remainingTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        remainingTicks = Math.max(0, tag.getInt(REMAINING_TICKS_TAG));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
