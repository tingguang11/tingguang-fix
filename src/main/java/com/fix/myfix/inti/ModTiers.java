package com.fix.myfix.inti;

import com.fix.myfix.MyFix;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

public final class ModTiers {
    public static final Tier FLINT = TierSortingRegistry.registerTier(
            new Tier() {
                @Override
                public int getUses() {
                    return 96;
                }

                @Override
                public float getSpeed() {
                    return 4.0F;
                }

                @Override
                public float getAttackDamageBonus() {
                    return 1.0F;
                }

                @Override
                @SuppressWarnings("deprecation")
                public int getLevel() {
                    return 1;
                }

                @Override
                public int getEnchantmentValue() {
                    return 5;
                }

                @Override
                public Ingredient getRepairIngredient() {
                    return Ingredient.of(Items.FLINT);
                }
            },
            ResourceLocation.fromNamespaceAndPath(MyFix.MODID, "flint"),
            List.of(Tiers.STONE),
            List.of(Tiers.IRON)
    );

    private ModTiers() {
    }
}
