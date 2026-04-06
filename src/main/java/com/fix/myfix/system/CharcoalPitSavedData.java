package com.fix.myfix.system;

import com.fix.myfix.config.HarderBeginningsConfig;
import com.fix.myfix.MyFix;
import com.fix.myfix.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CharcoalPitSavedData extends SavedData {
    private static final String DATA_NAME = MyFix.MODID + "_charcoal_pits";
    private static final String STRUCTURES_TAG = "Structures";
    private static final String MASTER_TAG = "Master";
    private static final String MEMBERS_TAG = "Members";
    private static final String PROGRESS_TAG = "Progress";
    private static final String BURN_TICKS_TAG = "BurnTicks";
    private static final String CURRENT_SEALED_RATIO_TAG = "CurrentSealedRatio";
    private static final String CHECKED_SAMPLES_TAG = "CheckedSamples";
    private static final String FULLY_SEALED_SAMPLES_TAG = "FullySealedSamples";
    private static final Comparator<BlockPos> MASTER_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getZ);

    private final Map<Long, CharcoalPitStructure> activeStructures = new HashMap<>();
    private final Map<Long, Long> slaveToMaster = new HashMap<>();

    public static CharcoalPitSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                CharcoalPitSavedData::load,
                CharcoalPitSavedData::new,
                DATA_NAME
        );
    }

    public static CharcoalPitSavedData load(CompoundTag tag) {
        CharcoalPitSavedData data = new CharcoalPitSavedData();
        ListTag structures = tag.getList(STRUCTURES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < structures.size(); i++) {
            CompoundTag structureTag = structures.getCompound(i);
            CharcoalPitStructure structure = CharcoalPitStructure.load(structureTag);
            data.activeStructures.put(structure.masterPos(), structure);
            data.indexStructure(structure);
        }
        return data;
    }

    public boolean isTracked(BlockPos pos) {
        return slaveToMaster.containsKey(pos.asLong());
    }

    public CharcoalPitInfo getInfo(BlockPos pos) {
        Long masterPos = slaveToMaster.get(pos.asLong());
        if (masterPos == null) {
            return null;
        }

        CharcoalPitStructure structure = activeStructures.get(masterPos);
        if (structure == null) {
            slaveToMaster.remove(pos.asLong());
            return null;
        }

        return new CharcoalPitInfo(
                BlockPos.of(structure.masterPos()),
                Math.max(0, structure.burnTicks() - structure.progressTicks()),
                structure.burnTicks(),
                structure.currentSealedRatio(),
                structure.qualifyingRatio(),
                structure.isProjectedToSucceed()
        );
    }

    public boolean discardStructureContaining(ServerLevel level, BlockPos pos) {
        Long masterPos = slaveToMaster.get(pos.asLong());
        if (masterPos == null) {
            return false;
        }

        CharcoalPitStructure removed = activeStructures.remove(masterPos);
        if (removed == null) {
            slaveToMaster.remove(pos.asLong());
            return false;
        }

        unindexStructure(removed);
        setDirty();
        ModNetwork.syncCharcoalPits(level, this);
        return true;
    }

    public boolean tryIgnite(ServerLevel level, BlockPos origin) {
        if (!level.getBlockState(origin).is(BlockTags.LOGS) || isTracked(origin)) {
            return false;
        }

        Set<Long> members = floodFillLogs(level, origin);
        if (members.isEmpty()) {
            return false;
        }

        for (Long memberPos : members) {
            if (slaveToMaster.containsKey(memberPos)) {
                return false;
            }
        }

        BlockPos masterPos = selectMaster(members);
        double currentSealedRatio = calculateSealedRatio(level, members);
        CharcoalPitStructure structure = new CharcoalPitStructure(
                masterPos.asLong(),
                members,
                0,
                members.size() * HarderBeginningsConfig.charcoalBurnTicksPerLog(),
                currentSealedRatio,
                0,
                0
        );

        activeStructures.put(structure.masterPos(), structure);
        indexStructure(structure);
        setDirty();
        ModNetwork.syncCharcoalPits(level, this);
        return true;
    }

    public void tick(ServerLevel level) {
        if (activeStructures.isEmpty()) {
            return;
        }

        List<Long> finishedStructures = new ArrayList<>();
        boolean dirty = false;
        boolean shouldSync = false;

        for (CharcoalPitStructure structure : activeStructures.values()) {
            structure.advance();
            dirty = true;

            if (structure.progressTicks() % 20 == 0) {
                spawnSmoke(level, structure);
            }

            if (structure.shouldCheckSeal()) {
                structure.sampleSeal(calculateSealedRatio(level, structure.members()));
                shouldSync = true;
            }

            if (structure.isFinished() && canResolveStructure(level, structure)) {
                finishedStructures.add(structure.masterPos());
            }
        }

        for (Long masterPos : finishedStructures) {
            finishStructure(level, masterPos);
        }

        if (dirty) {
            setDirty();
        }

        if (shouldSync || !finishedStructures.isEmpty()) {
            ModNetwork.syncCharcoalPits(level, this);
        }
    }

    public List<CharcoalPitSyncEntry> getSyncEntries(ServerLevel level) {
        List<CharcoalPitSyncEntry> entries = new ArrayList<>(activeStructures.size());
        long gameTime = level.getGameTime();

        for (CharcoalPitStructure structure : activeStructures.values()) {
            entries.add(new CharcoalPitSyncEntry(
                    structure.masterPos(),
                    structure.members(),
                    gameTime + Math.max(0, structure.burnTicks() - structure.progressTicks()),
                    structure.burnTicks(),
                    structure.currentSealedRatio(),
                    structure.qualifyingRatio(),
                    structure.isProjectedToSucceed()
            ));
        }

        return entries;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag structures = new ListTag();
        for (CharcoalPitStructure structure : activeStructures.values()) {
            structures.add(structure.save());
        }
        tag.put(STRUCTURES_TAG, structures);
        return tag;
    }

    private void finishStructure(ServerLevel level, long masterPos) {
        CharcoalPitStructure structure = activeStructures.remove(masterPos);
        if (structure == null) {
            return;
        }

        unindexStructure(structure);
        boolean producesCharcoal = structure.qualifyingRatio() >= HarderBeginningsConfig.charcoalRequiredQualifiedTimeRatio();

        for (Long memberPos : structure.members()) {
            BlockPos pos = BlockPos.of(memberPos);
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS)) {
                continue;
            }

            level.removeBlock(pos, false);
            if (producesCharcoal) {
                Block.popResource(level, pos, new ItemStack(Items.CHARCOAL));
            }
        }

        setDirty();
    }

    private boolean canResolveStructure(ServerLevel level, CharcoalPitStructure structure) {
        for (Long memberPos : structure.members()) {
            if (!level.isLoaded(BlockPos.of(memberPos))) {
                return false;
            }
        }
        return true;
    }

    private void spawnSmoke(ServerLevel level, CharcoalPitStructure structure) {
        BlockPos masterPos = BlockPos.of(structure.masterPos());
        double x = masterPos.getX() + 0.5D;
        double y = masterPos.getY() + 1.05D;
        double z = masterPos.getZ() + 0.5D;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, 2, 0.2D, 0.15D, 0.2D, 0.01D);
    }

    private void indexStructure(CharcoalPitStructure structure) {
        for (Long memberPos : structure.members()) {
            slaveToMaster.put(memberPos, structure.masterPos());
        }
    }

    private void unindexStructure(CharcoalPitStructure structure) {
        for (Long memberPos : structure.members()) {
            slaveToMaster.remove(memberPos);
        }
    }

    private static BlockPos selectMaster(Set<Long> members) {
        BlockPos masterPos = null;
        for (Long memberPos : members) {
            BlockPos candidate = BlockPos.of(memberPos);
            if (masterPos == null || MASTER_ORDER.compare(candidate, masterPos) < 0) {
                masterPos = candidate;
            }
        }
        return masterPos == null ? BlockPos.ZERO : masterPos;
    }

    private static Set<Long> floodFillLogs(ServerLevel level, BlockPos origin) {
        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());
        visited.add(origin.asLong());

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                long neighborPos = neighbor.asLong();
                if (visited.contains(neighborPos)) {
                    continue;
                }

                if (level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                    visited.add(neighborPos);
                    queue.addLast(neighbor.immutable());
                }
            }
        }

        return visited;
    }

    private static double calculateSealedRatio(ServerLevel level, Set<Long> members) {
        if (members.isEmpty()) {
            return 0.0D;
        }

        long coveredFaces = 0L;
        long totalFaces = (long) members.size() * Direction.values().length;

        for (Long memberPos : members) {
            BlockPos current = BlockPos.of(memberPos);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.isFaceSturdy(level, neighbor, direction.getOpposite())) {
                    coveredFaces++;
                }
            }
        }

        return (double) coveredFaces / (double) totalFaces;
    }

    public static final class CharcoalPitStructure {
        private final long masterPos;
        private final Set<Long> members;
        private int progressTicks;
        private final int burnTicks;
        private double currentSealedRatio;
        private int checkedSamples;
        private int fullySealedSamples;

        private CharcoalPitStructure(long masterPos, Set<Long> members, int progressTicks, int burnTicks,
                                     double currentSealedRatio, int checkedSamples, int fullySealedSamples) {
            this.masterPos = masterPos;
            this.members = new HashSet<>(members);
            this.progressTicks = progressTicks;
            this.burnTicks = burnTicks;
            this.currentSealedRatio = currentSealedRatio;
            this.checkedSamples = checkedSamples;
            this.fullySealedSamples = fullySealedSamples;
        }

        public static CharcoalPitStructure load(CompoundTag tag) {
            Set<Long> members = new HashSet<>();
            for (long memberPos : tag.getLongArray(MEMBERS_TAG)) {
                members.add(memberPos);
            }

            return new CharcoalPitStructure(
                    tag.getLong(MASTER_TAG),
                    members,
                    tag.getInt(PROGRESS_TAG),
                    tag.getInt(BURN_TICKS_TAG),
                    tag.getDouble(CURRENT_SEALED_RATIO_TAG),
                    tag.getInt(CHECKED_SAMPLES_TAG),
                    tag.getInt(FULLY_SEALED_SAMPLES_TAG)
            );
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            long[] serializedMembers = new long[members.size()];
            int index = 0;
            for (Long memberPos : members) {
                serializedMembers[index++] = memberPos;
            }

            tag.putLong(MASTER_TAG, masterPos);
            tag.putLongArray(MEMBERS_TAG, serializedMembers);
            tag.putInt(PROGRESS_TAG, progressTicks);
            tag.putInt(BURN_TICKS_TAG, burnTicks);
            tag.putDouble(CURRENT_SEALED_RATIO_TAG, currentSealedRatio);
            tag.putInt(CHECKED_SAMPLES_TAG, checkedSamples);
            tag.putInt(FULLY_SEALED_SAMPLES_TAG, fullySealedSamples);
            return tag;
        }

        public void advance() {
            progressTicks++;
        }

        public boolean shouldCheckSeal() {
            return progressTicks % HarderBeginningsConfig.charcoalCheckIntervalTicks() == 0;
        }

        public void sampleSeal(double sealedRatio) {
            currentSealedRatio = sealedRatio;
            checkedSamples++;
            if (sealedRatio >= HarderBeginningsConfig.charcoalRequiredSealRatio()) {
                fullySealedSamples++;
            }
        }

        public boolean isFinished() {
            return progressTicks >= burnTicks;
        }

        public long masterPos() {
            return masterPos;
        }

        public Set<Long> members() {
            return members;
        }

        public int progressTicks() {
            return progressTicks;
        }

        public int burnTicks() {
            return burnTicks;
        }

        public double currentSealedRatio() {
            return currentSealedRatio;
        }

        public double qualifyingRatio() {
            if (checkedSamples <= 0) {
                return 0.0D;
            }
            return (double) fullySealedSamples / (double) checkedSamples;
        }

        public boolean isProjectedToSucceed() {
            return qualifyingRatio() >= HarderBeginningsConfig.charcoalRequiredQualifiedTimeRatio();
        }
    }

    public record CharcoalPitInfo(BlockPos masterPos, int remainingTicks, int totalTicks,
                                  double currentSealedRatio, double qualifyingRatio,
                                  boolean projectedToSucceed) {
    }

    public record CharcoalPitSyncEntry(long masterPos, Set<Long> members, long endGameTime, int totalTicks,
                                       double currentSealedRatio, double qualifyingRatio,
                                       boolean projectedToSucceed) {
    }
}
