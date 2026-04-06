package com.fix.myfix.inti;

import com.fix.myfix.MyFix;
import com.fix.myfix.recipe.ClayKilnRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MyFix.MODID);

    public static final RegistryObject<RecipeSerializer<?>> CLAY_KILN =
            RECIPE_SERIALIZERS.register("clay_kiln", ClayKilnRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
