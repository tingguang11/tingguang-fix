package com.fix.myfix.inti.blocks.entity;

import com.fix.myfix.inti.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WoodWorkbenchBlockEntity extends BlockEntity {

    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public WoodWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_WORKBENCH.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public void sync() {
        Level currentLevel = getLevel();
        setChanged();
        if (currentLevel != null && !currentLevel.isClientSide) {
            currentLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean addItem(int slot, ItemStack stack) {
        if (items.get(slot).isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            items.set(slot, copy);
            sync();
            return true;
        }
        return false;
    }
    public void clear() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        sync();
    }

    public ItemStack removeItem(int slot) {
        ItemStack old = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        sync();
        return old;
    }

    public void setAll(NonNullList<ItemStack> newItems) {
        for (int i = 0; i < 9; i++) {
            items.set(i, newItems.get(i));
        }
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
