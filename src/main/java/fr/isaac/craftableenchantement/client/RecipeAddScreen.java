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

/**
 * Add-recipe screen.
 *
 * Rendering is split into three addDrawable() layers so all text and fills
 * go through Minecraft's own rendering pipeline — the only way to make text
 * visible in MC 1.21.11.
 *
 *  Layer A (first addDrawable): slot fills + grid items + slider track
 *  Layer B (addDrawableChild):  TextFieldWidgets + Buttons
 *  Layer C (last addDrawable):  all labels, suggestions overlays
 */
@Environment(EnvType.CLIENT)
public class RecipeAddScreen extends Screen {

    // ── enchantment catalogue ─────────────────────────────────────────────
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
    private static final int SLOT   = 20;
    private static final int SUGG_H = 14;
    private static final int MAX_S  = 6;

    // ── state ─────────────────────────────────────────────────────────────
    private final CraftableConfigScreen parent;

    private String selectedEnchantId   = "";
    private String selectedEnchantName = "";
    private int    enchantMaxLevel     = 1;
    private int    minLevel            = 1;
    private int    maxLevel            = 1;
    private int    draggingHandle      = 0; // 0=none 1=min 2=max

    private String selectedItemId   = "";
    private String selectedItemName = "";

    private List<String[]> enchantSugg = new ArrayList<>();
    private List<String[]> itemSugg    = new ArrayList<>();

    private TextFieldWidget enchantField;
    private TextFieldWidget itemField;

    // ── constructor ───────────────────────────────────────────────────────
    public RecipeAddScreen(CraftableConfigScreen parent) {
        super(Text.literal("Add Custom Recipe"));
        this.parent = parent;
    }

    // ── layout ────────────────────────────────────────────────────────────
    private int leftX()  { return PAD; }
    private int leftW()  { return width / 2 - PAD - 6; }
    private int rightX() { return width / 2 + 4; }
    private int rightW() { return width - PAD - rightX(); }

    private int gridX()        { return rightX() + (rightW() - 3 * SLOT) / 2; }
    private int gridY()        { return 22; }
    private int sliderX()      { return rightX() + 4; }
    private int sliderW()      { return rightW() - 8; }
    private int sliderY()      { return gridY() + 3 * SLOT + 10; }
    private int sliderH()      { return 6; }

    private int enchLabelY()   { return 22; }
    private int enchFieldY()   { return enchLabelY() + 10; }
    private int enchSuggY()    { return enchFieldY() + 14; }

    private int itemLabelY()   { return enchSuggY() + MAX_S * SUGG_H + 10; }
    private int itemFieldY()   { return itemLabelY() + 10; }
    private int itemSuggY()    { return itemFieldY() + 14; }

    private int handleX(int lv) {
        if (enchantMaxLevel <= 1) return sliderX();
        float t = (float)(lv - 1) / (enchantMaxLevel - 1);
        return sliderX() + (int)(t * (sliderW() - 6));
    }
    private int levelFromX(double x) {
        if (enchantMaxLevel <= 1) return 1;
        float t = Math.max(0, Math.min(1, (float)(x - sliderX() - 3) / (sliderW() - 6)));
        return 1 + Math.round(t * (enchantMaxLevel - 1));
    }

    // ── init ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        // ── Layer A : fills + grid (before widgets, so they're behind) ────
        addDrawable((ctx, mx, my, d) -> renderLayerA(ctx, mx, my));

        // ── Layer B : widgets ─────────────────────────────────────────────
        enchantField = addDrawableChild(new TextFieldWidget(
                textRenderer, leftX(), enchFieldY(), leftW(), 14, Text.literal("e")));
        enchantField.setPlaceholder(Text.literal("Type enchantment name\u2026"));
        enchantField.setMaxLength(80);
        enchantField.setChangedListener(this::onEnchantTyped);

