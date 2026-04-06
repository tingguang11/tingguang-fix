package com.fix.myfix.system;

import com.fix.myfix.MyFix;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CampfireFuelSavedData extends SavedData {
    private static final String DATA_NAME = MyFix.MODID + "_campfire_fuel";
    private static final String ENTRIES_TAG = "Entries";
    private static final String POS_TAG = "Pos";
    private static final String FUEL_TICKS_TAG = "FuelTicks";

    private final Map<Long, Integer> fuelTicks = new HashMap<>();

    public static CampfireFuelSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                CampfireFuelSavedData::load,
                CampfireFuelSavedData::new,
                DATA_NAME
        );
    }

    public static CampfireFuelSavedData load(CompoundTag tag) {
        CampfireFuelSavedData data = new CampfireFuelSavedData();
        ListTag entries = tag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.getCompound(i);
            int storedFuelTicks = entryTag.getInt(FUEL_TICKS_TAG);
            if (storedFuelTicks > 0) {
                data.fuelTicks.put(entryTag.getLong(POS_TAG), storedFuelTicks);
            }
        }
        return data;
    }

    public int getRemainingFuel(BlockPos pos) {
        return fuelTicks.getOrDefault(pos.asLong(), 0);
    }

    public boolean hasFuel(BlockPos pos) {
        return getRemainingFuel(pos) > 0;
    }

    public void addFuel(BlockPos pos, int addedFuelTicks) {
        if (addedFuelTicks <= 0) {
            return;
        }

        fuelTicks.merge(pos.asLong(), addedFuelTicks, Integer::sum);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (fuelTicks.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    public boolean tryIgnite(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CampfireBlock) || !hasFuel(pos)) {
            return false;
        }

        if (!state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
        }
        return true;
    }

    public void tick(ServerLevel level) {
        if (fuelTicks.isEmpty()) {
            return;
        }

        boolean dirty = false;
        Iterator<Map.Entry<Long, Integer>> iterator = fuelTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Integer> entry = iterator.next();
            BlockPos pos = BlockPos.of(entry.getKey());
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof CampfireBlock)) {
                iterator.remove();
                dirty = true;
                continue;
            }

            int remainingTicks = entry.getValue();
            if (!state.getValue(BlockStateProperties.LIT)) {
                if (remainingTicks <= 0) {
                    iterator.remove();
                    dirty = true;
                }
                continue;
            }

            remainingTicks--;
            if (remainingTicks <= 0) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
                iterator.remove();
            } else {
                entry.setValue(remainingTicks);
            }
            dirty = true;
        }

        if (dirty) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (Map.Entry<Long, Integer> entry : fuelTicks.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong(POS_TAG, entry.getKey());
            entryTag.putInt(FUEL_TICKS_TAG, entry.getValue());
            entries.add(entryTag);
        }
        tag.put(ENTRIES_TAG, entries);
        return tag;
    }
}
