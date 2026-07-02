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
                for (String disabled : config.disabled_enchantments) {
                    String disabledPath = disabled.contains(":")
                            ? disabled.split(":")[1]
                            : disabled;
                    // Matches "fire_aspect_1", "fire_aspect_2", etc.
                    if (path.startsWith(disabledPath + "_") &&
                            entry.id().getValue().getNamespace().equals(CraftableEnchantementMod.MOD_ID)) {
                        return true;
                    }
                }
                return false;
            });
        }

        // 2. Add custom recipes from config
        for (ModConfig.CustomRecipeEntry entry : config.custom_recipes) {
            RecipeEntry<?> recipe = buildRecipe(entry);
            if (recipe != null) {
                recipes.add(recipe);
            }
        }

        return PreparedRecipes.of(recipes);
    }

    private RecipeEntry<ShapelessRecipe> buildRecipe(ModConfig.CustomRecipeEntry entry) {
        if (entry.enchantment == null || entry.enchantment.isEmpty()
                || entry.ingredient == null || entry.ingredient.isEmpty()
                || entry.level < 1) {
            CraftableEnchantementMod.LOGGER.warn(
                    "[CraftableEnchantement] Skipping invalid custom recipe entry: {}", entry.enchantment);
            return null;
        }

        // Resolve enchantment
        Identifier enchId = Identifier.tryParse(entry.enchantment);
        if (enchId == null) {
            CraftableEnchantementMod.LOGGER.warn(
                    "[CraftableEnchantement] Invalid enchantment ID: {}", entry.enchantment);
            return null;
        }

        RegistryKey<Enchantment> enchKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, enchId);
        Optional<RegistryEntry.Reference<Enchantment>> enchEntry =
                this.registries.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(enchKey);

        if (enchEntry.isEmpty()) {
            CraftableEnchantementMod.LOGGER.warn(
                    "[CraftableEnchantement] Enchantment not found in registry: {}", entry.enchantment);
            return null;
        }

        // Resolve ingredient item
        Identifier ingId = Identifier.tryParse(entry.ingredient);
        if (ingId == null || !Registries.ITEM.containsId(ingId)) {
            CraftableEnchantementMod.LOGGER.warn(
                    "[CraftableEnchantement] Item not found: {}", entry.ingredient);
            return null;
        }

        // Build result ItemStack
        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
        net.minecraft.component.type.ItemEnchantmentsComponent.Builder builder =
                new net.minecraft.component.type.ItemEnchantmentsComponent.Builder(
                        net.minecraft.component.type.ItemEnchantmentsComponent.DEFAULT);
        builder.add(enchEntry.get(), entry.level);
        result.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());

        // Build ingredients: N×XP bottle + book + thematic item
        List<Ingredient> ingredients = new ArrayList<>();
        Ingredient xpBottle = Ingredient.ofItems(Items.EXPERIENCE_BOTTLE);
        for (int i = 0; i < entry.level; i++) {
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

        // Build a unique registry key for this custom recipe
        String safeName = enchId.getNamespace().equals("minecraft")
                ? enchId.getPath()
                : enchId.getNamespace() + "_" + enchId.getPath();
        RegistryKey<Recipe<?>> recipeKey = RegistryKey.of(
                RegistryKeys.RECIPE,
                Identifier.of(CraftableEnchantementMod.MOD_ID, "custom/" + safeName + "_" + entry.level)
        );

        return new RecipeEntry<>(recipeKey, recipe);
    }
}
