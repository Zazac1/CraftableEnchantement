package fr.isaac.craftableenchantement.client;

import fr.isaac.craftableenchantement.ConfigLoader;
import fr.isaac.craftableenchantement.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class CraftableConfigScreen extends Screen {

    private final Screen parent;
    final List<String> recipes;
    final List<String> disabled;

    private int recipeScroll = 0;
    private int disabledScroll = 0;
    private TextFieldWidget newDisabledField;

    private static final int ROW = 22;
    private static final int SEC_TITLE = 14;
    private static final int PAD = 10;

    public CraftableConfigScreen(Screen parent) {
        super(Text.literal("Craftable Enchantement — Config"));
        this.parent = parent;
        ModConfig cfg = ConfigLoader.get();
        this.recipes = new ArrayList<>(cfg.custom_recipes);
        this.disabled = new ArrayList<>(cfg.disabled_enchantments);
    }

    @Override
    protected void init() {
        // Add recipe button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("+ Add Recipe"),
                b -> client.setScreen(new RecipeAddScreen(this))
        ).dimensions(width - PAD - 110, recipeTop() - 2, 110, 18).build());

        // Add disabled enchant button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("+ Add"),
                b -> {
                    String id = newDisabledField.getText().trim();
                    if (!id.isEmpty() && !disabled.contains(id)) {
                        disabled.add(id);
                        newDisabledField.setText("");
                        clearAndInit();
                    }
                }
        ).dimensions(width - PAD - 60, disabledTop() - 2, 60, 18).build());

        // New disabled field
        newDisabledField = addDrawableChild(new TextFieldWidget(
                textRenderer,
                PAD, disabledTop() - 2, width - PAD * 2 - 64, 18,
                Text.literal("minecraft:enchantment_id")));
        newDisabledField.setPlaceholder(Text.literal("minecraft:mending"));
        newDisabledField.setMaxLength(100);

        // Save
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Save"),
                b -> {
                    ModConfig cfg = ConfigLoader.get();
                    cfg.custom_recipes = new ArrayList<>(recipes);
                    cfg.disabled_enchantments = new ArrayList<>(disabled);
                    ConfigLoader.saveAndInvalidate(cfg);
                    client.setScreen(parent);
                }
        ).dimensions(width / 2 + 4, height - 26, 98, 20).build());

        // Cancel
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 102, height - 26, 98, 20).build());

        // Remove buttons for recipes
        int visibleRecipes = maxVisibleRecipes();
        for (int i = recipeScroll; i < Math.min(recipes.size(), recipeScroll + visibleRecipes); i++) {
            final int idx = i;
            int y = recipeTop() + (i - recipeScroll) * ROW;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("✕"),
                    b -> { recipes.remove(idx); clearAndInit(); }
            ).dimensions(width - PAD - 22, y + 1, 20, 18).build());
        }

        // Remove buttons for disabled
        int visibleDisabled = maxVisibleDisabled();
        for (int i = disabledScroll; i < Math.min(disabled.size(), disabledScroll + visibleDisabled); i++) {
            final int idx = i;
            int y = disabledTop() + (i - disabledScroll) * ROW + 22; // below add field
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("✕"),
                    b -> { disabled.remove(idx); clearAndInit(); }
            ).dimensions(width - PAD - 22, y + 1, 20, 18).build());
        }
    }

    // ─── layout helpers ──────────────────────────────────────────────────────

    private int recipeTop() {
        return 36;
    }

    private int recipeSectionHeight() {
        int half = (height - 70) / 2;
        return Math.max(half, 60);
    }

    private int disabledTop() {
        return recipeTop() + recipeSectionHeight() + SEC_TITLE + 8;
    }

    private int maxVisibleRecipes() {
        return Math.max(1, (recipeSectionHeight() - 4) / ROW);
    }

    private int maxVisibleDisabled() {
        int remaining = height - 70 - disabledTop() - 22 - 24;
        return Math.max(1, remaining / ROW);
    }

    // ─── render ──────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        drawSection(ctx, "Custom Recipes",
                PAD, recipeTop() - SEC_TITLE - 2,
                width - PAD * 2, recipeSectionHeight() + SEC_TITLE + 2);
        drawSection(ctx, "Disabled Enchantments",
                PAD, disabledTop() - SEC_TITLE - 2,
                width - PAD * 2, height - 34 - (disabledTop() - SEC_TITLE - 2));

        // Recipe rows
        int visibleRecipes = maxVisibleRecipes();
        for (int i = recipeScroll; i < Math.min(recipes.size(), recipeScroll + visibleRecipes); i++) {
            int y = recipeTop() + (i - recipeScroll) * ROW;
            String raw = recipes.get(i);
            String display = formatRecipeLabel(raw);
            ctx.fill(PAD + 1, y, width - PAD - 24, y + ROW - 2, 0x55000000);
            ctx.drawTextWithShadow(textRenderer, display, PAD + 5, y + 7, 0xE0E0E0);
        }
        if (recipes.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("No custom recipes — click '+ Add Recipe'"),
                    PAD + 5, recipeTop() + 6, 0x888888);
        }

        // Disabled rows (below the text field)
        int visibleDisabled = maxVisibleDisabled();
        for (int i = disabledScroll; i < Math.min(disabled.size(), disabledScroll + visibleDisabled); i++) {
            int y = disabledTop() + (i - disabledScroll) * ROW + 22;
            ctx.fill(PAD + 1, y, width - PAD - 24, y + ROW - 2, 0x55000000);
            ctx.drawTextWithShadow(textRenderer, disabled.get(i), PAD + 5, y + 7, 0xE0E0E0);
        }
        if (disabled.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("No disabled enchantments"),
                    PAD + 5, disabledTop() + 22 + 6, 0x888888);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawSection(DrawContext ctx, String label, int x, int y, int w, int h) {
        ctx.fill(x, y + SEC_TITLE, x + w, y + h, 0x88101010);
        drawBox(ctx, x, y + SEC_TITLE, w, h, 0xFF505050);
        int lw = textRenderer.getWidth(label) + 8;
        ctx.fill(x + 6, y, x + 6 + lw, y + SEC_TITLE, 0x88101010);
        drawBox(ctx, x + 6, y, lw, SEC_TITLE, 0xFF505050);
        ctx.drawTextWithShadow(textRenderer, label, x + 10, y + 3, 0xDDDDDD);
    }

    private void drawBox(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.drawHorizontalLine(x, x + w - 1, y, c);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, c);
        ctx.drawVerticalLine(x, y, y + h - 1, c);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, c);
    }

    private String formatRecipeLabel(String raw) {
        String[] parts = raw.split("\\|", 3);
        if (parts.length != 3) return raw;
        String ench = parts[0].contains(":") ? parts[0].split(":")[1].replace("_", " ") : parts[0];
        String enchCap = Character.toUpperCase(ench.charAt(0)) + ench.substring(1);
        String item = parts[2].contains(":") ? parts[2].split(":")[1].replace("_", " ") : parts[2];
        return enchCap + " " + toRoman(parts[1]) + "  —  " + item;
    }

    private String toRoman(String lvl) {
        return switch (lvl.trim()) {
            case "1" -> "I";
            case "2" -> "II";
            case "3" -> "III";
            case "4" -> "IV";
            case "5" -> "V";
            default -> lvl;
        };
    }

    // ─── scroll ──────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        int recipeSectionBottom = recipeTop() + recipeSectionHeight();
        if (mouseY < recipeSectionBottom) {
            recipeScroll = Math.max(0, Math.min(recipeScroll - (int) vAmt,
                    Math.max(0, recipes.size() - maxVisibleRecipes())));
        } else {
            disabledScroll = Math.max(0, Math.min(disabledScroll - (int) vAmt,
                    Math.max(0, disabled.size() - maxVisibleDisabled())));
        }
        clearAndInit();
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
