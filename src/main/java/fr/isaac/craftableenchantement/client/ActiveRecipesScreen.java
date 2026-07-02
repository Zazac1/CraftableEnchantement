package fr.isaac.craftableenchantement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ActiveRecipesScreen extends Screen {

    private static final int PAD = 8, ROW = 20;
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
        // Background fill drawable (fills work fine)
        addDrawable((ctx, mx, my, d) -> renderFills(ctx));

        // Title — left edge at PAD so setCentered centres at screen-centre
        MultilineTextWidget titleW = new MultilineTextWidget(PAD, 10, title, textRenderer);
        titleW.setMaxWidth(width - 20);
        titleW.setCentered(true);
        addDrawableChild(titleW);

        // Row labels + remove buttons
        int visible = maxVisible();
        for (int i = scroll; i < Math.min(recipes.size(), scroll + visible); i++) {
            final int idx = i;
            int y = rowY(i);

            // Label via MultilineTextWidget
            String raw = recipes.get(i);
            MultilineTextWidget lbl = new MultilineTextWidget(
                    PAD + 4, y + (ROW - 8) / 2,
                    Text.literal(formatLabel(raw)),
                    textRenderer);
            lbl.setMaxWidth(width - PAD * 2 - 30);
            lbl.setMaxRows(1);
            addDrawableChild(lbl);

            // Remove button
            addDrawableChild(ButtonWidget.builder(Text.literal("✕"),
                    b -> { recipes.remove(idx); clearAndInit(); }
            ).dimensions(width - PAD - 20, y + 1, 18, ROW - 3).build());
        }

        // Empty message
        if (recipes.isEmpty()) {
            MultilineTextWidget empty = new MultilineTextWidget(
                    PAD, listTop() + 8,
                    Text.literal("No custom recipes. Use \"Create a Recipe\".").withColor(0x999999),
                    textRenderer);
            empty.setMaxWidth(width - PAD * 2);
            empty.setCentered(true);
            addDrawableChild(empty);
        }

        // Add Recipe button
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Recipe"),
                b -> client.setScreen(new RecipeAddScreen(parent))
        ).dimensions(width / 2 - 100, height - 44, 200, 18).build());

        // Back
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"),
                b -> client.setScreen(parent)
        ).dimensions(width / 2 - 50, height - 22, 100, 18).build());
    }

    private int listTop()    { return 26; }
    private int listH()      { return height - 70; }
    private int maxVisible() { return Math.max(1, listH() / ROW); }
    private int rowY(int i)  { return listTop() + (i - scroll) * ROW; }

    private void renderFills(DrawContext ctx) {
        ctx.fill(PAD, listTop(), width - PAD, listTop() + listH(), 0x88101010);
        drawBox(ctx, PAD, listTop(), width - PAD * 2, listH(), 0xFF505050);
        int visible = maxVisible();
        for (int i = scroll; i < Math.min(recipes.size(), scroll + visible); i++) {
            int y = rowY(i);
            ctx.fill(PAD + 1, y, width - PAD - 22, y + ROW - 2, 0x44000000);
        }
    }

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

    private String formatLabel(String raw) {
        String[] p = raw.split("\\|", 3);
        if (p.length != 3) return raw;
        String ench = p[0].contains(":") ? p[0].split(":")[1].replace("_", " ") : p[0];
        ench = Character.toUpperCase(ench.charAt(0)) + ench.substring(1);
        String item = p[2].contains(":") ? p[2].split(":")[1].replace("_", " ") : p[2];
        String lvl = switch (p[1].trim()) {
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
