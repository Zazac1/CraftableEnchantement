package fr.isaac.craftableenchantement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("craftableenchantement.json");

    private static ModConfig cached = null;

    public static ModConfig get() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    /** Forces a reload from disk (called on /reload). */
    public static void invalidate() {
        cached = null;
    }

    private static ModConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            ModConfig defaults = new ModConfig();
            save(defaults);
            CraftableEnchantementMod.LOGGER.info(
                    "[CraftableEnchantement] Config created at: {}", CONFIG_PATH);
            return defaults;
        }
        try {
            String json = Files.readString(CONFIG_PATH);
            ModConfig config = GSON.fromJson(json, ModConfig.class);
            return config != null ? config : new ModConfig();
        } catch (IOException e) {
            CraftableEnchantementMod.LOGGER.error(
                    "[CraftableEnchantement] Failed to read config: {}", e.getMessage());
            return new ModConfig();
        }
    }

    private static void save(ModConfig config) {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            CraftableEnchantementMod.LOGGER.error(
                    "[CraftableEnchantement] Failed to write config: {}", e.getMessage());
        }
    }

    public static void saveAndInvalidate(ModConfig config) {
        save(config);
        invalidate();
    }

    private ConfigLoader() {}
}
