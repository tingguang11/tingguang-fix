package com.fix.myfix.recipe;

import com.fix.myfix.MyFix;
import com.fix.myfix.inti.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public class ClayKilnRecipe implements Recipe<SimpleContainer> {
    public static final RecipeType<ClayKilnRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return MyFix.MODID + ":clay_kiln";
        }
    };

    private final ResourceLocation id;
    private final List<CountedIngredient> ingredients;
    private final ItemStack result;
    private final int cookTime;

    public ClayKilnRecipe(ResourceLocation id, List<CountedIngredient> ingredients, ItemStack result, int cookTime) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.cookTime = cookTime;
    }

    public static ClayKilnRecipe findMatch(Level level, NonNullList<ItemStack> inputItems) {
        for (ClayKilnRecipe recipe : level.getRecipeManager().getAllRecipesFor(TYPE)) {
            if (recipe.matchesInputs(inputItems)) {
                return recipe;
            }
        }
        return null;
    }

    public int getCookTime() {
        return cookTime;
    }

    public boolean matchesInputs(NonNullList<ItemStack> inputItems) {
        List<ItemStack> remaining = copyInputs(inputItems);
        for (CountedIngredient countedIngredient : ingredients) {
            int needed = countedIngredient.count();
            for (ItemStack stack : remaining) {
                if (needed <= 0) {
                    break;
                }

                if (!countedIngredient.ingredient().test(stack)) {
                    continue;
                }

                int used = Math.min(needed, stack.getCount());
                needed -= used;
                stack.shrink(used);
            }

            if (needed > 0) {
                return false;
            }
        }
        return true;
    }

    public void consumeInputs(NonNullList<ItemStack> inputItems) {
        for (CountedIngredient countedIngredient : ingredients) {
            int needed = countedIngredient.count();
            for (int i = 0; i < inputItems.size() && needed > 0; i++) {
                ItemStack stack = inputItems.get(i);
                if (stack.isEmpty() || !countedIngredient.ingredient().test(stack)) {
                    continue;
                }

                int used = Math.min(needed, stack.getCount());
                stack.shrink(used);
                if (stack.isEmpty()) {
                    inputItems.set(i, ItemStack.EMPTY);
                }
                needed -= used;
            }
        }
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        NonNullList<ItemStack> inputItems = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            inputItems.set(i, container.getItem(i));
        }
        return matchesInputs(inputItems);
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CLAY_KILN.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    private static List<ItemStack> copyInputs(NonNullList<ItemStack> inputItems) {
        List<ItemStack> copied = new ArrayList<>(inputItems.size());
        for (ItemStack inputItem : inputItems) {
            copied.add(inputItem.copy());
        }
        return copied;
    }

    public static class Serializer implements RecipeSerializer<ClayKilnRecipe> {
        @Override
        public ClayKilnRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(json, "ingredients");
            List<CountedIngredient> ingredients = new ArrayList<>(ingredientsJson.size());
            for (int i = 0; i < ingredientsJson.size(); i++) {
                ingredients.add(CountedIngredient.fromJson(ingredientsJson.get(i).getAsJsonObject()));
            }

            JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
            ItemStack result = ShapedRecipe.itemStackFromJson(resultJson);
            int cookTime = GsonHelper.getAsInt(json, "cookTime", 200);
            return new ClayKilnRecipe(recipeId, ingredients, result, cookTime);
        }

        @Override
        public ClayKilnRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int ingredientCount = buffer.readVarInt();
            List<CountedIngredient> ingredients = new ArrayList<>(ingredientCount);
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.add(CountedIngredient.fromNetwork(buffer));
            }

            ItemStack result = buffer.readItem();
            int cookTime = buffer.readVarInt();
            return new ClayKilnRecipe(recipeId, ingredients, result, cookTime);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ClayKilnRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            for (CountedIngredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.cookTime);
        }
    }
}
