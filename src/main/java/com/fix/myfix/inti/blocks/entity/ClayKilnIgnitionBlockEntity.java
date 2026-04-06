package com.fix.myfix.inti.blocks.entity;

import com.fix.myfix.inti.ModBlockEntities;
import com.fix.myfix.recipe.ClayKilnRecipe;
import com.fix.myfix.system.ClayKilnStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ClayKilnIgnitionBlockEntity extends BlockEntity {
    private static final int INPUT_SLOT_COUNT = 4;

    private final NonNullList<ItemStack> inputItems = NonNullList.withSize(INPUT_SLOT_COUNT, ItemStack.EMPTY);
    private int storedFuelTicks;
    private int progressTicks;
    private boolean active;

    public ClayKilnIgnitionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLAY_KILN_IGNITION_PORT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ClayKilnIgnitionBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (level.getGameTime() % 20L == 0L && ClayKilnStructure.findByIgnition(level, pos) == null) {
            blockEntity.stopProcessing(true);
            return;
        }

        if (!blockEntity.active) {
            return;
        }

        ClayKilnRecipe recipe = ClayKilnRecipe.findMatch(level, blockEntity.inputItems);
        if (recipe == null) {
            blockEntity.stopProcessing(true);
            return;
        }

        if (blockEntity.storedFuelTicks <= 0) {
            blockEntity.stopProcessing(false);
            return;
        }

        blockEntity.storedFuelTicks--;
        blockEntity.progressTicks++;
        blockEntity.setChanged();

        if (blockEntity.progressTicks < recipe.getCookTime()) {
            return;
        }

        recipe.consumeInputs(blockEntity.inputItems);
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        if (!result.isEmpty()) {
            level.addFreshEntity(new ItemEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 0.5D,
                    result
            ));
        }

        blockEntity.progressTicks = 0;

        ClayKilnRecipe nextRecipe = ClayKilnRecipe.findMatch(level, blockEntity.inputItems);
        if (nextRecipe == null || blockEntity.storedFuelTicks <= 0) {
            blockEntity.active = false;
        }

        blockEntity.setChanged();
    }

    public boolean addInput(ItemStack stack) {
        for (int i = 0; i < inputItems.size(); i++) {
            ItemStack existing = inputItems.get(i);
            if (existing.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                inputItems.set(i, copy);
                setChanged();
                return true;
            }

            if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                setChanged();
                return true;
            }
        }

        return false;
    }

    public ItemStack removeOneInput() {
        for (int i = inputItems.size() - 1; i >= 0; i--) {
            ItemStack existing = inputItems.get(i);
            if (existing.isEmpty()) {
                continue;
            }

            ItemStack extracted = existing.split(1);
            if (existing.isEmpty()) {
                inputItems.set(i, ItemStack.EMPTY);
            }
            setChanged();
            return extracted;
        }

        return ItemStack.EMPTY;
    }

    public NonNullList<ItemStack> getInputItems() {
        return inputItems;
    }

    public void addFuel(int addedTicks) {
        storedFuelTicks += addedTicks;
        setChanged();
    }

    public int getStoredFuelTicks() {
        return storedFuelTicks;
    }

    public boolean startProcessing(Level level) {
        if (active) {
            return true;
        }

        if (storedFuelTicks <= 0) {
            return false;
        }

        ClayKilnRecipe recipe = ClayKilnRecipe.findMatch(level, inputItems);
        if (recipe == null) {
            return false;
        }

        active = true;
        setChanged();
        return true;
    }

    public void dropContents(Level level) {
        for (ItemStack stack : inputItems) {
            if (stack.isEmpty()) {
                continue;
            }

            level.addFreshEntity(new ItemEntity(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    stack.copy()
            ));
        }

        clearInputs();
        storedFuelTicks = 0;
        progressTicks = 0;
        active = false;
        setChanged();
    }

    private void stopProcessing(boolean resetProgress) {
        active = false;
        if (resetProgress) {
            progressTicks = 0;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, inputItems);
        tag.putInt("StoredFuelTicks", storedFuelTicks);
        tag.putInt("ProgressTicks", progressTicks);
        tag.putBoolean("Active", active);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        clearInputs();
        ContainerHelper.loadAllItems(tag, inputItems);
        storedFuelTicks = tag.getInt("StoredFuelTicks");
        progressTicks = tag.getInt("ProgressTicks");
        active = tag.getBoolean("Active");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void clearInputs() {
        for (int i = 0; i < inputItems.size(); i++) {
            inputItems.set(i, ItemStack.EMPTY);
        }
    }
}
