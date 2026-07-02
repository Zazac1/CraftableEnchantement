package fr.isaac.craftableenchantement.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fr.isaac.craftableenchantement.ConfigLoader;
import fr.isaac.craftableenchantement.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ConfigLoader.get();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Craftable Enchantement"))
                    .setSavingRunnable(() -> ConfigLoader.saveAndInvalidate(config));

            ConfigEntryBuilder eb = builder.entryBuilder();

            ConfigCategory recipes = builder.getOrCreateCategory(Text.literal("Recipes"));

            // --- Disabled enchantments ---
            recipes.addEntry(eb
                    .startStrList(
                            Text.literal("Disabled Enchantments"),
                            new ArrayList<>(config.disabled_enchantments))
                    .setDefaultValue(new ArrayList<>())
                    .setTooltip(
                            Text.literal("Enchantments to remove from the crafting table."),
                            Text.literal("Example: minecraft:mending"))
                    .setSaveConsumer(list -> config.disabled_enchantments = list)
                    .build());

            // --- Custom recipes ---
            recipes.addEntry(eb
                    .startStrList(
                            Text.literal("Custom Recipes"),
                            new ArrayList<>(config.custom_recipes))
                    .setDefaultValue(new ArrayList<>())
                    .setTooltip(
                            Text.literal("Add custom enchanted book recipes."),
                            Text.literal("Format: enchantment_id|level|ingredient_id"),
                            Text.literal("Example: minecraft:fire_aspect|1|minecraft:flint_and_steel"))
                    .setSaveConsumer(list -> config.custom_recipes = list)
                    .build());

            return builder.build();
        };
    }
}
