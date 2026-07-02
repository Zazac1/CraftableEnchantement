package fr.isaac.craftableenchantement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class RecipeAddScreen extends Screen {

    // ── vanilla enchantment catalogue ─────────────────────────────────────
    private static final String[][] ENCHANTMENTS = {
            {"minecraft:sharpness",            "Sharpness",            "5"},
            {"minecraft:smite",                "Smite",                "5"},
            {"minecraft:bane_of_arthropods",   "Bane of Arthropods",   "5"},
            {"minecraft:fire_aspect",          "Fire Aspect",          "2"},
            {"minecraft:knockback",            "Knockback",            "2"},
            {"minecraft:looting",              "Looting",              "3"},
            {"minecraft:sweeping_edge",        "Sweeping Edge",        "3"},
            {"minecraft:protection",           "Protection",           "4"},
            {"minecraft:fire_protection",      "Fire Protection",      "4"},
            {"minecraft:blast_protection",     "Blast Protection",     "4"},
            {"minecraft:projectile_protection","Projectile Protection","4"},
            {"minecraft:feather_falling",      "Feather Falling",      "4"},
            {"minecraft:thorns",               "Thorns",               "3"},
            {"minecraft:respiration",          "Respiration",          "3"},
            {"minecraft:aqua_affinity",        "Aqua Affinity",        "1"},
            {"minecraft:depth_strider",        "Depth Strider",        "3"},
            {"minecraft:frost_walker",         "Frost Walker",         "2"},
            {"minecraft:soul_speed",           "Soul Speed",           "3"},
            {"minecraft:swift_sneak",          "Swift Sneak",          "3"},
            {"minecraft:efficiency",           "Efficiency",           "5"},
            {"minecraft:silk_touch",           "Silk Touch",           "1"},
            {"minecraft:fortune",              "Fortune",              "3"},
            {"minecraft:unbreaking",           "Unbreaking",           "3"},
            {"minecraft:mending",              "Mending",              "1"},
            {"minecraft:power",                "Power",                "5"},
            {"minecraft:punch",                "Punch",                "2"},
            {"minecraft:flame",                "Flame",                "1"},
            {"minecraft:infinity",             "Infinity",             "1"},
            {"minecraft:multishot",            "Multishot",            "1"},
            {"minecraft:piercing",             "Piercing",             "4"},
            {"minecraft:quick_charge",         "Quick Charge",         "3"},
            {"minecraft:impaling",             "Impaling",             "5"},
            {"minecraft:riptide",              "Riptide",              "3"},
            {"minecraft:loyalty",              "Loyalty",              "3"},
            {"minecraft:channeling",           "Channeling",           "1"},
            {"minecraft:luck_of_the_sea",      "Luck of the Sea",      "3"},
            {"minecraft:lure",                 "Lure",                 "3"},
            {"minecraft:binding_curse",        "Curse of Binding",     "1"},
            {"minecraft:vanishing_curse",      "Curse of Vanishing",   "1"},
    };

    // ── constants ─────────────────────────────────────────────────────────
    private static final int PAD    = 8;
    private static final int SLOT   = 20;   // crafting-table slot size (px)
    private static final int SUGG_H = 13;   // autocomplete row height
    private static final int MAX_S  = 7;    // max suggestions shown

    // ── state ─────────────────────────────────────────────────────────────
    private final CraftableConfigScreen parent;

    // Enchantment
    private String         selectedEnchantId   = "";
    private String         selectedEnchantName = "";
    private int            enchantMaxLevel     = 5;
    private List<String[]> enchantSugg         = new ArrayList<>();

    // Ingredient
    private String       selectedItemId = "";
    private List<String> itemSugg       = new ArrayList<>();

    private int level = 1;

    // Widgets
    private TextFieldWidget enchantField;
    private TextFieldWidget itemField;

    // ── constructor ───────────────────────────────────────────────────────
    public RecipeAddScreen(CraftableConfigScreen parent) {
        super(Text.literal("Add Custom Recipe"));
        this.parent = parent;
    }

    // ── layout ────────────────────────────────────────────────────────────

    private int leftX()   { return PAD; }
    private int leftW()   { return width / 2 - PAD - 4; }
    private int rightX()  { return width / 2 + 4; }
    private int rightW()  { return width - PAD - rightX(); }

    private int enchLabelY() { return 22; }
    private int enchFieldY() { return enchLabelY() + 10; }
    private int enchSuggY()  { return enchFieldY() + 14; }

    private int itemLabelY() { return enchSuggY() + MAX_S * SUGG_H + 8; }
    private int itemFieldY() { return itemLabelY() + 10; }
    private int itemSuggY()  { return itemFieldY() + 14; }

    private int gridX()  { return rightX() + (rightW() - 3 * SLOT) / 2; }
    private int gridY()  { return 28; }

    // ── init ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        enchantField = addDrawableChild(new TextFieldWidget(
                textRenderer, leftX(), enchFieldY(), leftW(), 14,
                Text.literal("enchant")));
        enchantField.setPlaceholder(Text.literal("Type enchantment name\u2026"));
        enchantField.setMaxLength(80);
        enchantField.setChangedListener(this::onEnchantTyped);

        itemField = addDrawableChild(new TextFieldWidget(
                textRenderer, leftX(), itemFieldY(), leftW(), 14,
                Text.literal("item")));
        itemField.setPlaceholder(Text.literal("Type item id (flint_and_steel\u2026)"));
        itemField.setMaxLength(80);
        itemField.setChangedListener(this::onItemTyped);

        // Level  -  placed below the grid on the right side
        int lvlY = gridY() + 3 * SLOT + 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("\u2212"),
                b -> { if (level > 1) level--; }
        ).dimensions(rightX() + 4, lvlY, 16, 16).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                b -> { if (level < enchantMaxLevel) level++; }
        ).dimensions(rightX() + rightW() - 20, lvlY, 16, 16).build());

        // Cancel / Add
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 102, height - 22, 98, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("\u2713  Add Recipe"),
                b -> confirm()
        ).dimensions(width / 2 + 4, height - 22, 98, 18).build());
    }

    // ── autocomplete logic ────────────────────────────────────────────────

    private void onEnchantTyped(String q) {
        if (q.isBlank()) { enchantSugg = new ArrayList<>(); return; }
        String ql = q.toLowerCase(Locale.ROOT);
        enchantSugg = new ArrayList<>();
        for (String[] e : ENCHANTMENTS) {
            if (e[1].toLowerCase(Locale.ROOT).contains(ql) || e[0].contains(ql)) {
                enchantSugg.add(e);
                if (enchantSugg.size() >= MAX_S) break;
            }
        }
    }

    private void onItemTyped(String q) {
        if (q.isBlank()) { itemSugg = new ArrayList<>(); return; }
        String ql = q.toLowerCase(Locale.ROOT);
        itemSugg = new ArrayList<>();
        for (var entry : Registries.ITEM.getEntrySet()) {
            if (entry.getValue() == Items.AIR) continue;
            String id   = entry.getKey().getValue().toString();
            String path = entry.getKey().getValue().getPath();
            if (id.contains(ql) || path.contains(ql)) {
                itemSugg.add(id);
                if (itemSugg.size() >= MAX_S) break;
            }
        }
    }

    private void selectEnchant(String[] e) {
        selectedEnchantId   = e[0];
        selectedEnchantName = e[1];
        enchantMaxLevel     = Integer.parseInt(e[2]);
        level               = Math.min(level, enchantMaxLevel);
        enchantField.setText(e[1]);
        enchantSugg         = new ArrayList<>();
    }

    private void selectItem(String id) {
        selectedItemId = id;
        String path = id.contains(":") ? id.split(":")[1] : id;
        itemField.setText(path);
        itemSugg = new ArrayList<>();
    }

    // ── render ────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        // === Phase 1 : fills (behind widgets) ================================
        ctx.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        // Crafting grid slots (3×3)
        int gx0 = gridX(), gy0 = gridY();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int sx = gx0 + c * SLOT, sy = gy0 + r * SLOT;
                ctx.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0xFF3A3A3A);
                drawBox(ctx, sx, sy, SLOT, SLOT, 0xFF555555);
            }
        }
        // Items in slots
        for (int i = 0; i < 9; i++) {
            ItemStack stack = slotItem(i);
            if (!stack.isEmpty())
                ctx.drawItem(stack, gx0 + (i % 3) * SLOT + 2, gy0 + (i / 3) * SLOT + 2);
        }
        // Result slot
        int resX = gx0 + 3 * SLOT + 14, resY = gy0 + SLOT;
        ctx.fill(resX + 1, resY + 1, resX + SLOT - 1, resY + SLOT - 1, 0xFF3A3A3A);
        drawBox(ctx, resX, resY, SLOT, SLOT, 0xFF908830);
        ctx.drawItem(new ItemStack(Items.ENCHANTED_BOOK), resX + 2, resY + 2);

        // === Phase 2 : widgets ===============================================
        super.render(ctx, mouseX, mouseY, delta);

        // === Phase 3 : text + suggestion overlays ============================
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        // Input labels
        ctx.drawTextWithShadow(textRenderer, "Enchantment:",
                leftX(), enchLabelY(), 0xCCCCCC);
        ctx.drawTextWithShadow(textRenderer, "Ingredient:",
                leftX(), itemLabelY(), 0xCCCCCC);

        // Arrow
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u2192"),
                gx0 + 3 * SLOT + 7, gy0 + SLOT + 6, 0xFFFFFF);

        // Level label
        int lvlY = gridY() + 3 * SLOT + 6;
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Level ")
                        .append(Text.literal(toRoman(level)).withColor(0xFFD700))
                        .append(Text.literal("/" + toRoman(enchantMaxLevel)).withColor(0x777777)),
                rightX() + rightW() / 2, lvlY + 3, 0xFFFFFF);

        // Preview
        String en = selectedEnchantName.isEmpty() ? "?" : selectedEnchantName;
        String it = selectedItemId.isEmpty() ? "?"
                : (selectedItemId.contains(":") ? selectedItemId.split(":")[1] : selectedItemId)
                .replace("_", " ");
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(level + "\u00d7 XP + Book + " + it + "  \u2192  " + en + " " + toRoman(level)),
                width / 2, height - 38, 0xAAAAAA);

        // Enchantment suggestions overlay
        if (!enchantSugg.isEmpty()) {
            int sy = enchSuggY(), sw = leftW();
            int sh = enchantSugg.size() * SUGG_H;
            ctx.fill(leftX(), sy, leftX() + sw, sy + sh, 0xFF1A1C28);
            drawBox(ctx, leftX(), sy, sw, sh, 0xFF4A5578);
            for (int i = 0; i < enchantSugg.size(); i++) {
                String[] e = enchantSugg.get(i);
                int ry = sy + i * SUGG_H;
                boolean hov = mouseX >= leftX() && mouseX < leftX() + sw
                        && mouseY >= ry && mouseY < ry + SUGG_H;
                if (hov) ctx.fill(leftX() + 1, ry, leftX() + sw - 1, ry + SUGG_H, 0x553355BB);
                ctx.drawTextWithShadow(textRenderer,
                        e[1] + " \u00a77(max " + e[2] + ")",
                        leftX() + 4, ry + 3, hov ? 0xFFFFFF : 0xBBBBBB);
            }
        }

        // Item suggestions overlay
        if (!itemSugg.isEmpty()) {
            int sy = itemSuggY(), sw = leftW();
            int sh = itemSugg.size() * SUGG_H;
            ctx.fill(leftX(), sy, leftX() + sw, sy + sh, 0xFF1A1C28);
            drawBox(ctx, leftX(), sy, sw, sh, 0xFF4A5578);
            for (int i = 0; i < itemSugg.size(); i++) {
                String id = itemSugg.get(i);
                int ry = sy + i * SUGG_H;
                boolean hov = mouseX >= leftX() && mouseX < leftX() + sw
                        && mouseY >= ry && mouseY < ry + SUGG_H;
                if (hov) ctx.fill(leftX() + 1, ry, leftX() + sw - 1, ry + SUGG_H, 0x553355BB);
                var item = Registries.ITEM.get(Identifier.of(id));
                if (item != null && item != Items.AIR)
                    ctx.drawItem(new ItemStack(item), leftX() + 2, ry);
                String path = id.contains(":") ? id.split(":")[1] : id;
                ctx.drawTextWithShadow(textRenderer,
                        path.replace("_", " ") + " \u00a78" + id,
                        leftX() + 20, ry + 3, hov ? 0xFFFFFF : 0xBBBBBB);
            }
        }

        // Ingredient slot tooltip
        if (!selectedItemId.isEmpty()) {
            int s = level + 1;
            if (s < 9) {
                int sx = gx0 + (s % 3) * SLOT + 2, sy = gy0 + (s / 3) * SLOT + 2;
                if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                    var item = Registries.ITEM.get(Identifier.tryParse(selectedItemId));
                    if (item != null && item != Items.AIR)
                        ctx.drawItemTooltip(textRenderer, new ItemStack(item), mouseX, mouseY);
                }
            }
        }
    }

    // ── slot content ──────────────────────────────────────────────────────

    private ItemStack slotItem(int slot) {
        if (slot < level) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        if (slot == level) return new ItemStack(Items.BOOK);
        if (slot == level + 1 && !selectedItemId.isEmpty()) {
            var item = Registries.ITEM.get(Identifier.tryParse(selectedItemId));
            if (item != null && item != Items.AIR) return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    // ── mouse ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean focused) {
        double mx = click.x(), my = click.y();

        if (!enchantSugg.isEmpty()) {
            int sy = enchSuggY(), sw = leftW();
            if (mx >= leftX() && mx < leftX() + sw
                    && my >= sy && my < sy + enchantSugg.size() * SUGG_H) {
                int idx = (int) (my - sy) / SUGG_H;
                if (idx >= 0 && idx < enchantSugg.size()) {
                    selectEnchant(enchantSugg.get(idx));
                    return true;
                }
            }
        }

        if (!itemSugg.isEmpty()) {
            int sy = itemSuggY(), sw = leftW();
            if (mx >= leftX() && mx < leftX() + sw
                    && my >= sy && my < sy + itemSugg.size() * SUGG_H) {
                int idx = (int) (my - sy) / SUGG_H;
                if (idx >= 0 && idx < itemSugg.size()) {
                    selectItem(itemSugg.get(idx));
                    return true;
                }
            }
        }

        return super.mouseClicked(click, focused);
    }

    // ── keyboard ──────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyInput k) {
        if (k.key() == 256) { client.setScreen(parent); return true; }
        return super.keyPressed(k);
    }

    // ── confirm ───────────────────────────────────────────────────────────

    private void confirm() {
        if (selectedEnchantId.isEmpty() || selectedItemId.isEmpty()) return;
        String entry = selectedEnchantId + "|" + level + "|" + selectedItemId;
        if (!parent.recipes.contains(entry)) parent.recipes.add(entry);
        client.setScreen(parent);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void drawBox(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.drawHorizontalLine(x, x + w - 1, y, c);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, c);
        ctx.drawVerticalLine(x, y, y + h - 1, c);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, c);
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n);
        };
    }

    @Override
    public boolean shouldPause() { return false; }
}
