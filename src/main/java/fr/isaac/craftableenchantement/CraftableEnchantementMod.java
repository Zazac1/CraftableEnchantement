package fr.isaac.craftableenchantement;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraftableEnchantementMod implements ModInitializer {
    public static final String MOD_ID = "craftableenchantement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("{} initialized", MOD_ID);
    }
}
