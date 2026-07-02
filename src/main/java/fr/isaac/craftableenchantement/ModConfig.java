package fr.isaac.craftableenchantement;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    /**
     * List of enchantment IDs whose built-in recipes are disabled.
     * Example: ["minecraft:mending", "minecraft:silk_touch"]
     */
    public List<String> disabled_enchantments = new ArrayList<>();

    /**
     * Custom shapeless enchanted-book recipes.
     * Each entry: N XP bottles (= level) + book + ingredient → enchanted book at given level.
     * Works for any enchantment ID, including modded ones.
     */
    public List<CustomRecipeEntry> custom_recipes = new ArrayList<>();

    public static class CustomRecipeEntry {
        /** Enchantment resource location, e.g. "minecraft:fire_aspect" or "mymod:custom_enchant" */
        public String enchantment = "";
        /** Enchantment level (number of XP bottles required). */
        public int level = 1;
        /** Item resource location used as thematic ingredient, e.g. "minecraft:flint_and_steel" */
        public String ingredient = "";
    }
}
