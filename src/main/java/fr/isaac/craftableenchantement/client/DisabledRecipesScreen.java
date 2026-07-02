package fr.isaac.craftableenchantement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Manage the list of disabled default enchantment recipes.
 * Fills and text are both added via addDrawable so they render correctly in MC 1.21.11.
 */
@Environment(EnvType.CLIENT)
public class DisabledRecipesScreen extends Screen {

    private static final int PAD = 10;
    private static final int ROW = 20;

    private final CraftableConfigScreen parent;
    private final List<String> disabled;
    private TextFieldWidget addField;
    private int scroll = 0;

    public DisabledRecipesScreen(CraftableConfigScreen parent) {
        super(Text.literal("Default Recipes"));
        this.parent  = parent;
        this.disabled = parent.disabled;
    }

    @Override
    protected void init() {
        // ── Fills (background, entry rows) — rendered first ───────────────
        addDrawable((ctx, mx, my, d) -> renderFills(ctx));

        // ── Add field + button ────────────────────────────────────────────
        addField = addDrawableChild(new TextFieldWidget(
                textRenderer, PAD, height - 66, width - PAD * 2 - 66, 16,
                Text.literal("enchant id")));
        addField.setPlaceholder(Text.literal("minecraft:mending"));
        addField.setMaxLength(100);

        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add"),
                b -> {
                    String id = addField.getText().trim();
                    if (!id.isEmpty() && !disabled.contains(id)) {
                        disabled.add(id);
                        addField.setText("");
                        clearAndInit();
                    }
                }
        ).dimensions(width - PAD - 60, height - 66, 60, 16).build());

        // ── Remove buttons per row ────────────────────────────────────────
        int visible = maxVisible();
        for (int i = scroll; i < Math.min(disabled.size(), scroll + visible); i++) {
            final int idx = i;
            int y = rowY(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("✕"),
                    b -> { disabled.remove(idx); clearAndInit(); }
            ).dimensions(width - PAD - 20, y + 1, 18, 16).build());
        }

        // Back button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 50, height - 22, 100, 18).build());

        // ── Text (last = on top) ──────────────────────────────────────────
        addDrawable((ctx, mx, my, d) -> renderText(ctx));
    }

    // ── layout ─────────────────────────────────────────────────────────────
    private int listTop()   { return 30; }
    private int listH()     { return height - 96; }
    private int maxVisible(){ return Math.max(1, listH() / ROW); }
    private int rowY(int i) { return listTop() + (i - scroll) * ROW; }

    // ── fills ──────────────────────────────────────────────────────────────
    private void renderFills(DrawContext ctx) {
        ctx.fill(PAD, listTop() - 2, width - PAD, listTop() + listH(), 0x88101010);
        drawBox(ctx, PAD, listTop() - 2, width - PAD * 2, listH() + 2, 0xFF505050);

        int visible = maxVisible();
        for (int i = scroll; i < Math.min(disabled.size(), scroll + visible); i++) {
            int y = rowY(i);
            ctx.fill(PAD + 1, y, width - PAD - 22, y + ROW - 2, 0x44000000);
        }
    }

    // ── text ───────────────────────────────────────────────────────────────
    private void renderText(DrawContext ctx) {
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Disable a built-in recipe (enchantment ID):"),
                PAD, height - 80, 0xAAAAAA);

        int visible = maxVisible();
        if (disabled.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("All default recipes are active."),
                    width / 2, listTop() + 8, 0x999999);
        }
        for (int i = scroll; i < Math.min(disabled.size(), scroll + visible); i++) {
            int y = rowY(i);
            ctx.drawTextWithShadow(textRenderer, disabled.get(i), PAD + 4, y + 6, 0xE0E0E0);
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
        scroll = Math.max(0, Math.min(scroll - (int) v, Math.max(0, disabled.size() - maxVisible())));
        clearAndInit();
        return true;
    }

    private void drawBox(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.drawHorizontalLine(x, x + w - 1, y, c);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, c);
        ctx.drawVerticalLine(x, y, y + h - 1, c);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, c);
    }

    @Override public boolean shouldPause() { return false; }
}
