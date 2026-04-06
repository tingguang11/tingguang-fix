package com.fix.myfix.event;

import com.fix.myfix.MyFix;
import com.fix.myfix.config.HarderBeginningsConfig;
import com.fix.myfix.inti.blocks.ClayKilnPortBlock;
import com.fix.myfix.inti.ModItems;
import com.fix.myfix.network.ModNetwork;
import com.fix.myfix.system.CampfireFuelSavedData;
import com.fix.myfix.system.CharcoalPitSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

import java.util.Set;

@Mod.EventBusSubscriber(modid = MyFix.MODID)
public final class ModGameplayEvents {
    private static final Set<Block> FIBER_PLANTS = Set.of(
            Blocks.GRASS,
            Blocks.FERN,
            Blocks.TALL_GRASS,
            Blocks.LARGE_FERN
    );

    private ModGameplayEvents() {
    }

    public static boolean isFiberPlant(BlockState state) {
        return FIBER_PLANTS.contains(state.getBlock());
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.getAbilities().instabuild) {
            return;
        }

        if (player.getMainHandItem().isEmpty() && event.getState().is(BlockTags.LOGS)) {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) {
            return;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            CharcoalPitSavedData charcoalPits = CharcoalPitSavedData.get(serverLevel);
            if (charcoalPits.isTracked(event.getPos())) {
                if (player.getAbilities().instabuild) {
                    charcoalPits.discardStructureContaining(serverLevel, event.getPos());
                    return;
                }

                event.setCanceled(true);
                return;
            }
        }

        if (player.getAbilities().instabuild) {
            return;
        }

        BlockState state = event.getState();
        ItemStack heldItem = player.getMainHandItem();

        if (state.getBlock() instanceof CampfireBlock && player.level() instanceof ServerLevel serverLevel) {
            CampfireFuelSavedData.get(serverLevel).remove(event.getPos());
        }

        if (heldItem.isEmpty() && state.is(BlockTags.LOGS)) {
            event.setCanceled(true);
            return;
        }

        if (heldItem.is(ModItems.FLINT_KNIFE.get())
                && isFiberPlant(state)
                && player.level().random.nextDouble() < HarderBeginningsConfig.fiberDropChance()) {
            Block.popResource(player.level(), event.getPos(), new ItemStack(ModItems.FIBER.get()));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (tryCampfireInteraction(event, serverLevel)) {
            return;
        }

        if (serverLevel.getBlockState(event.getPos()).getBlock() instanceof ClayKilnPortBlock) {
            return;
        }

        if (tryPrimitiveIgnition(event, serverLevel)) {
            return;
        }

        ItemStack heldItem = event.getItemStack();
        if (!isIgnitionItem(heldItem)) {
            return;
        }

        BlockPos ignitionPos = findIgnitionTarget(serverLevel, event.getPos(), event.getFace());
        if (ignitionPos == null) {
            return;
        }

        CharcoalPitSavedData charcoalPits = CharcoalPitSavedData.get(serverLevel);
        if (!charcoalPits.tryIgnite(serverLevel, ignitionPos)) {
            return;
        }

        consumeIgnitionItem(event.getEntity(), event.getHand(), heldItem);
        serverLevel.playSound(
                null,
                ignitionPos,
                SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.BLOCKS,
                1.0F,
                serverLevel.random.nextFloat() * 0.4F + 0.8F
        );
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        CampfireFuelSavedData.get(serverLevel).tick(serverLevel);
        CharcoalPitSavedData.get(serverLevel).tick(serverLevel);
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState placedState = event.getPlacedBlock();
        if (!(placedState.getBlock() instanceof CampfireBlock) || !placedState.getValue(BlockStateProperties.LIT)) {
            return;
        }

        serverLevel.setBlock(event.getPos(), placedState.setValue(BlockStateProperties.LIT, false), 3);
        CampfireFuelSavedData.get(serverLevel).remove(event.getPos());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModNetwork.syncCharcoalPits(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModNetwork.syncCharcoalPits(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModNetwork.syncCharcoalPits(serverPlayer);
        }
    }

    private static boolean isIgnitionItem(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    private static boolean tryCampfireInteraction(PlayerInteractEvent.RightClickBlock event, ServerLevel level) {
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CampfireBlock)) {
            return false;
        }

        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();
        CampfireFuelSavedData campfireFuel = CampfireFuelSavedData.get(level);

        int addedFuelTicks = HarderBeginningsConfig.getCampfireFuelTicks(heldItem);
        if (addedFuelTicks > 0) {
            campfireFuel.addFuel(pos, addedFuelTicks);
            consumeFuelItem(player, event.getHand(), heldItem);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7F, 0.9F);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return true;
        }

