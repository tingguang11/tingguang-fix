package com.fix.myfix.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

public record CountedIngredient(Ingredient ingredient, int count) {
    public static CountedIngredient fromJson(JsonObject json) {
        JsonObject ingredientJson = GsonHelper.getAsJsonObject(json, "ingredient");
        Ingredient ingredient = Ingredient.fromJson(ingredientJson);
        int count = GsonHelper.getAsInt(json, "count", 1);
        return new CountedIngredient(ingredient, Math.max(1, count));
    }

    public static CountedIngredient fromNetwork(FriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.fromNetwork(buffer);
        int count = buffer.readVarInt();
        return new CountedIngredient(ingredient, count);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        ingredient.toNetwork(buffer);
        buffer.writeVarInt(count);
    }
}
