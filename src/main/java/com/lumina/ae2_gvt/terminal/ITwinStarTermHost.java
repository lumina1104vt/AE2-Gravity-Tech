package com.lumina.ae2_gvt.terminal;

import appeng.helpers.IPatternTerminalMenuHost;

/** What the twin-star pattern terminal's menu needs of whatever it is opened from: the terminal part, or the wireless terminal. */
public interface ITwinStarTermHost extends IPatternTerminalMenuHost {
    /** What the terminal remembers besides AE2's pattern logic. */
    TwinStarTermState state();
}
