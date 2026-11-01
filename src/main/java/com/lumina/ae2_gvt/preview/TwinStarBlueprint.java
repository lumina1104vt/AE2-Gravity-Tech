package com.lumina.ae2_gvt.preview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector3f;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.StructureFrameBlock;
import com.lumina.ae2_gvt.hub.HubUnits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * PROTOTYPE - the Twin-Star Hub layout, read straight from the structure {@code ae2_gvt:calculator}:
 * the mod's data/ae2_gvt/structure/calculator.nbt, or - if a structure block saved one under the
 * same name in the world (generated/ae2_gvt/structure) - that copy, which the structure manager
 * prefers. Re-saving the structure takes effect on the next check; nothing about the layout is
 * hard-coded:
 * <ul>
 *   <li>controller: the template's own preview blocks if it contains them, otherwise the lowest
 *       empty 2x2x2 on the template's vertical centre axis</li>
 *   <li>sphere centre and unit-shell radius: the unit cells' box plus the glass layer round it
 *       (or, for an empty template, the tinted / containment glass's bounding box)</li>
 *   <li>hidden while formed: everything in the upper sphere (from a little below the glass up);
 *       the base stays as built</li>
 *   <li>units: NOT matched block by block. The unit cells are the cells the template fills with
 *       units (or, if it has none, every cell inside its glass shell, by flood fill). Each must
 *       hold a hub unit - any tier or side, blank included - with exactly one black hole core and
 *       one white hole core among them</li>
 * </ul>
 * Checking goes in two steps: first the 2x2x2 controller must be complete, then the rest is
 * matched around the controller's centre. Every non-air template block must match by type; air is
 * "don't care". The layout is 4-fold symmetric about that centre, so it can face any direction.
 */
final class TwinStarBlueprint {
    /** The hub structure (structure block name); a copy saved in the world overrides the bundled one. */
    static final ResourceLocation STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "calculator");

    /**
     * {@code offset} is relative to the controller centre; {@code state} is the template's block
     * state (restored exactly after being hidden); {@code hidden}: swapped for a stand-in while formed.
     */
    record Entry(BlockPos offset, BlockState state, boolean hidden, boolean unitSlot) {
        Block block() {
            return state.getBlock();
        }
    }

    /**
     * Everything derived from the template. {@code sphereCentre} is relative to the controller's
     * min corner (the master cell), which is where the renderer draws from.
     */
    record Layout(List<Entry> entries, Vector3f sphereCentre, float shellRadius) {
        static final Layout EMPTY = new Layout(List.of(), new Vector3f(1, 24, 1), 11.9f);
    }

    record Result(boolean controllerComplete, boolean formed, int mismatches, int total, BlockPos firstBad, Block expected,
                  int blackCores, int whiteCores) {
        static final Result NO_CONTROLLER = new Result(false, false, 0, 0, null, null, 0, 0);

        /** Every block matched, but the cores are not one of each. */
        boolean wrongCores() {
            return mismatches == 0 && total > 0 && (blackCores != 1 || whiteCores != 1);
        }
    }

    /** Content of the template the cached layout was built from (re-saving updates the same object in place). */
    private static CompoundTag cachedTag;
    private static StructureTemplate cachedTemplate;
    private static Layout cachedLayout = Layout.EMPTY;

    private TwinStarBlueprint() {
    }

    /**
     * The current layout. Reading the template's content back (save + compare, ~120 KB of NBT) is
     * far too slow for every check, so it only happens on {@link #refresh} - a right-click on the
     * controller, which is what you do after re-saving the structure - or when the structure
     * manager hands out a different template object (a resource reload).
     */
    static Layout layout(ServerLevel level) {
        return layout(level, false);
    }

    /** Like {@link #layout(ServerLevel)}, re-reading the template's content to pick up a re-save. */
    static Layout refresh(ServerLevel level) {
        return layout(level, true);
    }

    private static Layout layout(ServerLevel level, boolean reread) {
        Optional<StructureTemplate> template = level.getStructureManager().get(STRUCTURE_ID);
        if (template.isEmpty()) {
            if (cachedTag != null) {
                AE2GravityTech.LOGGER.error("[PROTOTYPE] structure {} not found", STRUCTURE_ID);
            }
            cachedTag = null;
            cachedTemplate = null;
            cachedLayout = Layout.EMPTY;
            return cachedLayout;
        }
        if (!reread && template.get() == cachedTemplate) {
            return cachedLayout;
        }
        cachedTemplate = template.get();
        // StructureTemplate has no "all blocks" accessor, so read its own saved form
        CompoundTag tag = template.get().save(new CompoundTag());
        if (!tag.equals(cachedTag)) {
            cachedTag = tag;
            cachedLayout = derive(tag, template.get().getSize());
        }
        return cachedLayout;
    }

    private static Layout derive(CompoundTag tag, Vec3i size) {
        ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag info = blocks.getCompound(i);
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(info.getInt("state")));
            if (!state.isAir() && !state.is(Blocks.STRUCTURE_VOID)) {
                ListTag pos = info.getList("pos", Tag.TAG_INT);
                states.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), state);
            }
        }
        Map<BlockPos, Block> solid = new HashMap<>();
        states.forEach((p, s) -> solid.put(p, s.getBlock()));

        BlockPos controllerMin = findTemplateController(solid, size);
        if (controllerMin == null) {
            AE2GravityTech.LOGGER.error("[PROTOTYPE] structure has no controller spot (preview blocks, or an empty 2x2x2 on the centre axis)");
            return Layout.EMPTY;
        }
        BlockPos controllerCentre = controllerMin.offset(1, 1, 1);

        // The unit cells are NOT matched block by block - any unit goes anywhere in them (the
        // check only asks that every cell holds a unit and the cores are one of each), so how the
        // template happens to be filled (empty, or saved full of blank units) doesn't matter:
        //  - every cell the template fills with a unit (or core) is a unit cell;
        //  - if it has none, the cells inside its glass shell (tinted or containment glass) are,
        //    found by flood fill from the centre.
        java.util.Set<BlockPos> units = new java.util.HashSet<>();
        solid.forEach((p, b) -> {
            if (HubUnits.isUnit(b)) {
                units.add(p);
            }
        });
        // the sphere's box: the units grown by the glass layer round them, else the glass itself
        int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] max = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        int grow = units.isEmpty() ? 0 : 1;
        solid.forEach((p, b) -> {
            if (units.isEmpty() ? b == Blocks.TINTED_GLASS || b == com.lumina.ae2_gvt.blocks.Blocks.CONTAINMENT_GLASS.get()
                    : units.contains(p)) {
                int[] c = {p.getX(), p.getY(), p.getZ()};
                for (int k = 0; k < 3; k++) {
                    min[k] = Math.min(min[k], c[k] - grow);
                    max[k] = Math.max(max[k], c[k] + grow);
                }
            }
        });
        Vector3f centre;
        float shell;
        if (min[0] == Integer.MAX_VALUE) {
            centre = new Vector3f(size.getX() / 2f, size.getY() / 2f, size.getZ() / 2f);
            shell = Layout.EMPTY.shellRadius();
        } else {
            centre = new Vector3f((min[0] + max[0] + 1) / 2f, (min[1] + max[1] + 1) / 2f, (min[2] + max[2] + 1) / 2f);
            shell = (max[1] - min[1] + 1) / 2f - 0.1f;
        }
        // the sphere's frame reaches a few blocks beyond the glass; below that is the base
        float sphereBottom = centre.y - shell - 4.5f;

        if (units.isEmpty() && min[0] != Integer.MAX_VALUE) {
            java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
            BlockPos start = BlockPos.containing(centre.x, centre.y, centre.z);
            queue.add(start);
            units.add(start);
            while (!queue.isEmpty()) {
                BlockPos p = queue.poll();
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                    BlockPos n = p.relative(d);
                    boolean inBox = n.getX() >= min[0] && n.getX() <= max[0] && n.getY() >= min[1] && n.getY() <= max[1]
                            && n.getZ() >= min[2] && n.getZ() <= max[2];
                    if (inBox && solid.get(n) == null && units.add(n)) {
                        queue.add(n);
                    }
                }
            }
            if (units.size() > 20000) {   // the shell has a hole: the fill escaped
                AE2GravityTech.LOGGER.error("[PROTOTYPE] the glass shell is not closed; no unit volume");
                units.clear();
            }
        }

        List<Entry> entries = new ArrayList<>();
        states.forEach((p, s) -> {
            BlockPos fromMin = p.subtract(controllerMin);
            if (isControllerCell(fromMin) || units.contains(p)) {
                return;   // the controller is checked separately; unit cells are added below
            }
            // formed: the whole upper sphere gives way to the drawn effect; the base stays
            entries.add(new Entry(p.subtract(controllerCentre), s, p.getY() >= sphereBottom, false));
        });
        BlockState blank = HubUnits.BLANK.get().getBlock().defaultBlockState();
        for (BlockPos p : units) {
            entries.add(new Entry(p.subtract(controllerCentre), blank, true, true));
        }
        Vector3f fromMaster = new Vector3f(centre).sub(controllerMin.getX(), controllerMin.getY(), controllerMin.getZ());
        AE2GravityTech.LOGGER.info("[PROTOTYPE] hub layout: {} blocks ({} of them units), controller at {}, sphere centre {} (shell r={}) from the controller",
                entries.size(), units.size(), controllerMin, fromMaster, shell);
        return new Layout(List.copyOf(entries), fromMaster, shell);
    }

    /** The template's own controller cells if it has them, else the lowest empty 2x2x2 on the centre axis. */
    private static BlockPos findTemplateController(Map<BlockPos, Block> solid, Vec3i size) {
        BlockPos marked = null;
        for (Map.Entry<BlockPos, Block> e : solid.entrySet()) {
            if (e.getValue() == TwinStarPreview.BLOCK.get()) {
                BlockPos p = e.getKey();
                marked = marked == null ? p : new BlockPos(Math.min(marked.getX(), p.getX()), Math.min(marked.getY(), p.getY()), Math.min(marked.getZ(), p.getZ()));
            }
        }
        if (marked != null) {
            return marked;
        }
        int cx = size.getX() / 2 - 1, cz = size.getZ() / 2 - 1;
        for (int y = 0; y + 1 < size.getY(); y++) {
            boolean empty = true;
            for (int dx = 0; dx <= 1 && empty; dx++) {
                for (int dy = 0; dy <= 1 && empty; dy++) {
                    for (int dz = 0; dz <= 1 && empty; dz++) {
                        empty = !solid.containsKey(new BlockPos(cx + dx, y + dy, cz + dz));
                    }
                }
            }
            if (empty) {
                return new BlockPos(cx, y, cz);
            }
        }
        return null;
    }

    private static boolean isControllerCell(BlockPos fromMin) {
        return fromMin.getX() >= 0 && fromMin.getX() <= 1 && fromMin.getY() >= 0 && fromMin.getY() <= 1
                && fromMin.getZ() >= 0 && fromMin.getZ() <= 1;
    }

    /**
     * The min corner of the complete 2x2x2 controller that {@code pos} belongs to, or null if the
     * eight cells around it aren't all preview blocks.
     */
    static BlockPos findControllerMin(Level level, BlockPos pos) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    BlockPos min = pos.offset(-dx, -dy, -dz);
                    if (isCompleteController(level, min)) {
                        return min;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isCompleteController(Level level, BlockPos min) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    if (!level.getBlockState(min.offset(x, y, z)).is(TwinStarPreview.BLOCK.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // a hidden block's stand-in counts as the block it stands in for; unit cells take any unit
    private static boolean matches(BlockState state, Entry e) {
        if (e.unitSlot()) {
            return HubUnits.isUnit(unitOf(state));
        }
        return state.is(e.block()) || e.hidden() && state.is(TwinStarPreview.FRAME.get());
    }

    /** The unit block in this cell, looking through a stand-in; air if it is neither. */
    private static Block unitOf(BlockState state) {
        if (state.is(TwinStarPreview.FRAME.get())) {
            return PrototypeHubFrameBlock.source(state);
        }
        return state.getBlock();
    }

    /**
     * Swap the hidden blocks for invisible stand-ins and switch the visible structure frames to
     * their formed look, or back again. Only touches blocks that are
     * currently the expected type / a stand-in, so nothing else in the world is ever replaced.
     */
    static void setFrameHidden(ServerLevel level, BlockPos controllerMin, boolean hidden) {
        BlockPos centre = controllerMin.offset(1, 1, 1);
        Block frame = TwinStarPreview.FRAME.get();
        for (Entry e : layout(level).entries()) {
            BlockPos p = centre.offset(e.offset());
            if (!level.isLoaded(p)) {
                continue;
            }
            BlockState now = level.getBlockState(p);
            if (hidden) {
                if (e.unitSlot() ? HubUnits.isUnit(now.getBlock()) : e.hidden() && now.is(e.block())) {
                    level.setBlock(p, PrototypeHubFrameBlock.standInFor(frame, now), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                } else if (!e.hidden() && now.getBlock() instanceof StructureFrameBlock && !now.getValue(StructureFrameBlock.FORMED)) {
                    level.setBlock(p, now.setValue(StructureFrameBlock.FORMED, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            } else if (now.is(frame)) {
                // units come back as the exact unit the stand-in remembers; the rest from the template
                BlockState back = e.unitSlot() ? PrototypeHubFrameBlock.source(now).defaultBlockState() : e.state();
                level.setBlock(p, back, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            } else if (now.getBlock() instanceof StructureFrameBlock && now.getValue(StructureFrameBlock.FORMED)) {
                level.setBlock(p, now.setValue(StructureFrameBlock.FORMED, false), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    /** Check the structure around a complete controller whose min corner is {@code controllerMin}. */
    static Result check(ServerLevel level, BlockPos controllerMin) {
        BlockPos centre = controllerMin.offset(1, 1, 1);
        List<Entry> all = layout(level).entries();
        int bad = 0, black = 0, white = 0;
        BlockPos firstBad = null;
        Block expected = null;
        Block blackCore = HubUnits.BLACK_HOLE_CORE.get().getBlock(), whiteCore = HubUnits.WHITE_HOLE_CORE.get().getBlock();
        for (Entry e : all) {
            BlockPos p = centre.offset(e.offset());
            BlockState state = level.isLoaded(p) ? level.getBlockState(p) : null;
            if (state == null || !matches(state, e)) {
                if (bad++ == 0) {
                    firstBad = p;
                    expected = e.block();
                }
            } else if (e.unitSlot()) {
                Block unit = unitOf(state);
                if (unit == blackCore) {
                    black++;
                } else if (unit == whiteCore) {
                    white++;
                }
            }
        }
        boolean formed = !all.isEmpty() && bad == 0 && black == 1 && white == 1;
        return new Result(true, formed, bad, all.size(), firstBad, expected, black, white);
    }
}
