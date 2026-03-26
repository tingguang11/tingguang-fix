package com.fix.myfix.inti.blocks.entity;

import com.fix.myfix.inti.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SimpleWorkbenchBlockEntity extends BlockEntity {

    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public SimpleWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMPLE_WORKBENCH.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public boolean addItem(int slot, ItemStack stack) {
        if (items.get(slot).isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            items.set(slot, copy);
            setChanged();
            return true;
        }
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        return false;
    }

    public ItemStack removeItem(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    public void clear() {
        for (int i = 0; i < 9; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        ContainerHelper.saveAllItems(tag, this.items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        ContainerHelper.loadAllItems(tag, this.items);
    }
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
    }
}