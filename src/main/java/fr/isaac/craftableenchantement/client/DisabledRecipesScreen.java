package fr.isaac.craftableenchantement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Shows all 113 built-in enchanted-book recipes (one row per enchantment × level).
 * A toggle button enables / disables each recipe individually via the
 * disabled_enchantments config entry "minecraft:sharpness_5" (exact level)
 * or "minecraft:sharpness" (all levels).
 */
@Environment(EnvType.CLIENT)
public class DisabledRecipesScreen extends Screen {

    // {enchantId, displayName, maxLevel, ingredientId}
    private static final String[][] DATA = {
            // ── Sword ───────────────────────────────────────────────────────
            {"minecraft:sharpness",             "Sharpness",             "5", "minecraft:iron_sword"},
            {"minecraft:smite",                 "Smite",                 "5", "minecraft:bone"},
            {"minecraft:bane_of_arthropods",    "Bane of Arthropods",    "5", "minecraft:spider_eye"},
            {"minecraft:fire_aspect",           "Fire Aspect",           "2", "minecraft:flint_and_steel"},
            {"minecraft:knockback",             "Knockback",             "2", "minecraft:piston"},
            {"minecraft:looting",               "Looting",               "3", "minecraft:gold_ingot"},
            {"minecraft:sweeping_edge",         "Sweeping Edge",         "3", "minecraft:iron_ingot"},
            // ── Armor ────────────────────────────────────────────────────────
            {"minecraft:protection",            "Protection",            "4", "minecraft:iron_chestplate"},
            {"minecraft:fire_protection",       "Fire Protection",       "4", "minecraft:magma_cream"},
            {"minecraft:blast_protection",      "Blast Protection",      "4", "minecraft:tnt"},
            {"minecraft:projectile_protection", "Projectile Protection", "4", "minecraft:arrow"},
            {"minecraft:feather_falling",       "Feather Falling",       "4", "minecraft:feather"},
            {"minecraft:thorns",                "Thorns",                "3", "minecraft:cactus"},
            {"minecraft:respiration",           "Respiration",           "3", "minecraft:sponge"},
            {"minecraft:aqua_affinity",         "Aqua Affinity",         "1", "minecraft:prismarine_shard"},
            {"minecraft:depth_strider",         "Depth Strider",         "3", "minecraft:prismarine_crystals"},
            {"minecraft:frost_walker",          "Frost Walker",          "2", "minecraft:packed_ice"},
            {"minecraft:soul_speed",            "Soul Speed",            "3", "minecraft:soul_sand"},
            {"minecraft:swift_sneak",           "Swift Sneak",           "3", "minecraft:phantom_membrane"},
            // ── Tools ─────────────────────────────────────────────────────────
            {"minecraft:efficiency",            "Efficiency",            "5", "minecraft:golden_pickaxe"},
            {"minecraft:silk_touch",            "Silk Touch",            "1", "minecraft:string"},
            {"minecraft:fortune",               "Fortune",               "3", "minecraft:emerald"},
            {"minecraft:unbreaking",            "Unbreaking",            "3", "minecraft:obsidian"},
            {"minecraft:mending",               "Mending",               "1", "minecraft:nether_star"},
            // ── Bow ───────────────────────────────────────────────────────────
            {"minecraft:power",                 "Power",                 "5", "minecraft:bow"},
            {"minecraft:punch",                 "Punch",                 "2", "minecraft:cobweb"},
            {"minecraft:flame",                 "Flame",                 "1", "minecraft:blaze_rod"},
            {"minecraft:infinity",              "Infinity",              "1", "minecraft:rabbit_foot"},
            // ── Crossbow ──────────────────────────────────────────────────────
            {"minecraft:multishot",             "Multishot",             "1", "minecraft:crossbow"},
            {"minecraft:piercing",              "Piercing",              "4", "minecraft:iron_nugget"},
            {"minecraft:quick_charge",          "Quick Charge",          "3", "minecraft:redstone"},
            // ── Trident ───────────────────────────────────────────────────────
            {"minecraft:impaling",              "Impaling",              "5", "minecraft:prismarine_crystals"},
            {"minecraft:riptide",               "Riptide",               "3", "minecraft:nautilus_shell"},
            {"minecraft:loyalty",               "Loyalty",               "3", "minecraft:tripwire_hook"},
            {"minecraft:channeling",            "Channeling",            "1", "minecraft:lightning_rod"},
            // ── Fishing ───────────────────────────────────────────────────────
            {"minecraft:luck_of_the_sea",       "Luck of the Sea",       "3", "minecraft:tropical_fish"},
            {"minecraft:lure",                  "Lure",                  "3", "minecraft:fishing_rod"},
            // ── Curses ────────────────────────────────────────────────────────
            {"minecraft:binding_curse",         "Curse of Binding",      "1", "minecraft:lead"},
            {"minecraft:vanishing_curse",       "Curse of Vanishing",    "1", "minecraft:ender_pearl"},
    };

