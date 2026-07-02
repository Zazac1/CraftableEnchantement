package fr.isaac.craftableenchantement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class RecipeAddScreen extends Screen {

    // ── vanilla enchantment table ──────────────────────────────────────────
    private static final String[][] ENCHANTMENTS = {
            // Sword
            {"minecraft:sharpness", "Sharpness", "5"},
            {"minecraft:smite", "Smite", "5"},
            {"minecraft:bane_of_arthropods", "Bane of Arthropods", "5"},
            {"minecraft:fire_aspect", "Fire Aspect", "2"},
            {"minecraft:knockback", "Knockback", "2"},
            {"minecraft:looting", "Looting", "3"},
            {"minecraft:sweeping_edge", "Sweeping Edge", "3"},
            // Armor
            {"minecraft:protection", "Protection", "4"},
            {"minecraft:fire_protection", "Fire Protection", "4"},
            {"minecraft:blast_protection", "Blast Protection", "4"},
            {"minecraft:projectile_protection", "Projectile Protection", "4"},
            {"minecraft:feather_falling", "Feather Falling", "4"},
            {"minecraft:thorns", "Thorns", "3"},
            {"minecraft:respiration", "Respiration", "3"},
            {"minecraft:aqua_affinity", "Aqua Affinity", "1"},
            {"minecraft:depth_strider", "Depth Strider", "3"},
            {"minecraft:frost_walker", "Frost Walker", "2"},
            {"minecraft:soul_speed", "Soul Speed", "3"},
            {"minecraft:swift_sneak", "Swift Sneak", "3"},
            // Tools
            {"minecraft:efficiency", "Efficiency", "5"},
            {"minecraft:silk_touch", "Silk Touch", "1"},
            {"minecraft:fortune", "Fortune", "3"},
            {"minecraft:unbreaking", "Unbreaking", "3"},
            {"minecraft:mending", "Mending", "1"},
            // Bow
            {"minecraft:power", "Power", "5"},
            {"minecraft:punch", "Punch", "2"},
            {"minecraft:flame", "Flame", "1"},
            {"minecraft:infinity", "Infinity", "1"},
            // Crossbow
            {"minecraft:multishot", "Multishot", "1"},
            {"minecraft:piercing", "Piercing", "4"},
            {"minecraft:quick_charge", "Quick Charge", "3"},
            // Trident
            {"minecraft:impaling", "Impaling", "5"},
            {"minecraft:riptide", "Riptide", "3"},
            {"minecraft:loyalty", "Loyalty", "3"},
            {"minecraft:channeling", "Channeling", "1"},
            // Fishing
            {"minecraft:luck_of_the_sea", "Luck of the Sea", "3"},
            {"minecraft:lure", "Lure", "3"},
            // Curses
            {"minecraft:binding_curse", "Curse of Binding", "1"},
            {"minecraft:vanishing_curse", "Curse of Vanishing", "1"},
    };

    // ── state ─────────────────────────────────────────────────────────────
    private final CraftableConfigScreen parent;

    private int selectedEnchantIdx = 0;
    private int enchantScroll = 0;
    private int level = 1;
    private Item selectedIngredient = Items.BOOK;

    private TextFieldWidget enchantSearch;
    private TextFieldWidget customEnchantField;
    private TextFieldWidget itemSearch;

    private List<String[]> filteredEnchants = new ArrayList<>();
    private List<Item> filteredItems = new ArrayList<>();
    private List<Item> allItems = new ArrayList<>();

    private int itemScroll = 0;
    private static final int ITEM_SIZE = 20;
    private static final int ROW_HEIGHT = 20;
    private int enchantCols, itemCols, itemRows;
    private boolean useCustomEnchant = false;

    public RecipeAddScreen(CraftableConfigScreen parent) {
        super(Text.literal("Add Custom Recipe"));
        this.parent = parent;
    }

    // ── layout constants ──────────────────────────────────────────────────

    private int panelTop() { return 28; }
    private int panelH() { return height - panelTop() - 52; }
    private int enchantPanelW() { return (width - 30) / 2; }
    private int itemPanelW() { return width - 30 - enchantPanelW(); }
    private int enchantPanelX() { return 8; }
    private int itemPanelX() { return enchantPanelX() + enchantPanelW() + 14; }
    private int listTop() { return panelTop() + 24; }
    private int listH() { return panelH() - 44; }

    // ── init ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        loadAllItems();
        filteredEnchants = filterEnchants("");
        filteredItems = filterItems("");

        int ew = enchantPanelW();
        int iw = itemPanelW();
        itemCols = Math.max(1, iw / ITEM_SIZE);

        // Enchantment search
        enchantSearch = addDrawableChild(new TextFieldWidget(
                textRenderer, enchantPanelX(), panelTop() + 2, ew - 2, 18,
                Text.literal("Search enchantment")));
        enchantSearch.setPlaceholder(Text.literal("Search..."));
        enchantSearch.setChangedListener(s -> {
            filteredEnchants = filterEnchants(s);
            enchantScroll = 0;
            selectedEnchantIdx = filteredEnchants.isEmpty() ? -1 : 0;
            rebuildButtons();
        });

        // Item search
        itemSearch = addDrawableChild(new TextFieldWidget(
                textRenderer, itemPanelX(), panelTop() + 2, iw - 2, 18,
                Text.literal("Search item")));
        itemSearch.setPlaceholder(Text.literal("Search..."));
        itemSearch.setChangedListener(s -> {
            filteredItems = filterItems(s);
            itemScroll = 0;
            rebuildButtons();
        });

        // Custom enchant field (shown when no enchantment selected from list)
        customEnchantField = addDrawableChild(new TextFieldWidget(
                textRenderer, enchantPanelX(), panelTop() + 24 + listH() + 4, ew - 2, 16,
                Text.literal("Custom enchantment ID")));
        customEnchantField.setPlaceholder(Text.literal("mymod:custom_enchant"));
        customEnchantField.setMaxLength(100);
        customEnchantField.setChangedListener(s -> useCustomEnchant = !s.isBlank());

        // Level buttons
        int lvlY = height - 44;
        addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> {
            if (level > 1) { level--; rebuildButtons(); }
        }).dimensions(width / 2 - 38, lvlY, 20, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            int max = maxLevel();
            if (level < max) { level++; rebuildButtons(); }
        }).dimensions(width / 2 + 18, lvlY, 20, 18).build());

        // Cancel
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 102, height - 24, 98, 20).build());

        // Confirm
        addDrawableChild(ButtonWidget.builder(Text.literal("✓  Add Recipe"),
                b -> confirm()
        ).dimensions(width / 2 + 4, height - 24, 98, 20).build());

        rebuildButtons();
    }

    private void rebuildButtons() {
        // Remove and re-add item grid buttons and enchantment row buttons
        // (done via clearAndInit instead for simplicity)
        // All dynamic clicks are handled in mouseClicked
    }

    // ── data helpers ─────────────────────────────────────────────────────

    private void loadAllItems() {
        allItems = Registries.ITEM.stream()
                .filter(i -> i != Items.AIR)
                .sorted(Comparator.comparing(i -> Registries.ITEM.getId(i).getPath()))
                .collect(Collectors.toList());
    }

    private List<String[]> filterEnchants(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<String[]> result = new ArrayList<>();
        for (String[] e : ENCHANTMENTS) {
            if (e[1].toLowerCase(Locale.ROOT).contains(q) || e[0].contains(q))
                result.add(e);
        }
        return result;
    }

    private List<Item> filterItems(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return allItems.stream()
                .filter(i -> Registries.ITEM.getId(i).getPath().contains(q))
                .collect(Collectors.toList());
    }

    private int maxLevel() {
        if (useCustomEnchant) return 5;
        if (selectedEnchantIdx < 0 || selectedEnchantIdx >= filteredEnchants.size()) return 5;
        try { return Integer.parseInt(filteredEnchants.get(selectedEnchantIdx)[2]); }
        catch (NumberFormatException e) { return 5; }
    }

    // ── render ────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        // ── Enchantment panel ──────────────────────────────────────────
        int ex = enchantPanelX();
        int ew = enchantPanelW();
        int lh = listH();
        int lt = listTop();

        drawPanel(ctx, ex, panelTop(), ew, panelH(), "Enchantment");

        // Enchantment list rows
        int visible = lh / ROW_HEIGHT;
        for (int i = enchantScroll; i < Math.min(filteredEnchants.size(), enchantScroll + visible); i++) {
            String[] e = filteredEnchants.get(i);
            int ry = lt + (i - enchantScroll) * ROW_HEIGHT;
            boolean selected = i == selectedEnchantIdx && !useCustomEnchant;
            int bg = selected ? 0xAA3355AA : (mouseX >= ex && mouseX < ex + ew - 2
                    && mouseY >= ry && mouseY < ry + ROW_HEIGHT ? 0x44FFFFFF : 0x33000000);
            ctx.fill(ex + 1, ry, ex + ew - 2, ry + ROW_HEIGHT, bg);
            int col = selected ? 0xFFFFFF : 0xCCCCCC;
            ctx.drawTextWithShadow(textRenderer, e[1] + " (max " + e[2] + ")",
                    ex + 4, ry + 6, col);
        }

        // Scroll indicator if needed
        if (filteredEnchants.size() > visible) {
            int scrollMax = filteredEnchants.size() - visible;
            int barH = Math.max(10, lh * visible / filteredEnchants.size());
            int barY = lt + (lh - barH) * enchantScroll / scrollMax;
            ctx.fill(ex + ew - 5, barY, ex + ew - 2, barY + barH, 0xAA888888);
        }

        // Custom enchant hint
        ctx.drawTextWithShadow(textRenderer, Text.literal("Or enter a custom ID:"),
                ex, panelTop() + 24 + lh + 2, 0x999999);

        // ── Item panel ────────────────────────────────────────────────
        int ix = itemPanelX();
        int iw = itemPanelW();
        drawPanel(ctx, ix, panelTop(), iw, panelH(), "Ingredient");

        // Item grid
        int cols = Math.max(1, (iw - 8) / ITEM_SIZE);
        int itemsPerPage = cols * Math.max(1, lh / ITEM_SIZE);
        int startIdx = itemScroll * cols;
        for (int i = startIdx; i < Math.min(filteredItems.size(), startIdx + itemsPerPage); i++) {
            int col = (i - startIdx) % cols;
            int row = (i - startIdx) / cols;
            int gx = ix + 4 + col * ITEM_SIZE;
            int gy = lt + row * ITEM_SIZE;
            Item item = filteredItems.get(i);
            boolean sel = item == selectedIngredient;
            if (sel) ctx.fill(gx - 1, gy - 1, gx + 17, gy + 17, 0xAA3355AA);
            else if (mouseX >= gx && mouseX < gx + 16 && mouseY >= gy && mouseY < gy + 16)
                ctx.fill(gx - 1, gy - 1, gx + 17, gy + 17, 0x44FFFFFF);
            ctx.drawItem(new ItemStack(item), gx, gy);
            // Tooltip on hover
            if (mouseX >= gx && mouseX < gx + 16 && mouseY >= gy && mouseY < gy + 16) {
                ctx.drawItemTooltip(textRenderer, new ItemStack(item), mouseX, mouseY);
            }
        }

        // Selected ingredient label
        String ingLabel = Registries.ITEM.getId(selectedIngredient).getPath().replace("_", " ");
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Selected: ").append(Text.literal(ingLabel).withColor(0xFFD700)),
                ix + 4, panelTop() + panelH() - 14, 0xCCCCCC);

        // ── Level control ─────────────────────────────────────────────
        int lvlY = height - 44;
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Level: ").append(
                        Text.literal(toRoman(level)).withColor(0xFFD700)),
                width / 2, lvlY + 4, 0xFFFFFF);

        // ── Recipe preview ────────────────────────────────────────────
        String enchName = useCustomEnchant
                ? customEnchantField.getText()
                : (selectedEnchantIdx >= 0 && selectedEnchantIdx < filteredEnchants.size()
                ? filteredEnchants.get(selectedEnchantIdx)[1]
                : "?");
        String ingName = Registries.ITEM.getId(selectedIngredient).getPath().replace("_", " ");
        String preview = level + "× XP Bottle + Book + " + ingName + "  →  " + enchName + " " + toRoman(level);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(preview),
                width / 2, height - 46, 0xAAAAAA);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h, String label) {
        ctx.fill(x, y + 20, x + w, y + h, 0x88101010);
        drawBox(ctx, x, y + 20, w, h, 0xFF505050);
        int lw = textRenderer.getWidth(label) + 10;
        ctx.fill(x + 4, y, x + 4 + lw, y + 20, 0x88101010);
        drawBox(ctx, x + 4, y, lw, 20, 0xFF505050);
        ctx.drawTextWithShadow(textRenderer, label, x + 8, y + 6, 0xDDDDDD);
    }

    private void drawBox(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.drawHorizontalLine(x, x + w - 1, y, c);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, c);
        ctx.drawVerticalLine(x, y, y + h - 1, c);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, c);
    }

    // ── mouse ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean focused) {
        if (super.mouseClicked(click, focused)) return true;
        double mx = click.x();
        double my = click.y();

        // Enchantment list click
        int ex = enchantPanelX();
        int ew = enchantPanelW();
        int lt = listTop();
        int lh = listH();
        int visible = lh / ROW_HEIGHT;
        if (mx >= ex && mx < ex + ew - 4 && my >= lt && my < lt + lh) {
            int row = (int) (my - lt) / ROW_HEIGHT;
            int idx = row + enchantScroll;
            if (idx >= 0 && idx < filteredEnchants.size()) {
                selectedEnchantIdx = idx;
                useCustomEnchant = false;
                customEnchantField.setText("");
                level = Math.min(level, maxLevel());
            }
            return true;
        }

        // Item grid click
        int ix = itemPanelX();
        int iw = itemPanelW();
        int cols = Math.max(1, (iw - 8) / ITEM_SIZE);
        if (mx >= ix + 4 && mx < ix + iw && my >= lt && my < lt + lh) {
            int col = (int) (mx - ix - 4) / ITEM_SIZE;
            int row = (int) (my - lt) / ITEM_SIZE;
            int idx = itemScroll * cols + row * cols + col;
            if (idx >= 0 && idx < filteredItems.size()) {
                selectedIngredient = filteredItems.get(idx);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int ex = enchantPanelX();
        int ew = enchantPanelW();
        int lh = listH();
        int visible = lh / ROW_HEIGHT;

        if (mx >= ex && mx < ex + ew) {
            enchantScroll = Math.max(0, Math.min(
                    enchantScroll - (int) vAmt,
                    Math.max(0, filteredEnchants.size() - visible)));
        } else {
            int ix = itemPanelX();
            int iw = itemPanelW();
            int cols = Math.max(1, (iw - 8) / ITEM_SIZE);
            int totalRows = (int) Math.ceil((double) filteredItems.size() / cols);
            int visibleRows = lh / ITEM_SIZE;
            itemScroll = Math.max(0, Math.min(itemScroll - (int) vAmt,
                    Math.max(0, totalRows - visibleRows)));
        }
        return true;
    }

    // ── keyboard ──────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.key() == 256) { client.setScreen(parent); return true; }
        return super.keyPressed(keyInput);
    }

    // ── confirm ───────────────────────────────────────────────────────────

    private void confirm() {
        String enchId;
        if (useCustomEnchant) {
            enchId = customEnchantField.getText().trim();
        } else if (selectedEnchantIdx >= 0 && selectedEnchantIdx < filteredEnchants.size()) {
            enchId = filteredEnchants.get(selectedEnchantIdx)[0];
        } else {
            return;
        }
        if (enchId.isEmpty()) return;

        String ingId = Registries.ITEM.getId(selectedIngredient).toString();
        String entry = enchId + "|" + level + "|" + ingId;
        if (!parent.recipes.contains(entry)) {
            parent.recipes.add(entry);
        }
        client.setScreen(parent);
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    @Override
    public boolean shouldPause() { return false; }
}
