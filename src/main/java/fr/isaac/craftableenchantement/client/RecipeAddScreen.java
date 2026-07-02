package fr.isaac.craftableenchantement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
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
 * All text is rendered via MultilineTextWidget (the ONLY working text API in MC 1.21.11).
 * Fills/items use addDrawable (works fine).
 * Text field content is persisted across clearAndInit() in instance variables.
 */
@Environment(EnvType.CLIENT)
public class RecipeAddScreen extends Screen {

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

    private static final int PAD = 8, SLOT = 20, SUGG_H = 14, MAX_S = 6;

    private final CraftableConfigScreen parent;

    // Persisted state (survives clearAndInit)
    private String selectedEnchantId = "", selectedEnchantName = "";
    private int enchantMaxLevel = 1, minLevel = 1, maxLevel = 1;
    private int draggingHandle = 0;
    private String selectedItemId = "", selectedItemName = "";
    private List<String[]> enchantSugg = new ArrayList<>();
    private List<String[]> itemSugg    = new ArrayList<>();
    private String enchantFieldText = "";
    private String itemFieldText    = "";
    /** Which field should get focus after the next clearAndInit(). 1=enchant, 2=item, 0=none */
    private int restoreFocus = 0;

    private TextFieldWidget enchantField, itemField;

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
        // ── Fills + grid + slider (addDrawable works) ─────────────────────
        addDrawable((ctx, mx, my, d) -> renderFills(ctx, mx, my));

        // ── Static labels via MultilineTextWidget ─────────────────────────
        addDrawableChild(makeLabel(leftX(), enchLabelY(), "Enchantment:", 0xCCCCCC));
        addDrawableChild(makeLabel(leftX(), itemLabelY(),  "Ingredient:",  0xCCCCCC));

        // Level label (re-created on clearAndInit so it reflects current state)
        String lvlTxt = enchantMaxLevel <= 1 ? "Level I"
                : (minLevel == maxLevel ? "Level " + toRoman(minLevel)
                : "Level " + toRoman(minLevel) + " – " + toRoman(maxLevel));
        addDrawableChild(makeCenteredLabel(sliderX() + sliderW() / 2,
                sliderY() + sliderH() + 5, lvlTxt + " / " + toRoman(enchantMaxLevel), 0xFFD700));

        // Arrow
        addDrawableChild(makeCenteredLabel(gridX() + 3 * SLOT + 6, gridY() + SLOT + 5, "→", 0xFFFFFF));

        // Selected item name
        if (!selectedItemId.isEmpty()) {
            addDrawableChild(makeLabel(leftX() + 22, previewBoxY() + (20 - 8) / 2,
                    selectedItemName, 0x88FF88));
        }

        // Preview line
        String en = selectedEnchantName.isEmpty() ? "?" : selectedEnchantName;
        String it = selectedItemName.isEmpty() ? "?" : selectedItemName.toLowerCase(Locale.ROOT);
        String lvPart = (enchantMaxLevel > 1 && minLevel != maxLevel)
                ? toRoman(minLevel) + "–" + toRoman(maxLevel) : toRoman(minLevel);
        addDrawableChild(makeCenteredLabel(width / 2, height - 36,
                (minLevel == maxLevel ? minLevel + "\u00d7" : minLevel + "\u2013" + maxLevel + "\u00d7")
                        + " XP + Book + " + it + "  \u2192  " + en + " " + lvPart, 0x999999));

        // Enchant suggestions via MultilineTextWidget
        if (!enchantSugg.isEmpty()) {
            for (int i = 0; i < enchantSugg.size(); i++) {
                String[] e = enchantSugg.get(i);
                int ry = enchSuggY() + i * SUGG_H;
                addDrawableChild(makeLabel(leftX() + 4, ry + 3,
                        e[1] + "  (max " + e[2] + ")", 0xBBBBBB));
            }
        }

        // Item suggestions via MultilineTextWidget
        if (!itemSugg.isEmpty()) {
            for (int i = 0; i < itemSugg.size(); i++) {
                String[] s = itemSugg.get(i);
                int ry = itemSuggY() + i * SUGG_H;
                addDrawableChild(makeLabel(leftX() + 20, ry + 3,
                        s[1] + "  " + s[0], 0xCCCCCC));
            }
        }

        // ── Text fields ────────────────────────────────────────────────────
        enchantField = addDrawableChild(new TextFieldWidget(
                textRenderer, leftX(), enchFieldY(), leftW(), 14, Text.literal("e")));
        enchantField.setPlaceholder(Text.literal("Type enchantment name…"));
        enchantField.setMaxLength(80);
        enchantField.setText(enchantFieldText);
        enchantField.setChangedListener(s -> { enchantFieldText = s; restoreFocus = 1; onEnchantTyped(s); });

