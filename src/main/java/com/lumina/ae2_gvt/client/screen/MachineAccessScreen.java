package com.lumina.ae2_gvt.client.screen;

import java.util.List;
import java.util.Locale;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.LinkToolClient;
import com.lumina.ae2_gvt.items.parts.MachineAccessCore;
import com.lumina.ae2_gvt.items.tools.MachineLinkTool;
import com.lumina.ae2_gvt.menus.MachineAccessMenu;
import com.lumina.twinstar.api.IMachineAccess;
import com.lumina.twinstar.service.TwinStarService;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Machine access interface screen. Top: the machines it stands for (one cell each, a page at a time, click to
 * select, double click to light it up in the world) with each one's optional-machine slot under it, and the
 * mode switch. Bottom: the selected machine's, in provider mode what it is doing (its settings are the
 * left toolbar's), in ME interface mode its stock slots with the network's count under each (shift + right click
 * on the gear switches how the counts are written). Positions from assets/ae2/screens/machine_access_interface.json.
 */
public class MachineAccessScreen extends AEBaseScreen<MachineAccessMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/machine_access_interface.png");
    private static final int TEXT = 0x404040, MUTED = 0x808080, BUSY = 0x2E7D32, ERROR = 0xC03030, ACCENT = 0xFF2F86C8;
    private static final int BAR_X = 8, BAR_W = 160, BAR_H = 6;
    /** the machine cells: x of the first, y, spacing */
    private static final int CELL_X = 8, CELL_Y = 30, CELL = 18;
    private static final int NET_Y = 102, BAR_Y = 106, GEAR_Y = 68, STOCK_ROW_Y = 84, STOCK_LABEL_Y = 52, NET_LABEL_Y = 115;

    private final ModeButton modeButton = new ModeButton();
    private final PageButton prevPage = new PageButton(true);
    private final PageButton nextPage = new PageButton(false);
    private final GearButton[] gears = new GearButton[MachineAccessCore.STOCK_SLOTS];
    private final RecoverButton recoverButton = new RecoverButton();
    private final RecoverEnergyButton recoverEnergyButton = new RecoverEnergyButton();
    private final FacesButton facesButton = new FacesButton();
    private final BlockingButton blocking = new BlockingButton();
    private final InsertModeButton insert = new InsertModeButton();
    private final GroupButton groupButton = new GroupButton();
    private final RoleButton roleButton = new RoleButton();
    private final SlotButton slotButton = new SlotButton();
    private final SubRangeButton range = new SubRangeButton();
    private final PulseButton pulse = new PulseButton();
    /** what each slot's lower cell shows: false the amount it is set to keep, true the network's count of it */
    private final boolean[] aeCounts = new boolean[MachineAccessCore.STOCK_SLOTS];
    private long lastClickAt;
    private int lastClickCell = -1;

    public MachineAccessScreen(MachineAccessMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        for (int i = 0; i < gears.length; i++)
            gears[i] = new GearButton(i);
        addToLeftToolbar(modeButton);
        addToLeftToolbar(recoverButton);
        addToLeftToolbar(recoverEnergyButton);
        addToLeftToolbar(facesButton);
        addToLeftToolbar(blocking);
        addToLeftToolbar(insert);
        addToLeftToolbar(roleButton);
        addToLeftToolbar(slotButton);
        addToLeftToolbar(groupButton);
        addToLeftToolbar(range);
        addToLeftToolbar(pulse);
    }

    /** The mouse button of the click being handled, for buttons that go the other way on a right click. */
    private int lastMouseButton;
    private double lastMouseX, lastMouseY;
    /** the faces panel (opened by the faces button): a cube's net of big cells, each face on or off, any number of them */
    private boolean facesOpen;
    private static final int PANEL_X = 104, PANEL_Y = 58, CELL_BIG = 20;
    /** the net: column, row of each face's cell (the corners are empty), in Direction order: down, up, north, south, west, east */
    private static final int[][] NET = {{1, 2}, {1, 0}, {1, 1}, {2, 2}, {0, 1}, {2, 1}};

    @Override
    public void init() {
        super.init();
        addRenderableWidget(prevPage);
        addRenderableWidget(nextPage);
        for (GearButton gear : gears)
            addRenderableWidget(gear);
    }

    private boolean providerMode() {
        return menu.getAccessMode() == MachineAccessCore.Mode.PROVIDER;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        boolean provider = providerMode();
        boolean wireless = menu.getPart().isWireless();
        for (IconButton b : new IconButton[]{blocking, insert, groupButton, range, pulse})
            b.setVisibility(provider && !menu.isHatch());     // the machine's settings are the provider's; a hatch has none of them
        roleButton.setVisibility(provider && wireless);       // a bound block is a machine or a hatch of the machine before it
        slotButton.setVisibility(provider && wireless && menu.isNumbered());
        recoverButton.setVisibility(!provider);
        recoverEnergyButton.setVisibility(!provider && menu.recover);     // the FE a machine gives out, taken back as well
        facesButton.setVisibility(!provider && menu.recover);     // the ME interface's: take the output back, from which faces
        if (provider || !menu.recover)
            facesOpen = false;
        prevPage.setX(leftPos + 110);
        prevPage.setY(topPos + 7);
        nextPage.setX(leftPos + 152);
        nextPage.setY(topPos + 7);
        prevPage.setVisibility(menu.machinePages() > 1);
        nextPage.setVisibility(menu.machinePages() > 1);
        var stockSlots = menu.stockSlots();
        for (int i = 0; i < gears.length; i++) {          // AE2's gear over each stock slot that holds something
            gears[i].setX(leftPos + 8 + i * 18);
            gears[i].setY(topPos + GEAR_Y - 2);
            gears[i].setVisibility(!provider && GenericStack.fromItemStack(stockSlots.get(i).getItem()) != null);
        }
        for (int i = 0; i < stockSlots.size(); i++)       // a slot whose cell below shows the network's count does not write an amount itself
            stockSlots.get(i).setHideAmount(aeCounts[i]);
        menu.updateSlots();
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseButton = button;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        double lx = mouseX - leftPos, ly = mouseY - topPos;
        if (facesOpen) {
            Direction d = button == 0 ? facePanelCellAt(lx, ly) : null;
            if (d != null) {
                menu.toggleRecoverFace(d.ordinal());          // any number of faces can be on
                return true;
            }
            if (!facesButton.isMouseOver(mouseX, mouseY)) {   // a click anywhere else closes it
                facesOpen = false;
                return true;
            }
        }
        if (button == 0) {
            for (int i = 0; i < MachineAccessMenu.PER_PAGE; i++) {
                int index = menu.machinePage * MachineAccessMenu.PER_PAGE + i;
                if (index < menu.machineCount && inside(lx, ly, CELL_X + i * CELL, CELL_Y, 16, 16)) {
                    long now = System.currentTimeMillis();
                    if (lastClickCell == index && now - lastClickAt < 350)
                        highlight(index);                 // a double click lights the machine up in the world
                    else
                        menu.selectMachine(index);
                    lastClickCell = index;
                    lastClickAt = now;
                    return true;
                }
            }
        }
        // a click on the cell under a stock slot: that item as the network has it, a slot like any other
        if (!providerMode() && (button == 0 || button == 1)) {
            var slots = menu.stockSlots();
            for (int i = 0; i < slots.size(); i++) {
                if (inside(lx, ly, 8 + i * 18, NET_Y, 16, 16)) {
                    GenericStack g = GenericStack.fromItemStack(slots.get(i).getItem());
                    {
                        menu.netClick(i | (button == 1 ? 16 : 0) | (hasShiftDown() ? 32 : 0));
                        return true;
                    }
                }
            }
        }
        // pick-item click on a stock slot that holds something: the amount dialog, as AE2's interface has
        net.minecraft.world.inventory.Slot slot = this.hoveredSlot;
        if (minecraft.options.keyPickItem.matchesMouse(button) && slot != null
                && menu.getSlots(MachineAccessMenu.STOCK).contains(slot)) {
            GenericStack stack = GenericStack.fromItemStack(slot.getItem());
            if (stack != null) {
                switchToScreen(new SetStockAmountScreen(this, stack, slot));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderMachineTooltip(g, mouseX, mouseY);
    }

    /** Over a machine cell: its name, where it is, the machine it also stands for, and what a click does. */
    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.level == null)
            return;
        double lx = mouseX - leftPos, ly = mouseY - topPos;
        var part = menu.getPart();
        if (facesOpen) {
            Direction d = facePanelCellAt(lx, ly);
            if (d != null) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 500);
                g.renderTooltip(font, List.of(Component.translatable("gui.ae2_gvt.machine_access.recover_face",
                                Component.translatable("gui.ae2_gvt.machine_access.face." + d.getName())),
                        Component.translatable(menu.recoversFrom(d) ? "gui.ae2_gvt.machine_access.recover_face_on" : "gui.ae2_gvt.machine_access.recover_face_off")
                                .withStyle(ChatFormatting.GRAY)), java.util.Optional.empty(), mouseX, mouseY);
                g.pose().popPose();
                return;
            }
        }
        if (!providerMode()) {
            // over a lower cell: the item, what it is set to keep, and what the network has
            var slots = menu.stockSlots();
            for (int i = 0; i < slots.size(); i++) {
                GenericStack stack = GenericStack.fromItemStack(slots.get(i).getItem());
                if (stack != null && inside(lx, ly, 8 + i * 18, NET_Y, 16, 16)) {
                    g.renderTooltip(font, List.of(AEKeyRendering.getDisplayName(stack.what()),
                            Component.translatable("gui.ae2_gvt.machine_access.keep", String.format("%,d", stack.amount()))
                                    .withStyle(ChatFormatting.GRAY),
                            Component.translatable("gui.ae2_gvt.machine_access.in_network", String.format("%,d", menu.networkCount(i)))
                                    .withStyle(ChatFormatting.GRAY)), java.util.Optional.empty(), mouseX, mouseY);
                    return;
                }
            }
        }
        for (int i = 0; i < MachineAccessMenu.PER_PAGE; i++) {
            int index = menu.machinePage * MachineAccessMenu.PER_PAGE + i;
            if (index >= menu.machineCount)
                break;
            if (!inside(lx, ly, CELL_X + i * CELL, CELL_Y, 16, 16))
                continue;
            BlockPos pos = machinePos(index, part.host().pos(), part.host().side());
            List<Component> lines = new java.util.ArrayList<>();
            ItemStack icon = pos == null ? ItemStack.EMPTY : iconAt(minecraft.level, pos);
            lines.add(icon.isEmpty() ? Component.translatable(part.isWireless() ? "gui.ae2_gvt.machine_access.unloaded"
                    : "gui.ae2_gvt.machine_access.no_block") : icon.getHoverName());
            if (pos != null)
                lines.add(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ()).withStyle(ChatFormatting.GRAY));
            if (providerMode() && part.isWireless() && part.settings(index).getRole() != IMachineAccess.PortRole.MACHINE) {
                IMachineAccess.PortRole role = part.settings(index).getRole();
                int number = part.effectiveSlot(index);
                lines.add(Component.translatable("gui.ae2_gvt.machine_access.role." + role.name().toLowerCase(Locale.ROOT))
                        .withStyle(ChatFormatting.AQUA));
                if (number > 0)
                    lines.add(Component.translatable("gui.ae2_gvt.machine_access.slot_is", number).withStyle(ChatFormatting.GRAY));
            }
            var slot = providerMode() && index == menu.selected ? menu.machineSlot() : null;
            if (slot != null && !slot.getItem().isEmpty())
                lines.add(Component.translatable("gui.ae2_gvt.machine_access.also", slot.getItem().getHoverName()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.ae2_gvt.machine_access.cell_hint").withStyle(ChatFormatting.DARK_GRAY));
            g.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
    }

    /** Lights machine {@code index} up in the world for a few seconds (and the line to this interface). */
    private void highlight(int index) {
        var part = menu.getPart();
        MachineLinkTool.Link l = part.linkAt(index);
        BlockPos here = part.host().pos();
        Direction side = part.host().side();
        if (l != null) {
            LinkToolClient.flash(l.pos(), l.face(), here, side);
        } else if (here != null && side != null) {
            LinkToolClient.flash(here.relative(side), side.getOpposite(), here, side);
        }
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        if (providerMode()) {
            // a panel apart from the machine row: the slot of the machine it also stands for, what that is, and where the machine is
            g.fill(offsetX + 8, offsetY + 54, offsetX + 168, offsetY + 84, 0xFF9A9FB4);
            g.fill(offsetX + 9, offsetY + 55, offsetX + 167, offsetY + 83, 0xFFD3D4DD);
            g.fill(offsetX + 12, offsetY + 59, offsetX + 30, offsetY + 77, 0xFFF2F2F2);      // the cell: 1 px frame round the 16 x 16 slot
            g.fill(offsetX + 13, offsetY + 60, offsetX + 29, offsetY + 76, 0xFFADB0C4);
            g.fill(offsetX + 13, offsetY + 60, offsetX + 29, offsetY + 61, 0xFF9A9FB4);
            g.fill(offsetX + 8, offsetY + 88, offsetX + 169, offsetY + 89, 0xFF9A9FB4);
            g.fill(offsetX + 8, offsetY + 89, offsetX + 169, offsetY + 90, 0xFFF2F2F2);
            g.fill(offsetX + BAR_X, offsetY + BAR_Y, offsetX + BAR_X + BAR_W, offsetY + BAR_Y + BAR_H, 0xFF9A9FB4);
        } else {
            slotRow(g, offsetX + 7, offsetY + STOCK_ROW_Y - 1);   // the stock slots
            slotRow(g, offsetX + 7, offsetY + NET_Y - 1);         // and what the network has of each
        }
    }

    /** One row of nine slots in AE2's style (frame, panel, recessed cells) with its corner at {@code x}, {@code y}. */
    private static void slotRow(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 9 * 18 + 1, y + 19, 0xFFF2F2F2);
        g.fill(x + 1, y + 1, x + 9 * 18, y + 18, 0xFFCBCCD4);
        for (int i = 0; i < 9; i++) {
            g.fill(x + 1 + i * 18, y + 1, x + 17 + i * 18, y + 17, 0xFFADB0C4);
            g.fill(x + 1 + i * 18, y + 1, x + 17 + i * 18, y + 2, 0xFF9A9FB4);
        }
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        drawMachines(guiGraphics);
        if (providerMode())
            drawStatus(guiGraphics);
        else
            drawStock(guiGraphics, mouseX - leftPos, mouseY - topPos);
        if (facesOpen)
            drawFacesPanel(guiGraphics);
    }

    /** The cell of the faces panel under the point (relative to the screen's corner), or null. */
    private Direction facePanelCellAt(double lx, double ly) {
        int col = (int) Math.floor((lx - PANEL_X - 3) / CELL_BIG), row = (int) Math.floor((ly - PANEL_Y - 12) / CELL_BIG);
        if (lx - PANEL_X - 3 < 0 || ly - PANEL_Y - 12 < 0)
            return null;
        double inX = (lx - PANEL_X - 3) - col * CELL_BIG, inY = (ly - PANEL_Y - 12) - row * CELL_BIG;
        if (inX >= CELL_BIG - 2 || inY >= CELL_BIG - 2)
            return null;       // the gap between cells
        for (int i = 0; i < NET.length; i++)
            if (NET[i][0] == col && NET[i][1] == row)
                return Direction.values()[i];
        return null;
    }

    /** The faces panel: a cube's net, each face a big cell, lit when taken back from. Drawn over everything else. */
    private void drawFacesPanel(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        int w = 3 * CELL_BIG + 4, h = 3 * CELL_BIG + 15;
        g.fill(PANEL_X - 1, PANEL_Y - 1, PANEL_X + w + 1, PANEL_Y + h + 1, 0xFF413F54);
        g.fill(PANEL_X, PANEL_Y, PANEL_X + w, PANEL_Y + h, 0xFFCBCCD4);
        g.drawString(font, Component.translatable("gui.ae2_gvt.machine_access.recover_faces"), PANEL_X + 3, PANEL_Y + 2, TEXT, false);
        for (int i = 0; i < NET.length; i++) {
            int x = PANEL_X + 3 + NET[i][0] * CELL_BIG, y = PANEL_Y + 12 + NET[i][1] * CELL_BIG;
            boolean on = menu.recoversFrom(Direction.values()[i]);
            g.fill(x, y, x + CELL_BIG - 2, y + CELL_BIG - 2, 0xFF282C3C);
            g.fill(x + 2, y + 2, x + CELL_BIG - 4, y + CELL_BIG - 4, on ? 0xFFF4F9FF : 0xFF6E768C);
            if (on)       // a tick in the middle, so on does not depend on the brightness alone
                g.fill(x + 7, y + 7, x + 11, y + 11, 0xFF2F86C8);
        }
        g.pose().popPose();
    }

    /** The block at {@code pos}, as an item, or empty. */
    private ItemStack iconAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir())
            return ItemStack.EMPTY;
        ItemStack icon = new ItemStack(state.getBlock());
        return icon.is(Items.AIR) ? ItemStack.EMPTY : icon;
    }

    /** The listed machines, the page arrows, the selected one's name and the slot of each. */
    private void drawMachines(GuiGraphics g) {
        var part = menu.getPart();
        Level level = part.host().level();
        BlockPos hostPos = part.host().pos();
        Direction hostSide = part.host().side();
        g.drawString(font, Component.translatable("gui.ae2_gvt.machine_access.machines"), 8, 20, TEXT, false);
        if (menu.machinePages() > 1) {
            String n = (menu.machinePage + 1) + "/" + menu.machinePages();
            g.drawString(font, n, 140 - font.width(n) / 2, 13, TEXT, false);
        }
        for (int i = 0; i < MachineAccessMenu.PER_PAGE; i++) {
            int index = menu.machinePage * MachineAccessMenu.PER_PAGE + i;
            if (index >= menu.machineCount)
                break;
            BlockPos pos = machinePos(index, hostPos, hostSide);
            if (level != null && pos != null) {
                ItemStack icon = iconAt(level, pos);
                if (!icon.isEmpty())
                    g.renderItem(icon, CELL_X + i * CELL, CELL_Y);
            }
            if (providerMode() && part.isWireless()) {
                // a hatch: what it is for, small in the corner, and an input's slot number under it
                IMachineAccess.PortRole role = part.settings(index).getRole();
                if (role != IMachineAccess.PortRole.MACHINE) {
                    g.pose().pushPose();
                    g.pose().translate(CELL_X + i * CELL + 8, CELL_Y + 8, 150);
                    g.pose().scale(0.5f, 0.5f, 1f);
                    g.renderItem(roleIcon(role), 0, 0);
                    g.pose().popPose();
                    int number = part.effectiveSlot(index);
                    if (number > 0) {
                        String n = Integer.toString(number);
                        g.pose().pushPose();
                        g.pose().translate(CELL_X + i * CELL + 1, CELL_Y + 9, 200);
                        g.pose().scale(0.5f, 0.5f, 1f);
                        g.drawString(font, n, 0, 0, 0xFFFFFFFF, true);
                        g.pose().popPose();
                    }
                }
            }
            if (index == menu.selected) {       // the selected one: a frame round its cell
                int x = CELL_X + i * CELL - 1, y = CELL_Y - 1;
                g.fill(x, y, x + 18, y + 1, ACCENT);
                g.fill(x, y + 17, x + 18, y + 18, ACCENT);
                g.fill(x, y, x + 1, y + 18, ACCENT);
                g.fill(x + 17, y, x + 18, y + 18, ACCENT);
            }
        }
    }

    /** Where listed machine {@code index} is: a bound machine, or the block the part sits on. */
    private @org.jetbrains.annotations.Nullable BlockPos machinePos(int index, @org.jetbrains.annotations.Nullable BlockPos hostPos,
                                                                    @org.jetbrains.annotations.Nullable Direction hostSide) {
        MachineLinkTool.Link l = menu.getPart().linkAt(index);
        if (l != null)
            return l.pos();
        return hostPos != null && hostSide != null ? hostPos.relative(hostSide) : null;
    }

    /** ME interface mode, like AE2's: the config label, a gear over each slot that holds something, an arrow in each empty one,
     * and under them that item as the network has it. */
    private void drawStock(GuiGraphics g, int mx, int my) {
        g.drawString(font, Component.translatable("gui.ae2_gvt.machine_access.stock"), 8, STOCK_LABEL_Y, TEXT, false);
        var slots = menu.stockSlots();
        for (int i = 0; i < slots.size(); i++) {
            GenericStack stack = GenericStack.fromItemStack(slots.get(i).getItem());
            if (inside(mx, my, 8 + i * 18, NET_Y, 16, 16)) {
                g.pose().pushPose();      // a slot like any other: lit when pointed at
                g.pose().translate(0, 0, 150);
                g.fill(8 + i * 18, NET_Y, 8 + i * 18 + 16, NET_Y + 16, 0x80FFFFFF);
                g.pose().popPose();
            }
            if (stack == null) {
                Icon.WHITE_ARROW_DOWN.getBlitter().dest(8 + i * 18, STOCK_ROW_Y).blit(g);
                continue;
            }
            // the cell is the network's count of it (nothing written when it has none); the slot above says what to keep
            // by default no more than the amount above (its limit); after a shift + left click on its gear, all of it, and the slot above writes no amount
            long amount = aeCounts[i] ? menu.networkCount(i) : Math.min(menu.networkCount(i), stack.amount());
            if (amount <= 0)
                continue;
            AEKeyRendering.drawInGui(minecraft, g, 8 + i * 18, NET_Y, stack.what());
            String s = stack.what().formatAmount(amount, AmountFormat.SLOT);
            int shown = font.width(s);
            float scale = shown > 16 ? 16f / shown : 1f;
            g.pose().pushPose();
            g.pose().translate(8 + i * 18 + 16 - shown * scale, NET_Y + 16 - 8 * scale, 200);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, s, 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
    }

    private void drawStatus(GuiGraphics g) {
        var part = menu.getPart();
        var also = menu.machineSlot().getItem();
        if (also.isEmpty()) {      // an empty slot says what it is for: a plus, and the line next to it
            g.fill(17, 67, 25, 69, 0xFF8A8FA6);
            g.fill(20, 64, 22, 72, 0xFF8A8FA6);
        }
        g.drawString(font, Component.translatable("gui.ae2_gvt.machine_access.also_label"), 36, 60, TEXT, false);
        g.drawString(font, fit(also.isEmpty() ? Component.translatable("gui.ae2_gvt.machine_access.also_none") : also.getHoverName(), 128),
                36, 72, also.isEmpty() ? MUTED : TEXT, false);
        BlockPos pos = machinePos(menu.selected, part.host().pos(), part.host().side());
        MachineLinkTool.Link link = part.linkAt(menu.selected);
        Direction face = link != null ? link.face() : part.host().side() == null ? null : part.host().side().getOpposite();
        if (pos != null) {
            String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            g.drawString(font, coords, 166 - font.width(coords), 60, MUTED, false);
        }
        if (face != null) {
            Component f = Component.translatable("gui.ae2_gvt.machine_access.face." + face.getName());
            g.drawString(font, f, 166 - font.width(f), 72, MUTED, false);
        }
        TwinStarService.MachineState state = menu.state;
        boolean busy = state == TwinStarService.MachineState.FEEDING || state == TwinStarService.MachineState.WORKING;
        if (busy && menu.output != null) {
            AEKeyRendering.drawInGui(minecraft, g, 8, 92, menu.output.what());
            String count = state == TwinStarService.MachineState.FEEDING
                    ? Component.translatable("gui.ae2_gvt.machine_access.progress", menu.fed, menu.total).getString()
                    : Component.translatable("gui.ae2_gvt.machine_access.runs", menu.runsIn).getString();
            int countX = 168 - font.width(count);
            g.drawString(font, count, countX, 96, TEXT, false);
            g.drawString(font, fit(AEKeyRendering.getDisplayName(menu.output.what()), countX - 32), 28, 96, TEXT, false);
        }
        if (state == TwinStarService.MachineState.FEEDING && menu.total > 0) {
            int w = (int) Math.round(BAR_W * Math.min(1.0, (double) menu.fed / menu.total));
            g.fill(BAR_X, BAR_Y, BAR_X + w, BAR_Y + BAR_H, 0xFF4CAF50);
        }
        String key = switch (state) {
            case WORKING -> menu.chance ? "working_chance" : "working";
            default -> state.name().toLowerCase(Locale.ROOT);
        };
        int color = switch (state) {
            case INACTIVE, NO_HUB, NO_PATTERN -> ERROR;
            case FEEDING, WORKING -> BUSY;
            default -> MUTED;
        };
        g.drawString(font, fit(Component.translatable("gui.ae2_gvt.machine_access.state." + key), 160), 8, 116, color, false);
    }

    /** The text cut to {@code width} pixels, with an ellipsis when cut. */
    private String fit(Component text, int width) {
        String s = text.getString();
        if (font.width(s) <= width)
            return s;
        return font.plainSubstrByWidth(s, width - font.width("…")) + "…";
    }

    /**
     * AE2's own gear over a stock slot (its set-amount button: the cog lit when pointed at, no toolbar background, the click
     * sound): left click sets the amount, shift + left click switches what the cell under the slot shows.
     */
    private final class GearButton extends IconButton {
        private final int slot;

        GearButton(int slot) {
            super(btn -> {
            });
            this.slot = slot;
            setDisableBackground(true);
        }

        @Override
        protected Icon getIcon() {
            return isHoveredOrFocused() ? Icon.COG : Icon.COG_DISABLED;
        }

        @Override
        public void onPress() {
            var slots = menu.stockSlots();
            GenericStack held = GenericStack.fromItemStack(slots.get(slot).getItem());
            if (held == null)
                return;
            if (hasShiftDown())
                aeCounts[slot] = !aeCounts[slot];
            else
                switchToScreen(new SetStockAmountScreen(MachineAccessScreen.this, held, slots.get(slot)));
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.gear"),
                    Component.translatable("gui.ae2_gvt.machine_access.gear_hint").withStyle(ChatFormatting.GRAY));
        }
    }

    /** ME interface mode: whether it takes back what the machine gives out. */
    private final class RecoverButton extends IconButton {
        RecoverButton() {
            super(btn -> menu.toggleRecover());
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the icon is ours
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            Blitter.texture(TEXTURE).src(192 + (menu.recover ? 0 : 16), 68, 16, 16).dest(getX(), getY() + 1 + (isHovered() ? 1 : 0)).zOffset(3).blit(g);
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.recover"),
                    Component.translatable(menu.recover ? "gui.ae2_gvt.machine_access.recover_on" : "gui.ae2_gvt.machine_access.recover_off")
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    /** ME interface mode: whether the FE a machine gives out is taken back into the network too. */
    private final class RecoverEnergyButton extends IconButton {
        RecoverEnergyButton() {
            super(btn -> menu.toggleRecoverEnergy());
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the FE icon is drawn on it
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            int y = getY() + (isHovered() ? 1 : 0);
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            g.renderItem(new ItemStack(com.lumina.ae2_gvt.storage.FeEnergy.ICON.get()), getX(), y);
            if (!menu.recoverEnergy)
                g.fill(getX(), y, getX() + 16, y + 16, 0xB0C6C6C6);      // greyed out: off
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.recover_energy"),
                    Component.translatable(menu.recoverEnergy ? "gui.ae2_gvt.machine_access.recover_energy_on" : "gui.ae2_gvt.machine_access.recover_energy_off")
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * The faces to take the output back from, in one button: it opens a panel of big cells to pick any number of faces. The button
     * itself shows the net small (3 x 3 cells, centred) with the faces that are on lit.
     */
    private final class FacesButton extends IconButton {
        FacesButton() {
            super(btn -> facesOpen = !facesOpen);
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the net is drawn on it
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            int ox = getX() + 2, oy = getY() + 2 + (isHovered() ? 1 : 0);
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            for (int i = 0; i < NET.length; i++) {
                int x = ox + NET[i][0] * 4, y = oy + NET[i][1] * 4;
                g.fill(x, y, x + 4, y + 4, 0xFF282C3C);
                g.fill(x + 1, y + 1, x + 3, y + 3, menu.recoversFrom(Direction.values()[i]) ? 0xFFF4F9FF : 0xFF6E768C);
            }
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.recover_faces"),
                    Component.translatable("gui.ae2_gvt.machine_access.recover_faces_hint").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The mode: the item of the other kind of AE2 interface it acts like (a pattern provider, or an ME interface). */
    private final class ModeButton extends IconButton {
        ModeButton() {
            super(btn -> menu.setAccessMode((providerMode() ? MachineAccessCore.Mode.INTERFACE : MachineAccessCore.Mode.PROVIDER).ordinal()));
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the item is drawn on it
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            ItemStack icon = (providerMode() ? appeng.core.definitions.AEParts.PATTERN_PROVIDER : appeng.core.definitions.AEParts.INTERFACE).stack();
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            g.renderItem(icon, getX(), getY() + (isHovered() ? 1 : 0));
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            String mode = providerMode() ? "provider" : "interface";
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.mode"),
                    Component.translatable("gui.ae2_gvt.machine_access.mode." + mode).withStyle(ChatFormatting.WHITE),
                    Component.translatable("gui.ae2_gvt.machine_access.mode." + mode + ".desc").withStyle(ChatFormatting.GRAY));
        }
    }

    /** AE2's arrows: the previous or next page of listed machines. */
    private final class PageButton extends IconButton {
        private final boolean back;

        PageButton(boolean back) {
            super(btn -> menu.stepMachinePage(back));
            this.back = back;
        }

        @Override
        protected Icon getIcon() {
            return back ? Icon.ARROW_LEFT : Icon.ARROW_RIGHT;
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.page." + (back ? "prev" : "next")));
        }
    }

    /** AE2's blocking mode icons. */
    private final class BlockingButton extends IconButton {
        BlockingButton() {
            super(btn -> menu.toggleBlocking());
        }

        @Override
        protected Icon getIcon() {
            return menu.getBlockMode() == IMachineAccess.BlockMode.NONE ? Icon.BLOCKING_MODE_NO : Icon.BLOCKING_MODE_YES;
        }

        /** Full blocking: AE2's blocking icon with a "1" in its corner (one run at a time). */
        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible || menu.getBlockMode() != IMachineAccess.BlockMode.FULL)
                return;
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            g.drawString(font, "1", getX() + 11, getY() + 9 + (isHovered() ? 1 : 0), 0xFFFFFFFF, true);
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            String mode = switch (menu.getBlockMode()) {
                case NONE -> "blocking_off";
                case BLOCKING -> "blocking_on";
                case FULL -> "blocking_full";
            };
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.blocking"),
                    Component.translatable("gui.ae2_gvt.machine_access." + mode).withStyle(ChatFormatting.GRAY));
        }
    }

    /** What a role looks like: an item that does the same (no letters or colours to tell them by). */
    private static ItemStack roleIcon(IMachineAccess.PortRole role) {
        return switch (role) {
            case MACHINE -> appeng.core.definitions.AEBlocks.MOLECULAR_ASSEMBLER.stack();
            case ITEM_IN -> new ItemStack(Items.HOPPER);
            case FLUID_IN -> new ItemStack(Items.BUCKET);
            case OUTPUT -> new ItemStack(Items.DROPPER);
            case ENERGY -> appeng.core.definitions.AEBlocks.ENERGY_CELL.stack();
        };
    }

    /** What the selected bound block is: a machine, or a hatch of the machine before it (left click next, right click the one before). */
    private final class RoleButton extends IconButton {
        RoleButton() {
            super(btn -> {
            });
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the item is drawn on it
        }

        @Override
        public void onPress() {
            menu.cycleRole(lastMouseButton == 1);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            g.renderItem(roleIcon(menu.getRole()), getX(), getY() + (isHovered() ? 1 : 0));
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            String role = menu.getRole().name().toLowerCase(Locale.ROOT);
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.role"),
                    Component.translatable("gui.ae2_gvt.machine_access.role." + role).withStyle(ChatFormatting.WHITE),
                    Component.translatable("gui.ae2_gvt.machine_access.role." + role + ".desc").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.ae2_gvt.machine_access.role_click").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * An input hatch's slot number, the one a pattern's material slot names: left click one more, right click one less, shift
     * ten at a time; 0 goes by the hatch's place among the machine's hatches of its kind. A line under the number says it was set by hand.
     */
    private final class SlotButton extends IconButton {
        SlotButton() {
            super(btn -> {
            });
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the number is drawn on it
        }

        @Override
        public void onPress() {
            menu.stepSlot((lastMouseButton == 1 ? -1 : 1) * (hasShiftDown() ? 10 : 1));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            String number = Integer.toString(menu.slotNow);
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            int y = getY() + 5 + (isHovered() ? 1 : 0);
            g.drawString(font, number, getX() + (16 - font.width(number)) / 2, y, 0xFFFFFFFF, true);
            if (menu.slot > 0)
                g.fill(getX() + 3, y + 9, getX() + 13, y + 10, 0xFFFFFFFF);
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.slot"),
                    Component.translatable(menu.slot > 0 ? "gui.ae2_gvt.machine_access.slot_set" : "gui.ae2_gvt.machine_access.slot_auto",
                            menu.slotNow).withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.ae2_gvt.machine_access.slot_click").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** The pairing number on the button: a machine spread over several blocks. */
    private final class GroupButton extends IconButton {
        GroupButton() {
            super(btn -> menu.cycleGroup());
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the number is drawn on it
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            String number = menu.group == 0 ? "-" : Integer.toString(menu.group);
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            g.drawString(font, number, getX() + (16 - font.width(number)) / 2, getY() + 5 + (isHovered() ? 1 : 0), 0xFFFFFFFF, true);
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.group"),
                    Component.translatable(menu.group == 0 ? "gui.ae2_gvt.machine_access.group_off" : "gui.ae2_gvt.machine_access.group_on", menu.group)
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    /** AE2's redstone icons: a pulse once a run is all in, or no signal. */
    private final class PulseButton extends IconButton {
        PulseButton() {
            super(btn -> menu.toggleRedstonePulse());
        }

        @Override
        protected Icon getIcon() {
            return menu.redstonePulse ? Icon.REDSTONE_PULSE : Icon.REDSTONE_IGNORE;
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.pulse"),
                    Component.translatable(menu.redstonePulse ? "gui.ae2_gvt.machine_access.pulse_on"
                            : "gui.ae2_gvt.machine_access.pulse_off").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The sub-machine range on the button: left click one more, right click one less. */
    private final class SubRangeButton extends IconButton {
        SubRangeButton() {
            super(btn -> {
            });
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the number is drawn on it
        }

        /** AE2 presses toolbar buttons itself on a right click too: which button it was comes from the screen. */
        @Override
        public void onPress() {
            menu.cycleSubRange(lastMouseButton == 1);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            String number = Integer.toString(menu.subRange);
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            // a small ring of dots round the number: "around the machine"
            int cx = getX() + 8, cy = getY() + 8 + (isHovered() ? 1 : 0);
            for (int[] d : new int[][]{{-6, -6}, {0, -7}, {5, -6}, {-7, 0}, {6, 0}, {-6, 5}, {0, 6}, {5, 5}})
                g.fill(cx + d[0], cy + d[1], cx + d[0] + 1, cy + d[1] + 1, 0xFFB0B0C8);
            g.drawString(font, number, getX() + (16 - font.width(number)) / 2 + 1, getY() + 5 + (isHovered() ? 1 : 0), 0xFFFFFFFF, true);
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.sub_range"),
                    Component.translatable("gui.ae2_gvt.machine_access.sub_range_on", menu.subRange).withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.ae2_gvt.machine_access.sub_range_click").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Our own icons (16x16 at the right of the background texture): default, specified, any, output only. */
    private final class InsertModeButton extends IconButton {
        InsertModeButton() {
            super(btn -> menu.cycleInsertMode());
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the icon is ours
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            int u = 176 + 16 * menu.getInsertMode().ordinal();
            Blitter.texture(TEXTURE).src(u, 0, 16, 16).dest(getX(), getY() + 1 + (isHovered() ? 1 : 0)).zOffset(3).blit(g);
        }

        @Override
        public List<Component> getTooltipMessage() {
            String mode = menu.getInsertMode().name().toLowerCase(Locale.ROOT);
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.insert"),
                    Component.translatable("gui.ae2_gvt.machine_access.insert." + mode).withStyle(ChatFormatting.GRAY));
        }
    }
}
