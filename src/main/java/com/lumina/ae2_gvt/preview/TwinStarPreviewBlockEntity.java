package com.lumina.ae2_gvt.preview;

import org.joml.Quaterniond;

import org.joml.Vector3d;


import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PROTOTYPE - one of the eight blocks of the 2x2x2 controller. Only the controller's min corner
 * (lowest north-west cell) acts as the master: it checks the structure on the server (every 2 s and
 * on right-click), syncs the "formed" flag to clients and draws the animation; the other seven stay
 * idle. The animation only shows while formed, and replays the moment the structure completes.
 * <p>
 * Motion follows the Lightning Tech approach: each moving part has a phase advanced every frame
 * at a rate blended between an idle and a working speed, and the blend ("activity") eases toward
 * its target so the gyroscope spins up / winds down smoothly instead of jumping.
 */
public class TwinStarPreviewBlockEntity extends BlockEntity implements InternalInventoryHost {
    /** Never replayed: show the steady formed state straight away. */
    static final long NOT_REPLAYED = Long.MIN_VALUE;

    /** Structure complete (server-checked, synced to clients). */
    public boolean formed;
    private int checkCooldown;

    /** Sphere centre (from this master cell) and unit-shell radius, derived from the saved structure. */
    public float sphereX = 1, sphereY = 24, sphereZ = 1, shellRadius = 11.9f;

