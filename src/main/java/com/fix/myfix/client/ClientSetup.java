package com.fix.myfix.client;

import com.fix.myfix.inti.ModBlockEntities;
import com.fix.myfix.client.render.SimpleWorkbenchRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "myfix", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.SIMPLE_WORKBENCH.get(),
                SimpleWorkbenchRenderer::new
        );
    }
}