package com.fix.myfix;

import com.fix.myfix.config.HarderBeginningsConfig;
import com.fix.myfix.inti.ModBlockEntities;
import com.fix.myfix.inti.ModBlocks;
import com.fix.myfix.inti.ModCreativeModeTabs;
import com.fix.myfix.inti.ModItems;
import com.fix.myfix.inti.ModMobEffects;
import com.fix.myfix.inti.ModRecipes;
import com.fix.myfix.network.ModNetwork;
import com.fix.myfix.recipe.FeatureToggleCondition;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MyFix.MODID)
public class MyFix {
    public static final String MODID = "harder_beginnings";

    @SuppressWarnings("removal")
    public MyFix(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HarderBeginningsConfig.SPEC);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModNetwork.register();

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CraftingHelper.register(FeatureToggleCondition.SERIALIZER));
    }
}
