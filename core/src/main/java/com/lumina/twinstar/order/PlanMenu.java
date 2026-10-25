package com.lumina.twinstar.order;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuHostLocator;
import com.lumina.twinstar.Twinstar;
import com.lumina.twinstar.algorithm.ae2.PlanWrapper;
import com.lumina.twinstar.algorithm.model.Amount;
import com.lumina.twinstar.api.ITwinStarService;
import com.lumina.twinstar.service.TwinStarService;
import com.lumina.twinstar.storage.TwinstarContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The twin-star plan screen: plans the order with the twin-star engine (in place of AE2's confirm
 * screen), shows the crafting tree, and starts it on the hub. "Back" returns to the order screen
 * with the same amount.
 */
public class PlanMenu extends AEBaseMenu implements ISubMenu {
    private static final String START = "start";
    private static final String REPLAN = "replan";
    private static final String BACK = "back";

    /** Planning on the engine thread; the client shows "planning…". */
    @GuiSync(10)
    public boolean planning;
    /** Why the last start failed (a {@link appeng.api.networking.crafting.CraftingSubmitErrorCode} name, or "timeout" / "no_hub"); empty = none. */
    @GuiSync(11)
    public String error = "";

    private final ISubMenuHost host;
    // server
    private @Nullable AEKey what;
    private BigInteger amount = BigInteger.ONE;
    private CalculationStrategy strategy = CalculationStrategy.REPORT_MISSING_ITEMS;
    private boolean autoStart;
    private @Nullable Future<ICraftingPlan> job;
    private @Nullable ICraftingPlan result;
    private long startedNanos;
    // client
    private @Nullable PlanView view;

    public PlanMenu(int id, Inventory playerInventory, ISubMenuHost host) {
        super(TwinstarContent.PLAN_MENU.get(), id, playerInventory, host);
        this.host = host;
        registerClientAction(START, this::start);
        registerClientAction(REPLAN, this::replan);
        registerClientAction(BACK, this::back);
    }

    public static void open(ServerPlayer player, MenuHostLocator locator, AEKey what, BigInteger amount, boolean bestEffort,
                            boolean autoStart) {
        MenuOpener.open(TwinstarContent.PLAN_MENU.get(), player, locator);
        if (player.containerMenu instanceof PlanMenu menu) {
            menu.what = what;
            menu.amount = amount;
            menu.strategy = bestEffort ? CalculationStrategy.CRAFT_LESS : CalculationStrategy.REPORT_MISSING_ITEMS;
            menu.autoStart = autoStart;
            menu.plan();
            menu.broadcastChanges();
        }
    }

    @Override
    public ISubMenuHost getHost() {
        return host;
    }

    private @Nullable TwinStarService service() {
        IGrid grid = OrderFlow.gridOf(getActionHost());
        if (grid == null)
            return null;
        ITwinStarService s = grid.getService(ITwinStarService.class);
        return s instanceof TwinStarService ts && ts.hasHub() ? ts : null;
    }

    private void plan() {
        if (job != null)
            job.cancel(true);
        job = null;
        result = null;
        error = "";
        TwinStarService service = service();
        if (service == null || what == null) {
            planning = false;
            error = "no_hub";
            return;
        }
        startedNanos = System.nanoTime();
        job = service.beginCalculation(what, Amount.of(amount), strategy);
        planning = true;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide() && job != null && job.isDone())
            finishPlanning();
        super.broadcastChanges();
    }

    private void finishPlanning() {
        Future<ICraftingPlan> done = job;
        job = null;
        planning = false;
        try {
            result = done.get();
        } catch (ExecutionException e) {
            error = "timeout";
            // a timeout is expected now and then; anything else is a bug and must not vanish silently
            if (!(e.getCause() instanceof TwinStarService.PlanTimedOutException))
                Twinstar.LOGGER.error("[雙星] 規劃 {} ×{} 時引擎出錯", what, amount, e.getCause());
            return;
        } catch (Exception e) {
            return;     // cancelled
        }
        TwinStarService service = service();
        if (!(result instanceof PlanWrapper pw) || service == null || !(getPlayer() instanceof ServerPlayer player))
            return;
        PlanView v = PlanTree.build(pw, System.nanoTime() - startedNanos, service.freeCapacityExact(), service.getParallelism());
        PacketDistributor.sendToPlayer(player, new PlanViewPacket(containerId, v));
        if (autoStart && pw.plan().isComplete()) {
            autoStart = false;
            start();
        }
    }

    public void start() {
        if (!isServerSide()) {
            sendClientAction(START);
            return;
        }
        TwinStarService service = service();
        if (result == null || service == null)
            return;
        Player player = getPlayer();
        ICraftingSubmitResult r = service.submit(result, null, IActionSource.ofPlayer(player, getActionHost()));
        if (r.successful())
            host.returnToMainMenu(player, this);
        else
            error = r.errorCode() != null ? r.errorCode().name() : "unknown";
    }

    public void replan() {
        if (!isServerSide()) {
            view = null;
            sendClientAction(REPLAN);
            return;
        }
        plan();
    }

    public void back() {
        if (!isServerSide()) {
            sendClientAction(BACK);
            return;
        }
        MenuHostLocator locator = getLocator();
        if (what != null && locator != null && getPlayer() instanceof ServerPlayer player)
            OrderMenu.open(player, locator, what, amount);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (job != null)
            job.cancel(true);
    }

    // ── client ──

    public void setView(PlanView view) {
        this.view = view;
    }

    public @Nullable PlanView getView() {
        return view;
    }
}
