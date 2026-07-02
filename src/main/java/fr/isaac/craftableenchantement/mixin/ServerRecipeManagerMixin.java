package fr.isaac.craftableenchantement.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fr.isaac.craftableenchantement.ConfigLoader;
import fr.isaac.craftableenchantement.CraftableEnchantementMod;
import fr.isaac.craftableenchantement.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(ServerRecipeManager.class)
public abstract class ServerRecipeManagerMixin {

    @Shadow
    private RegistryWrapper.WrapperLookup registries;

    @ModifyVariable(
            method = "apply(Lnet/minecraft/recipe/PreparedRecipes;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private PreparedRecipes craftableenchantement$applyConfig(PreparedRecipes original) {
        ConfigLoader.invalidate();
        ModConfig config = ConfigLoader.get();

        List<RecipeEntry<?>> recipes = new ArrayList<>(original.recipes());

        // 1. Remove disabled enchantment recipes
        if (!config.disabled_enchantments.isEmpty()) {
            recipes.removeIf(entry -> {
                String path = entry.id().getValue().getPath(); // e.g. "fire_aspect_1"
                if (!entry.id().getValue().getNamespace().equals(CraftableEnchantementMod.MOD_ID)) return false;
                for (String disabled : config.disabled_enchantments) {
                    String disabledPath = disabled.contains(":")
                            ? disabled.split(":")[1]
                            : disabled;
                    // Exact match  → e.g. "sharpness_5"  disables just that level
                    if (path.equals(disabledPath)) return true;
                    // Prefix match → e.g. "sharpness"     disables all levels
                    if (path.startsWith(disabledPath + "_")) return true;
                }
                return false;
            });
        }

        // 2. Add custom recipes from config
        for (String entry : config.custom_recipes) {
            String[] parts = entry.split("\\|", 3);
            if (parts.length != 3) {
                CraftableEnchantementMod.LOGGER.warn(
                        "[CraftableEnchantement] Invalid custom recipe format (expected enchantment|level|ingredient): {}",
                        entry);
                continue;
            }
            RecipeEntry<?> recipe = buildRecipe(parts[0].trim(), parts[1].trim(), parts[2].trim());
            if (recipe != null) {
                recipes.add(recipe);
            }
        }

        return PreparedRecipes.of(recipes);
    }

    private RecipeEntry<ShapelessRecipe> buildRecipe(String enchantmentId, String levelStr, String ingredientId) {
        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            CraftableEnchantementMod.LOGGER.warn(
                    "[CraftableEnchantement] Invalid level '{}' in custom recipe", levelStr);
            return null;
        }
        if (level < 1) return null;

        // Resolve enchantment
        Identifier enchId = Identifier.tryParse(enchantmentId);
        if (enchId == null) {
            CraftableEnchantementMod.LOGGER.warn(
                    "[CraftableEnchantement] Invalid enchantment ID: {}", enchantmentId);
            return null;
        }

        RegistryKey<Enchantment> enchKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, enchId);
        Optional<RegistryEntry.Reference<Enchantment>> enchEntry =
                this.registries.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(enchKey);

        if (enchEntry.isEmpty()) {
            CraftableEnchantementMod.LOGGER.warn(
                    "[CraftableEnchantement] Enchantment not found: {}", enchantmentId);
            return null;
        }

        // Resolve ingredient item
        Identifier ingId = Identifier.tryParse(ingredientId);
        if (ingId == null || !Registries.ITEM.containsId(ingId)) {
            CraftableEnchantementMod.LOGGER.warn(
                    "[CraftableEnchantement] Item not found: {}", ingredientId);
            return null;
        }

        // Build result ItemStack
        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
        net.minecraft.component.type.ItemEnchantmentsComponent.Builder builder =
                new net.minecraft.component.type.ItemEnchantmentsComponent.Builder(
                        net.minecraft.component.type.ItemEnchantmentsComponent.DEFAULT);
        builder.add(enchEntry.get(), level);
        result.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());

        // Build ingredients
        List<Ingredient> ingredients = new ArrayList<>();
        Ingredient xpBottle = Ingredient.ofItems(Items.EXPERIENCE_BOTTLE);
        for (int i = 0; i < level; i++) {
            ingredients.add(xpBottle);
        }
        ingredients.add(Ingredient.ofItems(Items.BOOK));
        ingredients.add(Ingredient.ofItems(Registries.ITEM.get(ingId)));

        ShapelessRecipe recipe = new ShapelessRecipe(
                CraftableEnchantementMod.MOD_ID,
                CraftingRecipeCategory.EQUIPMENT,
                result,
                ingredients
        );

        String safeName = enchId.getNamespace().equals("minecraft")
                ? enchId.getPath()
                : enchId.getNamespace() + "_" + enchId.getPath();
        RegistryKey<Recipe<?>> recipeKey = RegistryKey.of(
                RegistryKeys.RECIPE,
                Identifier.of(CraftableEnchantementMod.MOD_ID, "custom/" + safeName + "_" + level)
        );

        return new RecipeEntry<>(recipeKey, recipe);
    }
}
