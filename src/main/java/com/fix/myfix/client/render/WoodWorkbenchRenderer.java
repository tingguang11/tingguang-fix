package com.fix.myfix.client.render;

import com.fix.myfix.inti.blocks.WoodWorkbenchBlock;
import com.fix.myfix.inti.blocks.entity.WoodWorkbenchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class WoodWorkbenchRenderer implements BlockEntityRenderer<WoodWorkbenchBlockEntity> {

    public WoodWorkbenchRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(WoodWorkbenchBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(getWorkbenchRotation(be)));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = be.getItems().get(i);
            if (stack.isEmpty()) continue;

            poseStack.pushPose();

            int row = i / 3;
            int col = i % 3;

            double x = (col + 0.5) / 3.0;
            double z = (row + 0.5) / 3.0;

            poseStack.translate(x, 0.51, z);
            poseStack.scale(0.25f, 0.25f, 0.25f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    light,
                    overlay,
                    poseStack,
                    buffer,
                    be.getLevel(),
                    be.getBlockPos().hashCode() + i
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private float getWorkbenchRotation(WoodWorkbenchBlockEntity blockEntity) {
        Direction facing = blockEntity.getBlockState().getValue(WoodWorkbenchBlock.FACING);
        return switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
