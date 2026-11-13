package com.lumina.ae2_gvt.integration;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * How a {@link WorldInteraction} is drawn, for JEI and EMI alike: a small picture rather than a line of words. What goes in
 * is on the left (an explosion over a dashed ring, an item dropping onto a hole), what comes of it on the right. The words
 * are tooltips on the pictures.
 */
public final class WorldScene {
    public static final int WIDTH = 200, HEIGHT = 56, SLOT = 18;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/world_scene.png");

    /** the sprites of textures/gui/world_scene.png (tools/world_scene_textures.py) */
    public enum Sprite {
        EXPLOSION(0, 0, 24, 24), ANY_ITEM(24, 0, 18, 18), ARROW(42, 0, 22, 15), DOWN(0, 24, 11, 12), CHEVRON(12, 24, 8, 11), PLUS(21, 24, 7, 7);

        public final int u, v, w, h;

        Sprite(int u, int v, int w, int h) {
            this.u = u;
            this.v = v;
            this.w = w;
            this.h = h;
        }
    }

    public enum Part {INPUT, CONDITION, OUTPUT}

    /** an item slot (top-left of its 18x18 box) showing entry {@code index} of the part's list */
    public record Slot(Part part, int index, int x, int y) {
    }

    /** a sprite, with the words that show when the mouse is on it ({@code tip} is a lang key, or null) */
    public record Icon(Sprite sprite, int x, int y, Component tip) {
    }

    /** the dashed ring that says "all of these together" */
    public record Ring(int x, int y, int w, int h, Component tip) {
    }

    /** @param hidden the conditions that are drawn as a sprite instead of a slot (the explosion); they still find the page */
    public record Layout(List<Slot> slots, List<Icon> icons, Ring ring, List<Integer> hidden) {
    }

    private static final int STAGE = 36, RESULT_X = 104, RESULT_CENTRE = 152, STEP = 26;

    private WorldScene() {
    }

    private static Component tip(String key) {
        return Component.translatable("gui.ae2_gvt.world.tip." + key);
    }

    public static Layout of(WorldInteraction w) {
        List<Slot> slots = new ArrayList<>();
        List<Icon> icons = new ArrayList<>();
        Ring ring = null;
        List<Integer> hidden = new ArrayList<>();
        int arrowY = (HEIGHT - Sprite.ARROW.h) / 2;
        int centre = STAGE - SLOT / 2;
        switch (w.scene()) {
            case DROP -> {
                slots.add(new Slot(Part.INPUT, 0, centre, 2));
                icons.add(new Icon(Sprite.DOWN, STAGE - 5, 22, null));
                slots.add(new Slot(Part.CONDITION, 0, centre, 36));
                column(slots, w.outputs().size());
            }
            case BLAST -> {
                icons.add(new Icon(Sprite.EXPLOSION, STAGE - 12, 0, tip("explosion")));
                hidden.add(0);
                int n = Math.min(3, w.inputs().size());
                int width = n * 20 - 2;
                for (int i = 0; i < n; i++)
                    slots.add(new Slot(Part.INPUT, i, STAGE - width / 2 + i * 20, 30));
                ring = new Ring(STAGE - width / 2 - 4, 26, width + 8, 26, tip("range"));
                column(slots, w.outputs().size());
            }
            case BLOWN -> {
                icons.add(new Icon(Sprite.EXPLOSION, STAGE - 12, 0, tip("explosion")));
                hidden.add(0);
                slots.add(new Slot(Part.INPUT, 0, centre, 32));
                slots.add(new Slot(Part.OUTPUT, 0, RESULT_CENTRE - SLOT / 2, 4));
                slots.add(new Slot(Part.OUTPUT, 1, RESULT_CENTRE - SLOT / 2 - 14, 34));
                icons.add(new Icon(Sprite.CHEVRON, RESULT_CENTRE - SLOT / 2 - 14 + SLOT + 2, 38, tip("collect")));
                slots.add(new Slot(Part.CONDITION, 1, RESULT_CENTRE - SLOT / 2 - 14 + SLOT + 12, 34));
            }
            case GROWTH -> {
                icons.add(new Icon(Sprite.ANY_ITEM, centre, 2, tip("any_item")));
                icons.add(new Icon(Sprite.DOWN, STAGE - 5, 22, null));
                slots.add(new Slot(Part.INPUT, 0, centre, 36));
                buds(slots, icons, w.outputs().size() - 1);
                slots.add(new Slot(Part.OUTPUT, 4, RESULT_X + 3 * STEP, 36));
                icons.add(new Icon(Sprite.PLUS, RESULT_X + 3 * STEP + 5, 27, tip("with")));
            }
            case SEED -> {
                slots.add(new Slot(Part.INPUT, 0, centre, 2));
                icons.add(new Icon(Sprite.DOWN, STAGE - 5, 22, null));
                slots.add(new Slot(Part.CONDITION, 0, centre - 21, 36));
                slots.add(new Slot(Part.INPUT, 1, centre, 36));
                slots.add(new Slot(Part.CONDITION, 1, centre + 21, 36));
                slots.add(new Slot(Part.OUTPUT, 0, RESULT_CENTRE - SLOT / 2, (HEIGHT - SLOT) / 2));
            }
            case BUDDING -> {
                slots.add(new Slot(Part.INPUT, 0, centre, 4));
                icons.add(new Icon(Sprite.DOWN, STAGE - 5, 23, tip("decay")));
                slots.add(new Slot(Part.OUTPUT, 4, centre, 36));
                buds(slots, icons, 4);
                arrowY = 6;
            }
        }
        icons.add(new Icon(Sprite.ARROW, 76, arrowY, w.text()));
        return new Layout(List.copyOf(slots), List.copyOf(icons), ring, List.copyOf(hidden));
    }

