package com.lumina.ae2_gvt.items.tools;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.blockentities.MachineAccessBlockEntity;
import com.lumina.ae2_gvt.config.Config;
import com.lumina.ae2_gvt.items.parts.MachineAccessCore;
import com.lumina.ae2_gvt.items.parts.MachineAccessInterface;

import appeng.api.parts.IPartHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 機器連結器: sneak + right-click a 機器接入接口 to pick it, then right-click machines to bind them to
 * it (again to let go of one); one interface can hold many. Its right-click on a block comes before the
 * block's own, so a machine with a screen is linked, not opened. Sneak + right-click in the air forgets the
 * picked interface. While it is held the picked interface glows and its machines are drawn
 * ({@code client.LinkToolClient}).
 * <p>
 * Ctrl + right-click on a machine links every block of the same kind within a radius of it at once (it only adds; a plain
 * right click is still one machine, and again lets go of it). The radius and the most one click links are in the config
 * ({@code linkAllRadius}, {@code linkAllMax}).
 */
public class MachineLinkTool extends Item {
    public static final String KEY = "picked_interface";
    /** a click of this many ticks or less after the client said Ctrl was down is a Ctrl click */
    private static final int CTRL_TICKS = 5;
    private static final java.util.Map<java.util.UUID, Long> CTRL = new java.util.concurrent.ConcurrentHashMap<>();

    /** A place and a face: the interface part's block and side, or a machine's block and the face to use. */
    public record Link(ResourceKey<Level> dimension, BlockPos pos, Direction face) {
        public CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            t.putString("dim", dimension.location().toString());
            t.putInt("x", pos.getX());
            t.putInt("y", pos.getY());
            t.putInt("z", pos.getZ());
            t.putString("face", face.getName());
            return t;
        }

        public static @Nullable Link fromTag(CompoundTag t) {
            ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
            Direction face = Direction.byName(t.getString("face"));
            if (dim == null || face == null || !t.contains("x"))
                return null;
            return new Link(ResourceKey.create(Registries.DIMENSION, dim),
                    new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")), face);
        }
    }

    public MachineLinkTool(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static @Nullable Link getSelected(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null)
            return null;
        CompoundTag tag = data.copyTag();
        return tag.contains(KEY) ? Link.fromTag(tag.getCompound(KEY)) : null;
    }

    public static void setSelected(ItemStack stack, @Nullable Link link) {
        if (link == null)
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(KEY));
        else
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.put(KEY, link.toTag()));
    }

    /** The client says Ctrl is down for the click it sends next (see {@link com.lumina.ae2_gvt.payload.LinkAllPayload}). */
    public static void ctrlDown(net.minecraft.server.level.ServerPlayer player) {
        CTRL.put(player.getUUID(), player.serverLevel().getGameTime());
    }

    /** Whether this click came with Ctrl down; each Ctrl report is used once. */
    private static boolean takeCtrl(Player player, Level level) {
        Long at = CTRL.remove(player.getUUID());
        return at != null && level.getGameTime() - at <= CTRL_TICKS;
    }

    /** Picks {@code me} as the interface to set up (again: lets go of it), and says so. */
    public static void pick(ItemStack stack, Player player, Link me) {
        if (me.equals(getSelected(stack))) {
            setSelected(stack, null);
            player.displayClientMessage(Component.translatable("message.ae2_gvt.link_tool.deselected"), true);
        } else {
            setSelected(stack, me);
            player.displayClientMessage(Component.translatable("message.ae2_gvt.link_tool.selected",
                    me.pos().getX(), me.pos().getY(), me.pos().getZ()), true);
        }
    }

    /** The picked interface (a cable part, or the wireless block), when it is there. */
    public static @Nullable MachineAccessCore interfaceOf(Level level, @Nullable Link picked) {
        if (picked == null || !picked.dimension().equals(level.dimension()) || !level.isLoaded(picked.pos()))
            return null;
        var be = level.getBlockEntity(picked.pos());
        if (be instanceof MachineAccessBlockEntity block)
            return block.core();
        if (be instanceof IPartHost host && host.getPart(picked.face()) instanceof MachineAccessInterface part)
            return part.core();
        return null;
    }

    /**
     * Runs before the block's own right-click (a machine's screen), so the linker always works on what it
     * is pointed at. A cable with a part is left to the part, which picks itself when sneaking.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null)
            return InteractionResult.PASS;
        if (level.getBlockEntity(ctx.getClickedPos()) instanceof IPartHost host) {
            // sneaking skips the block's own use, so the part is picked here, not in its onUseItemOn
            var hit = host.selectPartWorld(ctx.getClickLocation());
            if (hit.part instanceof MachineAccessInterface && player.isShiftKeyDown()) {
                if (!level.isClientSide)
                    pick(stack, player, new Link(level.dimension(), ctx.getClickedPos(), hit.side));
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(ctx.getClickedPos()) instanceof MachineAccessBlockEntity) {
            if (!player.isShiftKeyDown())
                return InteractionResult.PASS;       // its screen
            if (!level.isClientSide)
                pick(stack, player, new Link(level.dimension(), ctx.getClickedPos(), Direction.UP));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            if (getSelected(stack) != null)
                com.lumina.ae2_gvt.client.LinkToolClient.reportCtrl();     // says to the server if Ctrl is down
        } else {
            boolean ctrl = takeCtrl(player, level);
            Link picked = getSelected(stack);
            MachineAccessCore part = interfaceOf(level, picked);
            Link target = new Link(level.dimension(), ctx.getClickedPos(), ctx.getClickedFace());
            if (part == null) {
                if (picked != null)
                    setSelected(stack, null);
                player.displayClientMessage(Component.translatable("message.ae2_gvt.link_tool.no_interface"), true);
            } else if (ctrl) {
                player.displayClientMessage(part.linkAllSame(target, Config.LINK_ALL_RADIUS.get(), Config.LINK_ALL_MAX.get()), true);
            } else {
                player.displayClientMessage(part.toggleLink(target), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && getSelected(stack) != null) {
            if (!level.isClientSide) {
                setSelected(stack, null);
                player.displayClientMessage(Component.translatable("message.ae2_gvt.link_tool.deselected"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Link picked = getSelected(stack);
        if (picked == null)
            lines.add(Component.translatable("tooltip.ae2_gvt.link_tool.empty"));
        else
            lines.add(Component.translatable("tooltip.ae2_gvt.link_tool.picked",
                    picked.pos().getX(), picked.pos().getY(), picked.pos().getZ()));
        lines.add(Component.translatable("tooltip.ae2_gvt.link_tool.usage"));
        lines.add(Component.translatable("tooltip.ae2_gvt.link_tool.ctrl").withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
