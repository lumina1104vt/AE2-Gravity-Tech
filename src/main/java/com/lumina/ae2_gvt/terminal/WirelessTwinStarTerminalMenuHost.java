package com.lumina.ae2_gvt.terminal;

import java.util.function.BiConsumer;

import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What the wireless twin-star terminal opens: AE2's wireless terminal host (the link to the network, the power it draws) with
 * the pattern encoding logic and what the terminal remembers, both saved into the item itself, so they stay with the terminal
 * wherever it is carried.
 */
public class WirelessTwinStarTerminalMenuHost extends WirelessTerminalMenuHost implements ITwinStarTermHost, IPatternTerminalLogicHost {
    private static final String KEY = "twin_star_terminal";

    private final PatternEncodingLogic logic = new PatternEncodingLogic(this);
    private final TwinStarTermState state = new TwinStarTermState(this::markForSave, this::isClientSide);
    private boolean loading;
    private final long openedAt;

    public WirelessTwinStarTerminalMenuHost(Player player, Integer slot, ItemStack stack, BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, slot, stack, returnToMainMenu);
        this.openedAt = player.level().getGameTime();
        CompoundTag tag = getItemStack().getTag();
        if (tag != null && tag.contains(KEY)) {
            CompoundTag saved = tag.getCompound(KEY);
            loading = true;
            try {
                logic.readFromNBT(saved);
                state.read(saved);
            } finally {
                loading = false;
            }
        }
    }

    /**
     * The terminal closes when it is no longer connected: out of power, out of reach, unlinked. Just opened, it is given a
     * moment to find out (its first tick has not run yet).
     */
    public boolean stillConnected() {
        return getPlayer().isCreative() || getPlayer().level().getGameTime() - openedAt < 20
                || (getActionableNode() != null && rangeCheck());
    }

    /**
     * What taking items out of the network costs is paid from the terminal's battery; a creative player may open the terminal with
     * an empty battery, so for one it costs nothing, or the list would show the items and not give any.
     */
    @Override
    public double extractAEPower(double amount, appeng.api.config.Actionable mode, appeng.api.config.PowerMultiplier multiplier) {
        if (getPlayer().isCreative())
            return amount;
        return super.extractAEPower(amount, mode, multiplier);
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return logic;
    }

    @Override
    public TwinStarTermState state() {
        return state;
    }

    @Override
    public Level getLevel() {
        return getPlayer().level();
    }

    @Override
    public void markForSave() {
        if (loading || isClientSide()) {
            return;     // the server's copy is the one that is kept
        }
        CompoundTag tag = new CompoundTag();
        logic.writeToNBT(tag);
        state.write(tag);
        getItemStack().getOrCreateTag().put(KEY, tag);
    }
}