    /** up to three products in a column, centred */
    private static void column(List<Slot> slots, int count) {
        int n = Math.max(1, Math.min(3, count));
        int top = (HEIGHT - (n * 20 - 2)) / 2;
        for (int i = 0; i < Math.min(n, count); i++)
            slots.add(new Slot(Part.OUTPUT, i, RESULT_CENTRE - SLOT / 2, top + i * 20));
    }

    /** the four bud stages in a row, growing */
    private static void buds(List<Slot> slots, List<Icon> icons, int count) {
        for (int i = 0; i < count; i++) {
            slots.add(new Slot(Part.OUTPUT, i, RESULT_X + i * STEP, 4));
            if (i + 1 < count)
                icons.add(new Icon(Sprite.CHEVRON, RESULT_X + i * STEP + SLOT, 8, tip("grow")));
        }
    }

    /** the sprites and the ring (the slots are the screen's own) */
    public static void draw(GuiGraphics g, Layout l) {
        if (l.ring() != null) {
            Ring r = l.ring();
            dashed(g, r.x(), r.y(), r.w(), r.h(), 0xFF3E6FB0);
            g.drawString(Minecraft.getInstance().font, "1.5", r.x() + r.w() + 2, r.y() + r.h() - 9, 0xFF3E6FB0, false);
        }
        for (Icon i : l.icons())
            g.blit(TEXTURE, i.x(), i.y(), i.sprite().u, i.sprite().v, i.sprite().w, i.sprite().h);
    }

    private static void dashed(GuiGraphics g, int x, int y, int w, int h, int colour) {
        for (int i = 0; i < w; i++)
            if (i / 3 % 2 == 0) {
                g.fill(x + i, y, x + i + 1, y + 1, colour);
                g.fill(x + i, y + h - 1, x + i + 1, y + h, colour);
            }
        for (int i = 0; i < h; i++)
            if (i / 3 % 2 == 0) {
                g.fill(x, y + i, x + 1, y + i + 1, colour);
                g.fill(x + w - 1, y + i, x + w, y + i + 1, colour);
            }
    }

    /** the words for what the mouse is on (icons, then the ring), empty if it is on neither */
    public static List<Component> tooltip(Layout l, double mouseX, double mouseY) {
        for (Icon i : l.icons())
            if (i.tip() != null && mouseX >= i.x() && mouseX < i.x() + i.sprite().w && mouseY >= i.y() && mouseY < i.y() + i.sprite().h)
                return List.of(i.tip());
        Ring r = l.ring();
        if (r != null && mouseX >= r.x() && mouseX < r.x() + r.w() && mouseY >= r.y() && mouseY < r.y() + r.h())
            return List.of(r.tip());
        return List.of();
    }
}