    /** Flat list of {enchantId, displayName, level(int), ingredientId} */
    private record RecipeRow(String enchantId, String name, int level, String ingredientId) {
        /** The key stored in disabled_enchantments for this exact level. */
        String key() {
            return enchantId + "_" + level;
        }
        /** Path part used by the mixin (e.g. "sharpness_5"). */
        String path() {
            String base = enchantId.contains(":") ? enchantId.split(":")[1] : enchantId;
            return base + "_" + level;
        }
    }

    private static final RecipeRow[] ROWS;
    static {
        int count = 0;
        for (String[] d : DATA) count += Integer.parseInt(d[2]);
        ROWS = new RecipeRow[count];
        int idx = 0;
        for (String[] d : DATA) {
            int max = Integer.parseInt(d[2]);
            for (int lv = 1; lv <= max; lv++)
                ROWS[idx++] = new RecipeRow(d[0], d[1], lv, d[3]);
        }
    }

    // ── layout ────────────────────────────────────────────────────────────
    private static final int PAD = 8;
    private static final int ROW = 18;
    private static final int ICON_W = 16;

    private final CraftableConfigScreen parent;
    private final List<String> disabled;
    private int scroll = 0;

    public DisabledRecipesScreen(CraftableConfigScreen parent) {
        super(Text.literal("Default Recipes"));
        this.parent   = parent;
        this.disabled = parent.disabled;
    }

    // ── layout helpers ────────────────────────────────────────────────────
    private int listTop()    { return 26; }
    private int listH()      { return height - listTop() - 30; }
    private int maxVisible() { return Math.max(1, listH() / ROW); }
    private int rowY(int i)  { return listTop() + (i - scroll) * ROW; }

    // ── is a specific row disabled? ───────────────────────────────────────
    private boolean isDisabled(RecipeRow row) {
        for (String d : disabled) {
            String dp = d.contains(":") ? d.split(":")[1] : d;
            if (row.path().equals(dp)) return true;          // exact level match
            if (row.path().startsWith(dp + "_")) return true; // whole-enchant prefix
        }
        return false;
    }

    private void toggle(RecipeRow row) {
        // Use the per-level key "minecraft:sharpness_5"
        String key = row.key();
        // Also check for a whole-enchant entry and remove it, then add per-level entries
        // If a full-enchant entry exists, expand it first
        String baseKey = row.enchantId;
        String basePath = baseKey.contains(":") ? baseKey.split(":")[1] : baseKey;
        boolean hadBase = disabled.removeIf(d -> {
            String dp = d.contains(":") ? d.split(":")[1] : d;
            return dp.equals(basePath);
        });
        if (hadBase) {
            // Was fully disabled → enable everything, then disable all except this row
            int max = getMaxLevel(row.enchantId);
            for (int lv = 1; lv <= max; lv++) {
                String lk = row.enchantId + "_" + lv;
                if (lv != row.level) disabled.add(lk); // keep others disabled
            }
            // This row becomes ENABLED (removed from disabled)
        } else {
            // Toggle this specific level
            if (!disabled.remove(key)) disabled.add(key);
        }
        clearAndInit();
    }

