package com.fix.myfix.recipe;

import com.fix.myfix.MyFix;
import com.fix.myfix.config.HarderBeginningsConfig;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public record FeatureToggleCondition(String feature) implements ICondition {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MyFix.MODID, "feature_enabled");
    public static final Serializer SERIALIZER = new Serializer();

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return switch (feature) {
            case "torch" -> HarderBeginningsConfig.torchEnabled();
            case "flint_tools" -> HarderBeginningsConfig.flintToolsEnabled();
            case "wood_workbench" -> HarderBeginningsConfig.woodWorkbenchEnabled();
            case "death_penalty" -> HarderBeginningsConfig.deathPenaltyEnabled();
            default -> false;
        };
    }

    public static final class Serializer implements IConditionSerializer<FeatureToggleCondition> {
        @Override
        public void write(JsonObject json, FeatureToggleCondition value) {
            json.addProperty("feature", value.feature);
        }

        @Override
        public FeatureToggleCondition read(JsonObject json) {
            return new FeatureToggleCondition(GsonHelper.getAsString(json, "feature"));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
