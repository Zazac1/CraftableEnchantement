package fr.isaac.craftableenchantement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Shows the list of user-added custom recipes, with ✕ to remove.
 * Fills and text are both added via addDrawable so they render inside MC's pipeline.
 */
@Environment(EnvType.CLIENT)
public class ActiveRecipesScreen extends Screen {

    private static final int PAD = 10;
    private static final int ROW = 20;

    private final CraftableConfigScreen parent;
    private final List<String> recipes;
    private int scroll = 0;

    public ActiveRecipesScreen(CraftableConfigScreen parent) {
        super(Text.literal("Activated Recipes"));
        this.parent  = parent;
        this.recipes = parent.recipes;
    }

    @Override
    protected void init() {
        // ── Static fills (added first = rendered behind widgets) ──────────
        addDrawable((ctx, mx, my, d) -> renderFills(ctx));

        // ── Remove buttons per row ────────────────────────────────────────
        int visible = maxVisible();
        for (int i = scroll; i < Math.min(recipes.size(), scroll + visible); i++) {
            final int idx = i;
            int y = rowY(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("✕"),
                    b -> { recipes.remove(idx); clearAndInit(); }
            ).dimensions(width - PAD - 20, y + 1, 18, 16).build());
        }

        // + Add button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("+ Add Recipe"),
                b -> client.setScreen(new RecipeAddScreen(parent))
        ).dimensions(width / 2 - 100, height - 44, 200, 18).build());

        // Back button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 50, height - 22, 100, 18).build());

        // ── Text (added last = rendered on top) ───────────────────────────
        addDrawable((ctx, mx, my, d) -> renderText(ctx));
    }

    // ── layout ─────────────────────────────────────────────────────────────
    private int listTop()   { return 30; }
    private int listH()     { return height - 74; }
    private int maxVisible(){ return Math.max(1, listH() / ROW); }
    private int rowY(int i) { return listTop() + (i - scroll) * ROW; }

    // ── fills ──────────────────────────────────────────────────────────────
    private void renderFills(DrawContext ctx) {
        ctx.fill(PAD, listTop() - 2, width - PAD, listTop() + listH(), 0x88101010);
        drawBox(ctx, PAD, listTop() - 2, width - PAD * 2, listH() + 2, 0xFF505050);

        int visible = maxVisible();
        for (int i = scroll; i < Math.min(recipes.size(), scroll + visible); i++) {
            int y = rowY(i);
            ctx.fill(PAD + 1, y, width - PAD - 22, y + ROW - 2, 0x44000000);
        }
    }

    // ── text ───────────────────────────────────────────────────────────────
    private void renderText(DrawContext ctx) {
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        int visible = maxVisible();
        if (recipes.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("No custom recipes. Click \"+ Add Recipe\"."),
                    width / 2, listTop() + 8, 0x999999);
        }
        for (int i = scroll; i < Math.min(recipes.size(), scroll + visible); i++) {
            int y = rowY(i);
            ctx.drawTextWithShadow(textRenderer, formatLabel(recipes.get(i)),
                    PAD + 4, y + 6, 0xE0E0E0);
        }
    }

    // ── render ─────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, Math.min(scroll - (int) v, Math.max(0, recipes.size() - maxVisible())));
        clearAndInit();
        return true;
    }

    // ── helpers ────────────────────────────────────────────────────────────
    private String formatLabel(String raw) {
        String[] p = raw.split("\\|", 3);
        if (p.length != 3) return raw;
        String ench = p[0].contains(":") ? p[0].split(":")[1].replace("_", " ") : p[0];
        ench = Character.toUpperCase(ench.charAt(0)) + ench.substring(1);
        String item = p[2].contains(":") ? p[2].split(":")[1].replace("_", " ") : p[2];
        String lvl  = switch (p[1].trim()) {
            case "1" -> "I"; case "2" -> "II"; case "3" -> "III";
            case "4" -> "IV"; case "5" -> "V"; default -> p[1];
        };
        return ench + " " + lvl + "  \u2014  " + item;
    }

    private void drawBox(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.drawHorizontalLine(x, x + w - 1, y, c);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, c);
        ctx.drawVerticalLine(x, y, y + h - 1, c);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, c);
    }

    @Override public boolean shouldPause() { return false; }
}