        itemField = addDrawableChild(new TextFieldWidget(
                textRenderer, leftX(), itemFieldY(), leftW(), 14, Text.literal("i")));
        itemField.setPlaceholder(Text.literal("Type item id…"));
        itemField.setMaxLength(80);
        itemField.setText(itemFieldText);
        itemField.setChangedListener(s -> { itemFieldText = s; restoreFocus = 2; onItemTyped(s); });

        // ── Buttons ────────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 102, height - 22, 98, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("✓  Add Recipe"),
                b -> confirm()
        ).dimensions(width / 2 + 4, height - 22, 98, 18).build());
    }

    private int previewBoxY() {
        return Math.min(itemSuggY() + (itemSugg.isEmpty() ? 2 : itemSugg.size() * SUGG_H + 4), height - 60);
        // Restore focus to the field the user was typing in
        if (restoreFocus == 1) { setFocused(enchantField); restoreFocus = 0; }
        else if (restoreFocus == 2) { setFocused(itemField); restoreFocus = 0; }
    }
        if (q.isBlank()) { enchantSugg = new ArrayList<>(); }
        else {
            String ql = q.toLowerCase(Locale.ROOT);
            enchantSugg = new ArrayList<>();
            for (String[] e : ENCHANTMENTS)
                if (e[1].toLowerCase(Locale.ROOT).contains(ql) || e[0].contains(ql)) {
                    enchantSugg.add(e); if (enchantSugg.size() >= MAX_S) break;
                }
        }
        clearAndInit();
    }

    private void onItemTyped(String q) {
        if (q.isBlank()) { itemSugg = new ArrayList<>(); }
        else {
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
        clearAndInit();
    }

    private void selectEnchant(String[] e) {
        selectedEnchantId = e[0]; selectedEnchantName = e[1];
        enchantMaxLevel   = Integer.parseInt(e[2]);
        minLevel = 1; maxLevel = enchantMaxLevel;
        enchantFieldText  = e[1];
        enchantSugg       = new ArrayList<>();
        clearAndInit();
    }

    private void selectItem(String[] s) {
        selectedItemId   = s[0]; selectedItemName = s[1];
        itemFieldText    = s[1].toLowerCase(Locale.ROOT).replace(" ", "_");
        itemSugg         = new ArrayList<>();
        clearAndInit();
    }

    // ── fills + grid ──────────────────────────────────────────────────────
    private void renderFills(DrawContext ctx, int mx, int my) {
        // Grid slots + items
        int gx0 = gridX(), gy0 = gridY();
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            int sx = gx0 + c * SLOT, sy = gy0 + r * SLOT;
            ctx.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0xFF3A3A3A);
            drawBox(ctx, sx, sy, SLOT, SLOT, 0xFF555555);
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stk = slotItem(i);
            if (!stk.isEmpty()) ctx.drawItem(stk, gx0 + (i % 3) * SLOT + 2, gy0 + (i / 3) * SLOT + 2);
        }
        // Result slot
        int resX = gx0 + 3 * SLOT + 12, resY = gy0 + SLOT;
        ctx.fill(resX + 1, resY + 1, resX + SLOT - 1, resY + SLOT - 1, 0xFF3A3A3A);
        drawBox(ctx, resX, resY, SLOT, SLOT, 0xFF908830);
        ctx.drawItem(new ItemStack(Items.ENCHANTED_BOOK), resX + 2, resY + 2);

        // Range slider
        if (enchantMaxLevel > 1) {
            int ty = sliderY();
            ctx.fill(sliderX(), ty, sliderX() + sliderW(), ty + sliderH(), 0xFF404040);
            ctx.fill(handleX(minLevel) + 3, ty, handleX(maxLevel) + 3, ty + sliderH(), 0xFF4477DD);
            for (int lv = 1; lv <= enchantMaxLevel; lv++) {
                int tx = handleX(lv) + 3;
                ctx.fill(tx - 1, ty - 2, tx + 1, ty, 0xFF888888);
            }
            int mhx = handleX(minLevel), Mhx = handleX(maxLevel);
            ctx.fill(mhx, ty - 2, mhx + 6, ty + sliderH() + 2, draggingHandle == 1 || Math.abs(mx - mhx - 3) <= 6 ? 0xFFDDDDDD : 0xFF999999);
            drawBox(ctx, mhx, ty - 2, 6, sliderH() + 4, 0xFF444444);
            ctx.fill(Mhx, ty - 2, Mhx + 6, ty + sliderH() + 2, draggingHandle == 2 || Math.abs(mx - Mhx - 3) <= 6 ? 0xFFDDDDDD : 0xFF999999);
            drawBox(ctx, Mhx, ty - 2, 6, sliderH() + 4, 0xFF444444);
        }

        // Selected item preview box
        if (!selectedItemId.isEmpty()) {
            int py = previewBoxY();
            ctx.fill(leftX(), py, leftX() + leftW(), py + 20, 0x88203020);
            drawBox(ctx, leftX(), py, leftW(), 20, 0xFF447744);
            var item = Registries.ITEM.get(Identifier.tryParse(selectedItemId));
            if (item != null && item != Items.AIR) ctx.drawItem(new ItemStack(item), leftX() + 2, py + 2);
        }

        // Enchant suggestion backgrounds + item icons
        if (!enchantSugg.isEmpty()) {
            int sy = enchSuggY(), sw = leftW(), sh = enchantSugg.size() * SUGG_H;
            ctx.fill(leftX(), sy, leftX() + sw, sy + sh, 0xFF1A1C28);
            drawBox(ctx, leftX(), sy, sw, sh, 0xFF4A5578);
            for (int i = 0; i < enchantSugg.size(); i++) {
                int ry = sy + i * SUGG_H;
                if (mx >= leftX() && mx < leftX() + sw && my >= ry && my < ry + SUGG_H)
                    ctx.fill(leftX() + 1, ry, leftX() + sw - 1, ry + SUGG_H, 0x553355BB);
            }
        }

        // Item suggestion backgrounds + item icons
        if (!itemSugg.isEmpty()) {
            int sy = itemSuggY(), sw = leftW(), sh = itemSugg.size() * SUGG_H;
            ctx.fill(leftX(), sy, leftX() + sw, sy + sh, 0xFF1A1C28);
            drawBox(ctx, leftX(), sy, sw, sh, 0xFF4A5578);
            for (int i = 0; i < itemSugg.size(); i++) {
                String[] s = itemSugg.get(i);
                int ry = sy + i * SUGG_H;
                if (mx >= leftX() && mx < leftX() + sw && my >= ry && my < ry + SUGG_H)
                    ctx.fill(leftX() + 1, ry, leftX() + sw - 1, ry + SUGG_H, 0x553355BB);
                var item = Registries.ITEM.get(Identifier.tryParse(s[0]));
                if (item != null && item != Items.AIR)
                    ctx.drawItem(new ItemStack(item), leftX() + 2, ry);
            }
        }
    }

    private ItemStack slotItem(int slot) {
        if (slot < maxLevel) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        if (slot == maxLevel) return new ItemStack(Items.BOOK);
        if (slot == maxLevel + 1 && !selectedItemId.isEmpty()) {
            var item = Registries.ITEM.get(Identifier.tryParse(selectedItemId));
            if (item != null && item != Items.AIR) return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    // ── render ────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── mouse ─────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(Click click, boolean focused) {
        double mx = click.x(), my = click.y();
        if (enchantMaxLevel > 1) {
            int ty = sliderY();
            if (my >= ty - 4 && my <= ty + sliderH() + 6 && mx >= sliderX() && mx <= sliderX() + sliderW()) {
                int dMin = Math.abs((int)mx - (handleX(minLevel) + 3));
                int dMax = Math.abs((int)mx - (handleX(maxLevel) + 3));
                if (dMin <= dMax) { draggingHandle = 1; minLevel = levelFromX(mx); if (minLevel > maxLevel) maxLevel = minLevel; }
                else              { draggingHandle = 2; maxLevel = levelFromX(mx); if (maxLevel < minLevel) minLevel = maxLevel; }
                clearAndInit(); return true;
            }
        }
        if (!enchantSugg.isEmpty()) {
            int sy = enchSuggY(), sw = leftW();
            if (mx >= leftX() && mx < leftX() + sw && my >= sy && my < sy + enchantSugg.size() * SUGG_H) {
                int idx = (int)(my - sy) / SUGG_H;
                if (idx >= 0 && idx < enchantSugg.size()) { selectEnchant(enchantSugg.get(idx)); return true; }
            }
        }
        if (!itemSugg.isEmpty()) {
            int sy = itemSuggY(), sw = leftW();
            if (mx >= leftX() && mx < leftX() + sw && my >= sy && my < sy + itemSugg.size() * SUGG_H) {
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
            clearAndInit(); return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override public boolean mouseReleased(Click click) { draggingHandle = 0; return super.mouseReleased(click); }

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
    private MultilineTextWidget makeLabel(int x, int y, String text, int color) {
        MultilineTextWidget w = new MultilineTextWidget(x, y, Text.literal(text).withColor(color), textRenderer);
        w.setMaxWidth(leftW()); w.setMaxRows(1); return w;
    }

    private MultilineTextWidget makeCenteredLabel(int cx, int y, String text, int color) {
        MultilineTextWidget w = new MultilineTextWidget(cx, y, Text.literal(text).withColor(color), textRenderer);
        w.setMaxWidth(width - 10); w.setCentered(true); w.setMaxRows(1); return w;
    }

    private void drawBox(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.drawHorizontalLine(x, x + w - 1, y, c);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, c);
        ctx.drawVerticalLine(x, y, y + h - 1, c);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, c);
    }

    private String toRoman(int n) { return switch(n){case 1->"I";case 2->"II";case 3->"III";case 4->"IV";case 5->"V";default->String.valueOf(n);}; }
    private String pathToName(String id) {
        String[] words = (id.contains(":") ? id.split(":")[1] : id).split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }

    @Override public boolean shouldPause() { return false; }
}
