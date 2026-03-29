package com.fix.myfix.inti.blocks;

import com.fix.myfix.inti.ModItems;
import com.fix.myfix.inti.blocks.entity.SimpleWorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class SimpleStoneWorkbenchBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public SimpleStoneWorkbenchBlock() {
        super(Properties.of().strength(2f).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    private static final AbstractContainerMenu DUMMY_MENU = new AbstractContainerMenu(null, -1) {
        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player player) { return false; }
    };

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleWorkbenchBlockEntity(pos, state);
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {

        if (!state.is(newState.getBlock())) {

            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);

                if (be instanceof SimpleWorkbenchBlockEntity workbench) {

                    for (ItemStack stack : workbench.getItems()) {
                        if (!stack.isEmpty()) {
                            Containers.dropItemStack(
                                    level,
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ(),
                                    stack
                            );
                        }
                    }

                    // ❗ 防止重复掉落
                    workbench.clear();
                }
            }

            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) return InteractionResult.CONSUME;

        if (hit.getDirection() != Direction.UP) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SimpleWorkbenchBlockEntity workbench)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        int slot = getSlot(hit);

        // 锤子合成
        if (held.is(ModItems.HAMMER.get())) {
            craft(workbench, player);
            return InteractionResult.CONSUME;
        }

        // 取出
        if (held.isEmpty()) {
            ItemStack out = workbench.removeItem(slot);
            if (!out.isEmpty()) {
                player.addItem(out);
                return InteractionResult.CONSUME;
            }
        }

        // 放入
        if (!held.isEmpty()) {
            ItemStack copy = held.copy();
            copy.setCount(1);
            if (workbench.addItem(slot, copy)) {
                held.shrink(1);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    private int getSlot(BlockHitResult hit) {

        Vec3 loc = hit.getLocation();
        BlockPos pos = hit.getBlockPos();

        double x = loc.x - pos.getX();
        double z = loc.z - pos.getZ();

        int col = Math.min(2, (int)(x * 3));
        int row = Math.min(2, (int)(z * 3));

        return row * 3 + col;
    }

    private void craft(SimpleWorkbenchBlockEntity be, Player player) {

        CraftingContainer inv = new TransientCraftingContainer(DUMMY_MENU, 3, 3);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, be.getItems().get(i));
        }

        Level level = player.level();

        Optional<CraftingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, inv, level);

        if (recipe.isEmpty()) return;

        ItemStack result = recipe.get().assemble(inv, level.registryAccess());

        level.addFreshEntity(new ItemEntity(
                level,
                be.getBlockPos().getX() + 0.5,
                be.getBlockPos().getY() + 1,
                be.getBlockPos().getZ() + 0.5,
                result
        ));

        NonNullList<ItemStack> remaining = recipe.get().getRemainingItems(inv);

        NonNullList<ItemStack> newItems = NonNullList.withSize(9, ItemStack.EMPTY);

        for (int i = 0; i < 9; i++) {
            if (!remaining.get(i).isEmpty()) {
                newItems.set(i, remaining.get(i).copy());
            }
        }

        be.setAll(newItems);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}