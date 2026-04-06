package com.fix.myfix.client;

import com.fix.myfix.MyFix;
import com.fix.myfix.client.render.WoodWorkbenchRenderer;
import com.fix.myfix.inti.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyFix.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.WOOD_WORKBENCH.get(),
                WoodWorkbenchRenderer::new
        );
    }
}
