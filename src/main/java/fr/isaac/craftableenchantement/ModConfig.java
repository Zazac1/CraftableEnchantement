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
     * Format per entry: "enchantment_id|level|ingredient_id"
     * Example: ["minecraft:fire_aspect|1|minecraft:flint_and_steel", "mymod:custom|2|minecraft:diamond"]
     */
    public List<String> custom_recipes = new ArrayList<>();
}

