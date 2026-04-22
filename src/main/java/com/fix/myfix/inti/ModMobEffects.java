package com.fix.myfix.inti;

import com.fix.myfix.MyFix;
import com.fix.myfix.effect.AftereffectsMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MyFix.MODID);

    public static final RegistryObject<MobEffect> AFTEREFFECTS =
            MOB_EFFECTS.register("aftereffects", AftereffectsMobEffect::new);

    private ModMobEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