    private int getMaxLevel(String enchantId) {
        for (String[] d : DATA) if (d[0].equals(enchantId)) return Integer.parseInt(d[2]);
        return 1;
    }

    // ── init ─────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        // Layer A: background fills
        addDrawable((ctx, mx, my, d) -> renderFills(ctx));

        // Layer B: per-row toggle buttons
        int visible = maxVisible();
        for (int i = scroll; i < Math.min(ROWS.length, scroll + visible); i++) {
            RecipeRow row = ROWS[i];
            boolean dis = isDisabled(row);
            int y = rowY(i);
            final RecipeRow finalRow = row;
            addDrawableChild(ButtonWidget.builder(
                    dis ? Text.literal("Disabled").withColor(0xFF5555)
                        : Text.literal("Enabled").withColor(0x55FF55),
                    b -> toggle(finalRow)
            ).dimensions(width - PAD - 70, y + 1, 70, ROW - 3).build());
        }

        // Back button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"), b -> client.setScreen(parent)
        ).dimensions(width / 2 - 50, height - 24, 100, 18).build());

        // Layer C: text (always last = on top)
        addDrawable((ctx, mx, my, d) -> renderText(ctx));
    }

    // ── fills ─────────────────────────────────────────────────────────────
    private void renderFills(DrawContext ctx) {
        ctx.fill(PAD, listTop(), width - PAD, listTop() + listH(), 0x88101010);
        drawBox(ctx, PAD, listTop(), width - PAD * 2, listH(), 0xFF505050);

        int visible = maxVisible();
        for (int i = scroll; i < Math.min(ROWS.length, scroll + visible); i++) {
            RecipeRow row = ROWS[i];
            int y = rowY(i);
            boolean dis = isDisabled(row);

            // Row background tint
            ctx.fill(PAD + 1, y, width - PAD - 72, y + ROW - 2,
                    dis ? 0x44550000 : 0x22005500);

            // Ingredient icon
            var item = Registries.ITEM.get(Identifier.tryParse(row.ingredientId));
            if (item != null && item != Items.AIR)
                ctx.drawItem(new ItemStack(item), width - PAD - 72 - ICON_W - 4, y + 1);
        }
    }

    // ── text ──────────────────────────────────────────────────────────────
    private void renderText(DrawContext ctx) {
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        int visible = maxVisible();
        for (int i = scroll; i < Math.min(ROWS.length, scroll + visible); i++) {
            RecipeRow row = ROWS[i];
            int y = rowY(i);
            boolean dis = isDisabled(row);
            int col = dis ? 0x888888 : 0xE0E0E0;
            String label = row.name() + " " + toRoman(row.level());
            ctx.drawTextWithShadow(textRenderer, label, PAD + 4, y + 5, col);
        }

        // Scroll hint if needed
        if (ROWS.length > maxVisible()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal((scroll + 1) + "\u2013" + Math.min(scroll + maxVisible(), ROWS.length)
                            + " / " + ROWS.length),
                    width / 2, listTop() + listH() + 4, 0x666666);
        }
    }

    // ── render ────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, Math.min(scroll - (int) v, Math.max(0, ROWS.length - maxVisible())));
        clearAndInit();
        return true;
    }

    // ── helpers ───────────────────────────────────────────────────────────
    private void drawBox(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.drawHorizontalLine(x, x + w - 1, y, c);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, c);
        ctx.drawVerticalLine(x, y, y + h - 1, c);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, c);
    }

    private String toRoman(int n) {
        return switch (n) { case 1->"I"; case 2->"II"; case 3->"III"; case 4->"IV"; case 5->"V"; default->String.valueOf(n); };
    }

    @Override public boolean shouldPause() { return false; }
}