        itemField = addDrawableChild(new TextFieldWidget(
                textRenderer, leftX(), itemFieldY(), leftW(), 14, Text.literal("i")));
        itemField.setPlaceholder(Text.literal("Type item id (flint_and_steel\u2026)"));
        itemField.setMaxLength(80);
        itemField.setChangedListener(this::onItemTyped);

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 102, height - 22, 98, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("\u2713  Add Recipe"),
                b -> confirm()
        ).dimensions(width / 2 + 4, height - 22, 98, 18).build());

        // ── Layer C : text + suggestions (last = on top of everything) ────
        addDrawable((ctx, mx, my, d) -> renderLayerC(ctx, mx, my));
    }

    // ── autocomplete ──────────────────────────────────────────────────────

    private void onEnchantTyped(String q) {
        if (q.isBlank()) { enchantSugg = new ArrayList<>(); return; }
        String ql = q.toLowerCase(Locale.ROOT);
        enchantSugg = new ArrayList<>();
        for (String[] e : ENCHANTMENTS)
            if (e[1].toLowerCase(Locale.ROOT).contains(ql) || e[0].contains(ql)) {
                enchantSugg.add(e);
                if (enchantSugg.size() >= MAX_S) break;
            }
    }

    private void onItemTyped(String q) {
        if (q.isBlank()) { itemSugg = new ArrayList<>(); return; }
        String ql = q.toLowerCase(Locale.ROOT);
        itemSugg = new ArrayList<>();
        for (var entry : Registries.ITEM.getEntrySet()) {
            if (entry.getValue() == Items.AIR) continue;
            String id = entry.getKey().getValue().toString();
            if (id.contains(ql) || entry.getKey().getValue().getPath().contains(ql)) {
                itemSugg.add(new String[]{id, pathToName(id)});
                if (itemSugg.size() >= MAX_S) break;
            }
        }
    }

    private void selectEnchant(String[] e) {
        selectedEnchantId = e[0]; selectedEnchantName = e[1];
        enchantMaxLevel   = Integer.parseInt(e[2]);
        minLevel = 1; maxLevel = enchantMaxLevel;
        enchantField.setText(e[1]);
        enchantSugg = new ArrayList<>();
    }

    private void selectItem(String[] s) {
        selectedItemId = s[0]; selectedItemName = s[1];
        itemField.setText(s[1].toLowerCase(Locale.ROOT).replace(" ", "_"));
        itemSugg = new ArrayList<>();
    }

    // ── Layer A render (fills + grid items + slider) ───────────────────────

    private void renderLayerA(DrawContext ctx, int mx, int my) {
        // Crafting grid slots + items
        int gx0 = gridX(), gy0 = gridY();
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            int sx = gx0 + c * SLOT, sy = gy0 + r * SLOT;
            ctx.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0xFF3A3A3A);
            drawBox(ctx, sx, sy, SLOT, SLOT, 0xFF555555);
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stk = slotItem(i);
            if (!stk.isEmpty())
                ctx.drawItem(stk, gx0 + (i % 3) * SLOT + 2, gy0 + (i / 3) * SLOT + 2);
        }
        // Result slot
        int resX = gx0 + 3 * SLOT + 12, resY = gy0 + SLOT;
        ctx.fill(resX + 1, resY + 1, resX + SLOT - 1, resY + SLOT - 1, 0xFF3A3A3A);
        drawBox(ctx, resX, resY, SLOT, SLOT, 0xFF908830);
        ctx.drawItem(new ItemStack(Items.ENCHANTED_BOOK), resX + 2, resY + 2);

        // Range slider track
        if (enchantMaxLevel > 1) {
            int ty = sliderY();
            ctx.fill(sliderX(), ty, sliderX() + sliderW(), ty + sliderH(), 0xFF404040);
            ctx.fill(handleX(minLevel) + 3, ty, handleX(maxLevel) + 3, ty + sliderH(), 0xFF4477DD);
            for (int lv = 1; lv <= enchantMaxLevel; lv++) {
                int tx = handleX(lv) + 3;
                ctx.fill(tx - 1, ty - 2, tx + 1, ty, 0xFF888888);
            }
            // Min handle
            int mhx = handleX(minLevel);
            ctx.fill(mhx, ty - 2, mhx + 6, ty + sliderH() + 2,
                    (Math.abs(mx - mhx - 3) <= 6 || draggingHandle == 1) ? 0xFFDDDDDD : 0xFF999999);
            drawBox(ctx, mhx, ty - 2, 6, sliderH() + 4, 0xFF444444);
            // Max handle
            int Mhx = handleX(maxLevel);
            ctx.fill(Mhx, ty - 2, Mhx + 6, ty + sliderH() + 2,
                    (Math.abs(mx - Mhx - 3) <= 6 || draggingHandle == 2) ? 0xFFDDDDDD : 0xFF999999);
            drawBox(ctx, Mhx, ty - 2, 6, sliderH() + 4, 0xFF444444);
        }

        // Selected item preview bar
        if (!selectedItemId.isEmpty()) {
            int py = previewY();
            ctx.fill(leftX(), py, leftX() + leftW(), py + 20, 0x88203020);
            drawBox(ctx, leftX(), py, leftW(), 20, 0xFF447744);
            var item = Registries.ITEM.get(Identifier.tryParse(selectedItemId));
            if (item != null && item != Items.AIR)
                ctx.drawItem(new ItemStack(item), leftX() + 2, py + 2);
        }
    }

    private int previewY() {
        return Math.min(itemSuggY() + (itemSugg.isEmpty() ? 2 : itemSugg.size() * SUGG_H + 4),
                height - 60);
    }

    // ── Layer C render (all text + suggestions) ────────────────────────────

    private void renderLayerC(DrawContext ctx, int mx, int my) {
        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        // Section labels
        ctx.drawTextWithShadow(textRenderer, "Enchantment:", leftX(), enchLabelY(), 0xCCCCCC);
        ctx.drawTextWithShadow(textRenderer, "Ingredient:",  leftX(), itemLabelY(), 0xCCCCCC);

        // Arrow →
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u2192"),
                gridX() + 3 * SLOT + 6, gridY() + SLOT + 5, 0xFFFFFF);

        // Level label
        String lvlTxt = enchantMaxLevel <= 1
                ? "Level I"
                : (minLevel == maxLevel ? "Level " + toRoman(minLevel)
                : "Level " + toRoman(minLevel) + " \u2013 " + toRoman(maxLevel));
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(lvlTxt).withColor(0xFFD700),
                sliderX() + sliderW() / 2,
                sliderY() + sliderH() + 5, 0xFFFFFF);

        // Selected item name in preview box
        if (!selectedItemId.isEmpty()) {
            int py = previewY();
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(selectedItemName).withColor(0x88FF88)
                            .append(Text.literal("  \u00a78" + selectedItemId)),
                    leftX() + 22, py + 6, 0xCCCCCC);
        }

        // Preview line
        String en = selectedEnchantName.isEmpty() ? "?" : selectedEnchantName;
        String it = selectedItemName.isEmpty() ? "?" : selectedItemName.toLowerCase(Locale.ROOT);
        String lvPart = (enchantMaxLevel > 1 && minLevel != maxLevel)
                ? toRoman(minLevel) + "\u2013" + toRoman(maxLevel) : toRoman(minLevel);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(minLevel + "\u2013" + maxLevel
                        + "\u00d7 XP + Book + " + it + "  \u2192  " + en + " " + lvPart),
                width / 2, height - 36, 0x999999);

        // ── Enchantment suggestions overlay ──────────────────────────────
        if (!enchantSugg.isEmpty()) {
            int sy = enchSuggY(), sw = leftW(), sh = enchantSugg.size() * SUGG_H;
            ctx.fill(leftX(), sy, leftX() + sw, sy + sh, 0xFF1A1C28);
            drawBox(ctx, leftX(), sy, sw, sh, 0xFF4A5578);
            for (int i = 0; i < enchantSugg.size(); i++) {
                String[] e = enchantSugg.get(i);
                int ry = sy + i * SUGG_H;
                boolean hov = mx >= leftX() && mx < leftX() + sw && my >= ry && my < ry + SUGG_H;
                if (hov) ctx.fill(leftX() + 1, ry, leftX() + sw - 1, ry + SUGG_H, 0x553355BB);
                ctx.drawTextWithShadow(textRenderer,
                        e[1] + " \u00a77(max " + e[2] + ")",
                        leftX() + 4, ry + 3, hov ? 0xFFFFFF : 0xBBBBBB);
            }
        }

        // ── Item suggestions overlay ──────────────────────────────────────
        if (!itemSugg.isEmpty()) {
            int sy = itemSuggY(), sw = leftW(), sh = itemSugg.size() * SUGG_H;
            ctx.fill(leftX(), sy, leftX() + sw, sy + sh, 0xFF1A1C28);
            drawBox(ctx, leftX(), sy, sw, sh, 0xFF4A5578);
            for (int i = 0; i < itemSugg.size(); i++) {
                String[] s = itemSugg.get(i);
                int ry = sy + i * SUGG_H;
                boolean hov = mx >= leftX() && mx < leftX() + sw && my >= ry && my < ry + SUGG_H;
                if (hov) ctx.fill(leftX() + 1, ry, leftX() + sw - 1, ry + SUGG_H, 0x553355BB);
                var item = Registries.ITEM.get(Identifier.tryParse(s[0]));
                if (item != null && item != Items.AIR)
                    ctx.drawItem(new ItemStack(item), leftX() + 2, ry);
                ctx.drawTextWithShadow(textRenderer,
                        s[1] + "  \u00a78" + s[0],
                        leftX() + 20, ry + 3, hov ? 0xFFFFFF : 0xCCCCCC);
            }
        }
    }

    // ── slot item ─────────────────────────────────────────────────────────
    private ItemStack slotItem(int slot) {
        if (slot < maxLevel) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        if (slot == maxLevel) return new ItemStack(Items.BOOK);
        if (slot == maxLevel + 1 && !selectedItemId.isEmpty()) {
            var item = Registries.ITEM.get(Identifier.tryParse(selectedItemId));
            if (item != null && item != Items.AIR) return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    // ── render (minimal — just gradient + pipeline call) ──────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── mouse ─────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(Click click, boolean focused) {
        double mx = click.x(), my = click.y();

        // Slider interaction
        if (enchantMaxLevel > 1) {
            int ty = sliderY();
            if (my >= ty - 4 && my <= ty + sliderH() + 6
                    && mx >= sliderX() && mx <= sliderX() + sliderW()) {
                int dMin = Math.abs((int)mx - (handleX(minLevel) + 3));
                int dMax = Math.abs((int)mx - (handleX(maxLevel) + 3));
                if (dMin <= dMax) {
                    draggingHandle = 1; minLevel = levelFromX(mx);
                    if (minLevel > maxLevel) maxLevel = minLevel;
                } else {
                    draggingHandle = 2; maxLevel = levelFromX(mx);
                    if (maxLevel < minLevel) minLevel = maxLevel;
                }
                return true;
            }
        }

        // Enchantment suggestion
        if (!enchantSugg.isEmpty()) {
            int sy = enchSuggY(), sw = leftW();
            if (mx >= leftX() && mx < leftX() + sw && my >= sy
                    && my < sy + enchantSugg.size() * SUGG_H) {
                int idx = (int)(my - sy) / SUGG_H;
                if (idx >= 0 && idx < enchantSugg.size()) { selectEnchant(enchantSugg.get(idx)); return true; }
            }
        }

        // Item suggestion
        if (!itemSugg.isEmpty()) {
            int sy = itemSuggY(), sw = leftW();
            if (mx >= leftX() && mx < leftX() + sw && my >= sy
                    && my < sy + itemSugg.size() * SUGG_H) {
                int idx = (int)(my - sy) / SUGG_H;
                if (idx >= 0 && idx < itemSugg.size()) { selectItem(itemSugg.get(idx)); return true; }
            }
        }

        return super.mouseClicked(click, focused);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingHandle != 0 && enchantMaxLevel > 1) {
            int lv = levelFromX(click.x());
            if (draggingHandle == 1) { minLevel = lv; if (minLevel > maxLevel) maxLevel = minLevel; }
            else                     { maxLevel = lv; if (maxLevel < minLevel) minLevel = maxLevel; }
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) { draggingHandle = 0; return super.mouseReleased(click); }

    // ── keyboard ──────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(KeyInput k) {
        if (k.key() == 256) { client.setScreen(parent); return true; }
        return super.keyPressed(k);
    }

    // ── confirm ───────────────────────────────────────────────────────────
    private void confirm() {
        if (selectedEnchantId.isEmpty() || selectedItemId.isEmpty()) return;
        for (int lv = minLevel; lv <= maxLevel; lv++) {
            String entry = selectedEnchantId + "|" + lv + "|" + selectedItemId;
            if (!parent.recipes.contains(entry)) parent.recipes.add(entry);
        }
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
        return switch (n) { case 1->"I"; case 2->"II"; case 3->"III"; case 4->"IV"; case 5->"V"; default->String.valueOf(n); };
    }

    private String pathToName(String id) {
        String[] words = (id.contains(":") ? id.split(":")[1] : id).split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }

    @Override public boolean shouldPause() { return false; }
}