        if (heldItem.is(Items.FLINT) && player.getOffhandItem().is(Items.FLINT)) {
            if (!campfireFuel.hasFuel(pos)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return true;
            }

            return tryPrimitiveCampfireIgnition(event, level, campfireFuel, pos);
        }

        if (!isIgnitionItem(heldItem)) {
            return false;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (!campfireFuel.hasFuel(pos)) {
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.5F, 0.5F);
            level.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.7D,
                    pos.getZ() + 0.5D,
                    4,
                    0.15D,
                    0.1D,
                    0.15D,
                    0.01D);
            return true;
        }

        if (campfireFuel.tryIgnite(level, pos)) {
            consumeIgnitionItem(player, event.getHand(), heldItem);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                    level.random.nextFloat() * 0.4F + 0.8F);
        }
        return true;
    }

    private static boolean tryPrimitiveIgnition(PlayerInteractEvent.RightClickBlock event, ServerLevel level) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (!mainHand.is(Items.FLINT) || !offHand.is(Items.FLINT)) {
            return false;
        }

        BlockPos clickedPos = event.getPos();
        Direction face = event.getFace();
        if (face == null) {
            return false;
        }

        BlockPos firePos = clickedPos.relative(face);
        BlockPos ignitionPos = findIgnitionTarget(level, clickedPos, face);
        boolean canPlaceFire = canPlaceFire(level, firePos);

        if (!canPlaceFire && ignitionPos == null) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            mainHand.shrink(1);
            offHand.shrink(1);
        }

        boolean success = level.random.nextBoolean();
        if (!success) {
            level.playSound(null, clickedPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.6F, 0.6F);
            level.sendParticles(ParticleTypes.SMOKE,
                    firePos.getX() + 0.5D,
                    firePos.getY() + 0.1D,
                    firePos.getZ() + 0.5D,
                    4,
                    0.1D,
                    0.05D,
                    0.1D,
                    0.01D);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return true;
        }

        boolean ignitedPit = ignitionPos != null && CharcoalPitSavedData.get(level).tryIgnite(level, ignitionPos);
        if (canPlaceFire) {
            level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
        }

        if (canPlaceFire || ignitedPit) {
            level.playSound(null, firePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                    level.random.nextFloat() * 0.4F + 0.8F);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return true;
        }

        return false;
    }

    private static boolean tryPrimitiveCampfireIgnition(PlayerInteractEvent.RightClickBlock event, ServerLevel level,
                                                        CampfireFuelSavedData campfireFuel, BlockPos pos) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (!player.getAbilities().instabuild) {
            mainHand.shrink(1);
            offHand.shrink(1);
        }

        boolean success = level.random.nextBoolean();
        if (!success) {
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.6F, 0.6F);
            level.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.7D,
                    pos.getZ() + 0.5D,
                    4,
                    0.15D,
                    0.1D,
                    0.15D,
                    0.01D);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return true;
        }

        if (campfireFuel.tryIgnite(level, pos)) {
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                    level.random.nextFloat() * 0.4F + 0.8F);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        return true;
    }

    private static BlockPos findIgnitionTarget(Level level, BlockPos clickedPos, Direction face) {
        if (level.getBlockState(clickedPos).is(BlockTags.LOGS)) {
            return clickedPos;
        }

        if (face == null) {
            return null;
        }

        BlockPos adjacentPos = clickedPos.relative(face);
        if (level.getBlockState(adjacentPos).is(BlockTags.LOGS)) {
            return adjacentPos;
        }

        return null;
    }

    private static boolean canPlaceFire(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        return BaseFireBlock.canBePlacedAt(level, pos, Direction.UP);
    }

    private static void consumeIgnitionItem(Player player, InteractionHand hand, ItemStack heldItem) {
        if (player.getAbilities().instabuild) {
            return;
        }

        if (heldItem.is(Items.FLINT_AND_STEEL)) {
            heldItem.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
            return;
        }

        if (heldItem.is(Items.FIRE_CHARGE)) {
            heldItem.shrink(1);
        }
    }

    private static void consumeFuelItem(Player player, InteractionHand hand, ItemStack heldItem) {
        if (player.getAbilities().instabuild) {
            return;
        }

        ItemStack remainder = heldItem.getCraftingRemainingItem();
        heldItem.shrink(1);

        if (remainder.isEmpty()) {
            return;
        }

        if (heldItem.isEmpty()) {
            player.setItemInHand(hand, remainder);
            return;
        }

        if (!player.addItem(remainder)) {
            player.drop(remainder, false);
        }
    }
}
