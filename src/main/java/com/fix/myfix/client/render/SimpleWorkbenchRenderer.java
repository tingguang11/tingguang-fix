package com.fix.myfix.client.render;

import com.fix.myfix.inti.blocks.entity.SimpleWorkbenchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SimpleWorkbenchRenderer implements BlockEntityRenderer<SimpleWorkbenchBlockEntity> {

    public SimpleWorkbenchRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(SimpleWorkbenchBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {

        poseStack.pushPose();

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
                    be.getBlockPos().hashCode()
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}