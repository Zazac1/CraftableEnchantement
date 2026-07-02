package fr.isaac.craftableenchantement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class DisabledRecipesScreen extends Screen {

    private static final String[][] DATA = {
            {"minecraft:sharpness",             "Sharpness",             "5", "minecraft:iron_sword"},
            {"minecraft:smite",                 "Smite",                 "5", "minecraft:bone"},
            {"minecraft:bane_of_arthropods",    "Bane of Arthropods",    "5", "minecraft:spider_eye"},
            {"minecraft:fire_aspect",           "Fire Aspect",           "2", "minecraft:flint_and_steel"},
            {"minecraft:knockback",             "Knockback",             "2", "minecraft:piston"},
            {"minecraft:looting",               "Looting",               "3", "minecraft:gold_ingot"},
            {"minecraft:sweeping_edge",         "Sweeping Edge",         "3", "minecraft:iron_ingot"},
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
            {"minecraft:efficiency",            "Efficiency",            "5", "minecraft:golden_pickaxe"},
            {"minecraft:silk_touch",            "Silk Touch",            "1", "minecraft:string"},
            {"minecraft:fortune",               "Fortune",               "3", "minecraft:emerald"},
            {"minecraft:unbreaking",            "Unbreaking",            "3", "minecraft:obsidian"},
            {"minecraft:mending",               "Mending",               "1", "minecraft:nether_star"},
            {"minecraft:power",                 "Power",                 "5", "minecraft:bow"},
            {"minecraft:punch",                 "Punch",                 "2", "minecraft:cobweb"},
            {"minecraft:flame",                 "Flame",                 "1", "minecraft:blaze_rod"},
            {"minecraft:infinity",              "Infinity",              "1", "minecraft:rabbit_foot"},
            {"minecraft:multishot",             "Multishot",             "1", "minecraft:crossbow"},
            {"minecraft:piercing",              "Piercing",              "4", "minecraft:iron_nugget"},
            {"minecraft:quick_charge",          "Quick Charge",          "3", "minecraft:redstone"},
            {"minecraft:impaling",              "Impaling",              "5", "minecraft:prismarine_crystals"},
            {"minecraft:riptide",               "Riptide",               "3", "minecraft:nautilus_shell"},
            {"minecraft:loyalty",               "Loyalty",               "3", "minecraft:tripwire_hook"},
            {"minecraft:channeling",            "Channeling",            "1", "minecraft:lightning_rod"},
            {"minecraft:luck_of_the_sea",       "Luck of the Sea",       "3", "minecraft:tropical_fish"},
            {"minecraft:lure",                  "Lure",                  "3", "minecraft:fishing_rod"},
            {"minecraft:binding_curse",         "Curse of Binding",      "1", "minecraft:lead"},
            {"minecraft:vanishing_curse",       "Curse of Vanishing",    "1", "minecraft:ender_pearl"},
    };

    private record RecipeRow(String enchantId, String name, int level, String ingredientId) {
        String key()  { return enchantId + "_" + level; }
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

    private static final int PAD = 8, ROW = 18;
    private final CraftableConfigScreen parent;
    private final List<String> disabled;
    private int scroll = 0;

    public DisabledRecipesScreen(CraftableConfigScreen parent) {
        super(Text.literal("Default Recipes"));
        this.parent   = parent;
        this.disabled = parent.disabled;
    }

    private int listTop()    { return 26; }
    private int listH()      { return height - listTop() - 30; }
    private int maxVisible() { return Math.max(1, listH() / ROW); }
    private int rowY(int i)  { return listTop() + (i - scroll) * ROW; }

    private boolean isDisabled(RecipeRow row) {
        for (String d : disabled) {
            String dp = d.contains(":") ? d.split(":")[1] : d;
            if (row.path().equals(dp)) return true;
            if (row.path().startsWith(dp + "_")) return true;
        }
        return false;
    }

    private void toggle(RecipeRow row) {
        String key      = row.key();
        String baseKey  = row.enchantId;
        String basePath = baseKey.contains(":") ? baseKey.split(":")[1] : baseKey;
        boolean hadBase = disabled.removeIf(d -> (d.contains(":") ? d.split(":")[1] : d).equals(basePath));
        if (hadBase) {
            int max = getMaxLevel(row.enchantId);
            for (int lv = 1; lv <= max; lv++)
                if (lv != row.level) disabled.add(row.enchantId + "_" + lv);
        } else {
            if (!disabled.remove(key)) disabled.add(key);
        }
        clearAndInit();
    }

    private int getMaxLevel(String enchantId) {
        for (String[] d : DATA) if (d[0].equals(enchantId)) return Integer.parseInt(d[2]);
        return 1;
    }

    @Override
    protected void init() {
        // Background fills
        addDrawable((ctx, mx, my, d) -> {
            ctx.fill(PAD, listTop(), width - PAD, listTop() + listH(), 0x88101010);
            drawBox(ctx, PAD, listTop(), width - PAD * 2, listH(), 0xFF505050);
            int visible = maxVisible();
            for (int i = scroll; i < Math.min(ROWS.length, scroll + visible); i++) {
                RecipeRow row = ROWS[i];
                int y = rowY(i);
                boolean dis = isDisabled(row);
                ctx.fill(PAD + 1, y, width - PAD - 72, y + ROW - 2, dis ? 0x44550000 : 0x22005500);
                // Ingredient icon
                var item = Registries.ITEM.get(Identifier.tryParse(row.ingredientId()));
                if (item != null && item != Items.AIR)
                    ctx.drawItem(new ItemStack(item), width - PAD - 72 - 18, y + 1);
            }
        });

        // Title
        MultilineTextWidget titleW = new MultilineTextWidget(width / 2, 10, title, textRenderer);
        titleW.setMaxWidth(width - 20);
        titleW.setCentered(true);
        addDrawableChild(titleW);

        // Per-row: label widget + toggle button
        int visible = maxVisible();
        for (int i = scroll; i < Math.min(ROWS.length, scroll + visible); i++) {
            RecipeRow row = ROWS[i];
            boolean dis = isDisabled(row);
            int y = rowY(i);

            // Enchantment name+level as MultilineTextWidget
            String label = row.name() + " " + toRoman(row.level());
            MultilineTextWidget lbl = new MultilineTextWidget(
                    PAD + 4, y + (ROW - 8) / 2,
                    Text.literal(label).withColor(dis ? 0x888888 : 0xE0E0E0),
                    textRenderer);
            lbl.setMaxWidth(width - PAD * 2 - 100);
            lbl.setMaxRows(1);
            addDrawableChild(lbl);

            // Toggle button
            final RecipeRow finalRow = row;
            addDrawableChild(ButtonWidget.builder(
                    dis ? Text.literal("Disabled").withColor(0xFF5555)
                        : Text.literal("Enabled").withColor(0x55FF55),
                    b -> toggle(finalRow)
            ).dimensions(width - PAD - 70, y + 1, 70, ROW - 3).build());
        }

        // Scroll hint
        if (ROWS.length > maxVisible()) {
            MultilineTextWidget hint = new MultilineTextWidget(
                    width / 2, listTop() + listH() + 4,
                    Text.literal((scroll + 1) + "–" + Math.min(scroll + maxVisible(), ROWS.length) + " / " + ROWS.length).withColor(0x666666),
                    textRenderer);
            hint.setMaxWidth(200);
            hint.setCentered(true);
            addDrawableChild(hint);
        }

        // Back button
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 50, height - 24, 100, 18).build());
    }

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