    /** The hub's stored patterns (kept on the master cell only). Not sent with block updates; the GUI syncs them. */
    private static final int PATTERN_SLOTS = 72;
    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, PATTERN_SLOTS, 1);
    private boolean savingForSync;

    public long formedAt = NOT_REPLAYED;
    public boolean busy;

    // animation clock (client only; public so the client renderer can read it - prototype)
    public double lastTime = Double.NaN;
    public double activity;
    public double outerAngle, middleAngle, innerAngle;   // gimbal, degrees
    public double orbitAngle;                            // twin holes: angle around their orbit (deg)
    /** Orientation of the twin holes' orbit plane; tumbles freely so the orbit axis keeps changing. */
    public final Quaterniond orbitFrame = new Quaterniond();
    private double tumbleClock;
    public double twinClock;                              // seconds into the merge cycle

    public TwinStarPreviewBlockEntity(BlockPos pos, BlockState state) {
        super(TwinStarPreview.BLOCK_ENTITY.get(), pos, state);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, TwinStarPreviewBlockEntity be) {
        if (--be.checkCooldown <= 0) {
            be.checkCooldown = 40;
            be.periodicCheck((ServerLevel) level);
        }
    }

    /** The automatic check: only the master cell checks the structure; the other seven just stay unformed. */
    private void periodicCheck(ServerLevel level) {
        BlockPos min = TwinStarBlueprint.findControllerMin(level, worldPosition);
        if (!worldPosition.equals(min)) {
            setFormed(level, false);
            return;
        }
        recheck(level, false);
    }

    /**
     * Re-check from this block (a right-click), re-reading the saved structure first. Non-master
     * cells (and cells of an incomplete controller) are never formed themselves; the result still
     * reports on the whole controller / structure.
     */
    TwinStarBlueprint.Result recheck(ServerLevel level) {
        return recheck(level, true);
    }

    private TwinStarBlueprint.Result recheck(ServerLevel level, boolean reread) {
        if (reread) {
            TwinStarBlueprint.refresh(level);
        }
        BlockPos min = TwinStarBlueprint.findControllerMin(level, worldPosition);
        TwinStarBlueprint.Result result = min == null ? TwinStarBlueprint.Result.NO_CONTROLLER : TwinStarBlueprint.check(level, min);
        setFormed(level, worldPosition.equals(min) && result.formed());
        if (formed) {
            updateGeometry(level, TwinStarBlueprint.layout(level));
        }
        if (formed) {
            // idempotent: also covers a hub saved as formed before hiding existed, or frame
            // blocks placed back by hand while formed
            TwinStarBlueprint.setFrameHidden(level, worldPosition, true);
            setControllerLook(level, true);
        }
        return result;
    }

    private void updateGeometry(ServerLevel level, TwinStarBlueprint.Layout layout) {
        var c = layout.sphereCentre();
        if (c.x != sphereX || c.y != sphereY || c.z != sphereZ || layout.shellRadius() != shellRadius) {
            sphereX = c.x;
            sphereY = c.y;
            sphereZ = c.z;
            shellRadius = layout.shellRadius();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void setFormed(ServerLevel level, boolean value) {
        if (value != formed) {
            formed = value;
            // the master is always the controller's min corner, so that is where the structure hangs off
            TwinStarBlueprint.setFrameHidden(level, worldPosition, value);
            setControllerLook(level, value);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isHubFormed() {
        return formed && !isRemoved();
    }

    /** Patterns stored by the old hub GUI (removed): kept so they drop when the block is broken. */
    public InternalInventory getHubPatterns() {
        return patterns;
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        setChanged();
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    /** Called by the block when this master cell is removed (1.21.1 has no block entity removal hook). */
    void onRemoved(BlockPos pos) {
        // master broken while formed: put the placeholder frame back before we disappear
        if (formed && level instanceof ServerLevel server) {
            TwinStarBlueprint.setFrameHidden(server, worldPosition, false);
            setControllerLook(server, false);
        }
        if (level != null && !level.isClientSide()) {
            for (ItemStack pattern : patterns) {
                if (!pattern.isEmpty()) {
                    Block.popResource(level, pos, pattern);
                }
            }
            patterns.clear();
        }
    }

    /**
     * Switch the controller's eight cells (this master is its min corner) between the dormant mini
     * chip and the formed look, where each cell shows its quarter of the big design.
     */
    private void setControllerLook(ServerLevel level, boolean formedLook) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    BlockPos p = worldPosition.offset(x, y, z);
                    BlockState now = level.getBlockState(p);
                    if (!now.is(TwinStarPreview.BLOCK.get())) {
                        continue;   // e.g. the cell being broken right now
                    }
                    BlockState want = now.setValue(TwinStarPreviewBlock.FORMED, formedLook)
                            .setValue(TwinStarPreviewBlock.PX, x == 1)
                            .setValue(TwinStarPreviewBlock.PY, y == 1)
                            .setValue(TwinStarPreviewBlock.PZ, z == 1);
                    if (want != now) {
                        level.setBlock(p, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("formed", formed);
        tag.putFloat("sphereX", sphereX);
        tag.putFloat("sphereY", sphereY);
        tag.putFloat("sphereZ", sphereZ);
        tag.putFloat("shellRadius", shellRadius);
        if (!savingForSync) {
            patterns.writeToNBT(tag, "patterns");
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boolean wasFormed = formed;
        formed = tag.getBoolean("formed");
        sphereX = tag.contains("sphereX") ? tag.getFloat("sphereX") : 1;
        sphereY = tag.contains("sphereY") ? tag.getFloat("sphereY") : 24;
        sphereZ = tag.contains("sphereZ") ? tag.getFloat("sphereZ") : 1;
        shellRadius = tag.contains("shellRadius") ? tag.getFloat("shellRadius") : 11.9f;
        if (level == null || !level.isClientSide()) {
            patterns.readFromNBT(tag, "patterns");
        }
        // client: the structure just became complete -> play the formation sequence
        // (on chunk load level is still null, so an already-formed hub just shows its steady state)
        if (level != null && level.isClientSide() && formed && !wasFormed) {
            replayFormation(level.getGameTime());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        savingForSync = true;
        try {
            return saveWithoutMetadata();
        } finally {
            savingForSync = false;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    void replayFormation(long now) {
        formedAt = now;
        // the drawn rings start exactly on the placeholder rings, then spin up
        outerAngle = middleAngle = innerAngle = 0;
    }

    void toggleBusy() {
        busy = !busy;
    }

    /** Ticks since formation (huge when never replayed = fully formed). */
    public double sinceFormed(double time) {
        return formedAt == NOT_REPLAYED ? 1e9 : time - formedAt;
    }

    /** Advance every phase to {@code time} (in ticks, partial tick included). */
    public void advance(double time) {
        double dt = Double.isNaN(lastTime) ? 0 : Math.max(0, Math.min(5, time - lastTime)) / 20.0; // seconds
        lastTime = time;

        activity += ((busy ? 1 : 0) - activity) * Math.min(1, dt * 1.2);
        double spin = smoothstep(30, 60, sinceFormed(time));   // rings stay put until spin-up

        // incommensurate gimbal rates (1 : 1.618 : 2.618) so the pose never repeats
        outerAngle += dt * spin * lerp(6.0, 24.0, activity);
        middleAngle += dt * spin * lerp(9.7, 38.8, activity);
        innerAngle += dt * spin * lerp(15.7, 62.8, activity);

        // twin holes, black-hole-merger style: the cycle clock runs a bit faster when busy, and the
        // orbit speed follows the current separation (closer = faster)
        double pace = lerp(0.8, 1.25, activity);
        twinClock += dt * pace;
        orbitAngle += dt * pace * twinOmega(twinSeparation(twinClock));

        // tumble the orbit plane: rotate it a little about an axis that itself wanders (three
        // incommensurate frequencies), accumulated in a quaternion - so the orbit axis keeps
        // changing through every direction with no gimbal-lock flips
        tumbleClock += dt;
        double wx = Math.sin(tumbleClock * 0.37), wy = Math.cos(tumbleClock * 0.23 + 1.0), wz = Math.sin(tumbleClock * 0.31 + 2.0);
        double len = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (len > 1e-6) {
            new Quaterniond().rotateAxis(Math.toRadians(18.0) * dt, wx / len, wy / len, wz / len)
                    .mul(orbitFrame, orbitFrame);
            orbitFrame.normalize();
        }
    }

    /** Unit vector from the white hole to the black hole right now. */
    public Vector3d twinAxis() {
        double a = Math.toRadians(orbitAngle);
        return orbitFrame.transform(new Vector3d(Math.cos(a), 0, Math.sin(a)));
    }

    /** Twin-hole radius and the widest separation (centre to centre) in the cycle. */
    // widest separation 13.8: the holes swing out to ~9.9 from the centre, inside the unit shell (11.9)
    public static final double TWIN_RADIUS = 3.0, TWIN_MAX_SEPARATION = 2 * TWIN_RADIUS * 2.3;
    /** Seconds per spiral-in / merged / spiral-out cycle. */
    public static final double TWIN_CYCLE = 4.0;

    /**
     * Centre-to-centre separation at {@code clock} seconds: spiral in with a quickening chirp,
     * stay merged and spinning, then spiral back out. The cos / sin easing makes the radial speed
     * zero at the widest point, so the turn-around between spiralling out and spiralling back in
     * is smooth (a plain power curve reversed direction instantly there - a visible yank).
     */
    public static double twinSeparation(double clock) {
        double ph = (clock / TWIN_CYCLE) % 1.0;
        if (ph < 0.42) {
            return TWIN_MAX_SEPARATION * Math.pow(Math.cos(Math.PI / 2 * ph / 0.42), 0.6);
        }
        if (ph < 0.58) {
            return 0;
        }
        return TWIN_MAX_SEPARATION * Math.pow(Math.sin(Math.PI / 2 * (ph - 0.58) / 0.42), 0.6);
    }

    /** Orbit speed in degrees/second: Kepler-like (faster when closer), capped. */
    public static double twinOmega(double separation) {
        return Math.min(540.0, 55.0 * Math.pow(TWIN_MAX_SEPARATION / Math.max(separation, 0.8), 1.5));
    }

    public static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return com.lumina.ae2_gvt.client.renderer.TwinStarPreviewRenderer.renderBox(this);
    }
}
