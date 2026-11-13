package com.lumina.ae2_gvt.terminal;

import java.util.function.BiConsumer;

import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.parts.encoding.PatternEncodingLogic;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * What the wireless twin-star terminal opens: AE2's wireless terminal host (the link to the network, the power it draws) with
 * the pattern encoding logic and what the terminal remembers, both saved into the item itself, so they stay with the terminal
 * wherever it is carried.
 */
public class WirelessTwinStarTerminalMenuHost<T extends WirelessTwinStarTerminalItem> extends WirelessTerminalMenuHost<T>
        implements ITwinStarTermHost, IPatternTerminalLogicHost {
    private static final String KEY = "twin_star_terminal";

    private final PatternEncodingLogic logic = new PatternEncodingLogic(this);
    private final TwinStarTermState state = new TwinStarTermState(this::markForSave, this::isClientSide);
    private boolean loading;
    private final long openedAt;

    public WirelessTwinStarTerminalMenuHost(T item, Player player, ItemMenuHostLocator locator, BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
        this.openedAt = player.level().getGameTime();
        var data = getItemStack().get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag saved = data.copyTag().getCompound(KEY);
            loading = true;
            try {
                var registries = player.level().registryAccess();
                logic.readFromNBT(saved, registries);
                state.read(saved, registries);
            } finally {
                loading = false;
            }
        }
    }

    /** Looks again at whether the terminal is connected (power, link, an access point in reach). */
    public void refreshLink() {
        updateConnectedAccessPoint();
        updateLinkStatus();
    }

    /**
     * The terminal closes when it is no longer connected: out of power, out of reach, unlinked. Just opened, it is given a
     * moment to find out (its first tick has not run yet).
     */
    @Override
    public boolean isValid() {
        return super.isValid() && (getPlayer().isCreative() || getItem().getAECurrentPower(getItemStack()) > 0)
                && (getLinkStatus().connected() || getPlayer().level().getGameTime() - openedAt < 20);
    }

    /**
     * What taking items out of the network costs is paid from the terminal's battery; a creative player may open the terminal with
     * an empty battery (see {@link #isValid}), so for one it costs nothing, or the list would show the items and not give any.
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
        var registries = getPlayer().level().registryAccess();
        CompoundTag tag = new CompoundTag();
        logic.writeToNBT(tag, registries);
        state.write(tag, registries);
        getItemStack().update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(root -> root.put(KEY, tag)));
    }
}
