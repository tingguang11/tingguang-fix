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
    private ItemStack outputItem = ItemStack.EMPTY;
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

        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!blockEntity.canAcceptOutput(result)) {
            return;
        }

        if (blockEntity.storedFuelTicks <= 0) {
            blockEntity.stopProcessing(false);
            return;
        }

        blockEntity.storedFuelTicks--;
        blockEntity.progressTicks++;
        blockEntity.sync();

        if (blockEntity.progressTicks < recipe.getCookTime()) {
            return;
        }

        recipe.consumeInputs(blockEntity.inputItems);
        blockEntity.insertOutput(result.copy());

        blockEntity.progressTicks = 0;

        ClayKilnRecipe nextRecipe = ClayKilnRecipe.findMatch(level, blockEntity.inputItems);
        if (nextRecipe == null || blockEntity.storedFuelTicks <= 0) {
            blockEntity.active = false;
        }

        blockEntity.sync();
    }

    public boolean addInput(ItemStack stack) {
        for (int i = 0; i < inputItems.size(); i++) {
            ItemStack existing = inputItems.get(i);
            if (existing.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                inputItems.set(i, copy);
                sync();
                return true;
            }

            if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                sync();
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
            sync();
            return extracted;
        }

        return ItemStack.EMPTY;
    }

    public NonNullList<ItemStack> getInputItems() {
        return inputItems;
    }

    public void addFuel(int addedTicks) {
        storedFuelTicks += addedTicks;
        sync();
    }

    public int getStoredFuelTicks() {
        return storedFuelTicks;
    }

    public int getProgressTicks() {
        return progressTicks;
    }

    public boolean isActive() {
        return active;
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
        sync();
        return true;
    }

    public ItemStack getOutputItem() {
        return outputItem;
    }

    public ItemStack removeOutput() {
        if (outputItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = outputItem.copy();
        outputItem = ItemStack.EMPTY;
        sync();
        return extracted;
    }

    public boolean hasValidStructure(Level level) {
        return ClayKilnStructure.findByIgnition(level, worldPosition) != null;
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

        if (!outputItem.isEmpty()) {
            level.addFreshEntity(new ItemEntity(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    outputItem.copy()
            ));
        }

        clearInputs();
        outputItem = ItemStack.EMPTY;
        storedFuelTicks = 0;
        progressTicks = 0;
        active = false;
        sync();
    }

    private void stopProcessing(boolean resetProgress) {
        active = false;
        if (resetProgress) {
            progressTicks = 0;
        }
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, inputItems);
        if (!outputItem.isEmpty()) {
            tag.put("OutputItem", outputItem.save(new CompoundTag()));
        }
        tag.putInt("StoredFuelTicks", storedFuelTicks);
        tag.putInt("ProgressTicks", progressTicks);
        tag.putBoolean("Active", active);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        clearInputs();
        ContainerHelper.loadAllItems(tag, inputItems);
        outputItem = tag.contains("OutputItem") ? ItemStack.of(tag.getCompound("OutputItem")) : ItemStack.EMPTY;
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

    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }

        if (outputItem.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameTags(outputItem, result)) {
            return false;
        }

        return outputItem.getCount() + result.getCount() <= outputItem.getMaxStackSize();
    }

    private void insertOutput(ItemStack result) {
        if (result.isEmpty()) {
            return;
        }

        if (outputItem.isEmpty()) {
            outputItem = result;
            return;
        }

        outputItem.grow(result.getCount());
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void clearInputs() {
        for (int i = 0; i < inputItems.size(); i++) {
            inputItems.set(i, ItemStack.EMPTY);
        }
    }
}
