package com.lumina.twinstar.client.widget;

import appeng.client.gui.widgets.OpenGuideButton;
import appeng.client.gui.widgets.Scrollbar;
import com.lumina.twinstar.Twinstar;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Colours of the twin-star screens' dark widgets (ARGB), matching the black panels. */
public final class DarkStyle {
    public static final int TEXT = 0xFFDEDEEA;
    public static final int TEXT_DISABLED = 0xFF64647A;
    public static final int ERROR = 0xFFFF7474;

    public static final int FILL = 0xFF1E1C2C;
    public static final int EDGE = 0xFF4E4474;
    public static final int HIGHLIGHT = 0xFF322E48;
    public static final int SHADOW = 0xFF0E0C14;
    public static final int HOVER_FILL = 0xFF2C2642;
    public static final int HOVER_EDGE = 0xFFA082F0;
    public static final int DISABLED_FILL = 0xFF101016;
    public static final int DISABLED_EDGE = 0xFF282634;

    public static final int FIELD_FILL = 0xFF040408;

    public static final int TOGGLE_OFF_FILL = 0xFF100E18;
    public static final int TOGGLE_OFF_KNOB = 0xFF5A5478;
    public static final int TOGGLE_ON_FILL = 0xFF5C3CA0;
    public static final int TOGGLE_ON_EDGE = 0xFF6E5AAA;
    public static final int TOGGLE_ON_KNOB = 0xFFECE6FF;

    /** scrollbar handle sprites (textures/gui/sprites/scroller*.png) */
    public static final Scrollbar.Style SCROLLBAR = Scrollbar.Style.create(
            ResourceLocation.fromNamespaceAndPath(Twinstar.MODID, "scroller"),
            ResourceLocation.fromNamespaceAndPath(Twinstar.MODID, "scroller_disabled"));

    private DarkStyle() {
    }

    /** AE2 adds a guide button to every screen's left toolbar; the twin-star screens have no page. */
    public static void hideGuideButton(List<? extends GuiEventListener> children) {
        for (GuiEventListener child : children)
            if (child instanceof OpenGuideButton button)
                button.setVisibility(false);
    }
}
