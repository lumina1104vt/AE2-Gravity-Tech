package com.lumina.twinstar.tools;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encodes every recipe the server has loaded as an AE2 pattern and puts them into a block (anything
 * that takes items: the test hub with its 50,000 slots, a chest, a pattern provider). Op level 2.
 * <pre>
 * /twinstar gen_patterns &lt;pos&gt; [filter]                          all into pos
 * /twinstar gen_patterns &lt;pos&gt; no_processing [filter]            no processing patterns
 * /twinstar gen_patterns &lt;pos&gt; processing_to &lt;pos2&gt; [filter]    processing patterns into pos2
 * </pre>
 * {@code filter} keeps the recipes whose id contains it ({@code minecraft:}, {@code ae2:}, {@code iron}).
 * The work runs on the server thread a little each tick ({@link #TICK_BUDGET_NANOS}), so a pack
 * with tens of thousands of recipes does not freeze the server; {@code /twinstar gen_patterns cancel}
 * stops it. Patterns can only be put into blocks on the server thread, hence no worker thread.
 * <ul>
 *   <li>crafting table recipes → crafting patterns, laid out as the recipe is shaped, substitutes
 *   allowed (tag recipes take any matching item);</li>
 *   <li>stonecutter recipes → stonecutting patterns;</li>
 *   <li>everything else (furnaces, smithing, mod machines) → processing patterns: inputs as the
 *   recipe lists them, the result as the only output. Items left behind (the bucket of a water
 *   bucket, a damaged tool) are not written: an output written in a pattern is a main product to
 *   the engine, and a remainder is not one.</li>
 * </ul>
 * Every pattern is decoded once; one AE2 does not accept as crafting/stonecutting is made as a
 * processing pattern instead. Special recipes (fireworks, map cloning) and pure A → A recipes
 * (only the result item goes in) are skipped.
 */
public final class PatternGen {
    private PatternGen() {
    }

    /** Where processing patterns go: with the rest, nowhere, or into a second block. */
    private enum ProcessingTo { SAME, NONE, OTHER }

    /** Generations in progress, stepped on the server thread a little each tick. */
    private static final List<Job> JOBS = new ArrayList<>();
    /** server-thread time all jobs together may use per tick (a tick is 50 ms) */
    private static final long TICK_BUDGET_NANOS = 20_000_000L;
    private static final long PROGRESS_EVERY_NANOS = 10_000_000_000L;

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("twinstar").requires(s -> s.hasPermission(2))
                .then(Commands.literal("gen_patterns")
                        .then(Commands.literal("cancel").executes(ctx -> cancel(ctx.getSource())))
                        .then(withFilter(Commands.argument("pos", BlockPosArgument.blockPos()), ProcessingTo.SAME)
                                .then(withFilter(Commands.literal("no_processing"), ProcessingTo.NONE))
                                .then(Commands.literal("processing_to")
                                        .then(withFilter(Commands.argument("processing_pos", BlockPosArgument.blockPos()),
                                                ProcessingTo.OTHER))))));
    }

    /** {@code node} runs as is, or with a trailing filter. */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withFilter(T node, ProcessingTo mode) {
        return node.executes(ctx -> start(ctx, mode, ""))
                .then(Commands.argument("filter", StringArgumentType.greedyString())
                        .executes(ctx -> start(ctx, mode, StringArgumentType.getString(ctx, "filter"))));
    }

    /** Checks the targets and queues the job; the work happens in {@link #onServerTick}. */
    private static int start(CommandContext<CommandSourceStack> ctx, ProcessingTo mode, String filter) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        BlockPos processingPos = mode == ProcessingTo.OTHER ? BlockPosArgument.getLoadedBlockPos(ctx, "processing_pos") : null;
        if (handlerAt(source.getLevel(), pos) == null) {
            source.sendFailure(Component.literal("[雙星] " + pos.toShortString() + " 的方塊不能放入物品"));
            return 0;
        }
        if (processingPos != null && handlerAt(source.getLevel(), processingPos) == null) {
            source.sendFailure(Component.literal("[雙星] " + processingPos.toShortString() + " 的方塊不能放入物品"));
            return 0;
        }
        Job job = new Job(source, source.getLevel(), pos, processingPos, mode, filter);
        JOBS.add(job);
        source.sendSuccess(() -> Component.literal("[雙星] 開始生成樣板：" + job.recipes.size()
                + " 條配方，在背景分批進行（每 tick 最多 20 毫秒）；/twinstar gen_patterns cancel 可取消"), true);
        return job.recipes.size();
    }

    private static int cancel(CommandSourceStack source) {
        int n = JOBS.size();
        for (Job job : JOBS)
            job.report("[雙星] 已取消生成樣板（" + job.next + " / " + job.recipes.size() + "）：", false);
        JOBS.clear();
        source.sendSuccess(() -> Component.literal("[雙星] 取消了 " + n + " 個生成樣板的工作"), true);
        return n;
    }

    /** No generation running (dev automation waits for this before stopping the server). */
    public static boolean isIdle() {
        return JOBS.isEmpty();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty())
            return;
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext() && System.nanoTime() < deadline) {
            Job job = it.next();
            if (job.step(deadline))
                it.remove();
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
    }

    private static @Nullable IItemHandler handlerAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos))
            return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    /** Startup check ({@code -Dtwinstar.genPatternsCheck=true}): encode and decode everything at once, insert nothing. */
    public static String check(ServerLevel level) {
        Job job = new Job(null, level, null, null, ProcessingTo.SAME, "");
        job.step(Long.MAX_VALUE);
        return job.summary();
    }

    /** One generation: the recipes it covers, how far it got, what went where. */
    private static final class Job {
        final @Nullable CommandSourceStack source;
        final ServerLevel level;
        final @Nullable BlockPos pos;
        final @Nullable BlockPos processingPos;
        final ProcessingTo mode;
        final List<RecipeHolder<?>> recipes = new ArrayList<>();
        int next;
        int crafting, stonecutting, processing, skipped, selfLoops;
        final List<String> selfLoopIds = new ArrayList<>();
        final Sink main = new Sink();
        final Sink other = new Sink();
        long lastProgress = System.nanoTime();

        Job(@Nullable CommandSourceStack source, ServerLevel level, @Nullable BlockPos pos, @Nullable BlockPos processingPos,
            ProcessingTo mode, String filter) {
            this.source = source;
            this.level = level;
            this.pos = pos;
            this.processingPos = processingPos;
            this.mode = mode;
            for (RecipeHolder<?> h : level.getRecipeManager().getRecipes())
                if (filter.isEmpty() || h.id().toString().contains(filter))
                    recipes.add(h);
        }

        /** Works until {@code deadline}; true when done or stopped. */
        boolean step(long deadline) {
            // the targets are looked up again every tick: the blocks may have been broken meanwhile
            IItemHandler target = null;
            IItemHandler processingTarget = null;
            if (pos != null) {
                target = handlerAt(level, pos);
                if (target == null)
                    return stop(pos);
                processingTarget = target;
                if (mode == ProcessingTo.OTHER) {
                    processingTarget = handlerAt(level, processingPos);
                    if (processingTarget == null)
                        return stop(processingPos);
                }
            }
            while (next < recipes.size() && System.nanoTime() < deadline)
                one(recipes.get(next++), target, processingTarget);
            if (next >= recipes.size()) {
                report(summary(), true);
                return true;
            }
            long now = System.nanoTime();
            if (now - lastProgress > PROGRESS_EVERY_NANOS) {
                lastProgress = now;
                report("[雙星] 生成樣板進度：" + next + " / " + recipes.size(), true);
            }
            return false;
        }

        private void one(RecipeHolder<?> holder, @Nullable IItemHandler target, @Nullable IItemHandler processingTarget) {
            ItemStack pattern;
            try {
                if (pureSelfRecipe(holder.value(), level.registryAccess())) {
                    selfLoops++;
                    if (selfLoopIds.size() < 5)
                        selfLoopIds.add(holder.id().toString());
                    return;
                }
                pattern = encode(holder, level.registryAccess(), level);
            } catch (RuntimeException e) {
                pattern = null;     // a mod's recipe that cannot answer outside its machine
            }
            if (pattern == null) {
                skipped++;
                return;
            }
            switch (kindOf(holder, pattern, level)) {
                case 0 -> {
                    crafting++;
                    main.put(target, pattern);
                }
                case 1 -> {
                    stonecutting++;
                    main.put(target, pattern);
                }
                default -> {
                    processing++;
                    if (mode == ProcessingTo.SAME)
                        main.put(target, pattern);
                    else if (mode == ProcessingTo.OTHER)
                        other.put(processingTarget, pattern);
                }
            }
        }

        private boolean stop(BlockPos where) {
            report("[雙星] 生成樣板中止：" + where.toShortString() + " 的方塊不見了或所在區塊已卸載（"
                    + next + " / " + recipes.size() + "）。" + summary(), false);
            return true;
        }

        void report(String text, boolean ok) {
            if (source == null)
                return;
            if (ok)
                source.sendSuccess(() -> Component.literal(text), true);
            else
                source.sendFailure(Component.literal(text));
        }

        String summary() {
            StringBuilder sb = new StringBuilder("[雙星] 生成樣板：合成 " + crafting + "、切石機 " + stonecutting);
            sb.append(mode == ProcessingTo.NONE ? "（處理樣板 " + processing + " 張未生成）" : "、處理 " + processing);
            sb.append("；略過 ").append(skipped).append(" 條特殊或無法讀取的配方");
            if (selfLoops > 0)
                sb.append("、").append(selfLoops).append(" 條純粹 A 變 A 的配方（").append(String.join("、", selfLoopIds))
                        .append(selfLoops > selfLoopIds.size() ? "…" : "").append("）");
            sb.append("。");
            if (pos == null)
                return sb.toString();
            sb.append("放入 ").append(main.inserted).append(" 張");
            if (main.full > 0)
                sb.append("，目標已滿，剩 ").append(main.full).append(" 張沒放");
            if (mode == ProcessingTo.OTHER) {
                sb.append("；處理樣板放入另一個容器 ").append(other.inserted).append(" 張");
                if (other.full > 0)
                    sb.append("，已滿，剩 ").append(other.full).append(" 張沒放");
            }
            return sb.toString();
        }
    }

    /** One target's tally: once it is full the rest is only counted. */
    private static final class Sink {
        int inserted;
        int full;

        void put(@Nullable IItemHandler handler, ItemStack pattern) {
            if (handler == null)
                return;
            if (full == 0 && ItemHandlerHelper.insertItem(handler, pattern, false).isEmpty())
                inserted++;
            else
                full++;
        }
    }

    /**
     * Pure A → A: every input is the result item itself and nothing else (a tag ingredient counts
     * only when all its options are that item). Usually a recipe that clears an item's data. Not
     * generated. Template duplication (template + diamonds → 2 templates) and upgrades (terminal +
     * another terminal → terminal) need other inputs and stay.
     */
    static boolean pureSelfRecipe(Recipe<?> r, RegistryAccess registries) {
        ItemStack result = r.getResultItem(registries);
        if (result.isEmpty())
            return false;
        boolean any = false;
        for (Ingredient ing : r.getIngredients()) {
            if (ing.isEmpty())
                continue;
            ItemStack[] options = ing.getItems();
            if (options.length == 0)
                return false;
            for (ItemStack option : options)
                if (!option.is(result.getItem()))
                    return false;
            any = true;
        }
        return any;
    }

    /** 0 = crafting, 1 = stonecutting, 2 = processing (as encoded). */
    private static int kindOf(RecipeHolder<?> holder, ItemStack pattern, ServerLevel level) {
        var details = PatternDetailsHelper.decodePattern(pattern, level);
        String name = details == null ? "" : details.getClass().getSimpleName();
        return name.contains("Crafting") ? 0 : name.contains("Stonecutting") ? 1 : 2;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable ItemStack encode(RecipeHolder<?> holder, RegistryAccess registries, ServerLevel level) {
        Recipe<?> r = holder.value();
        if (r.isSpecial())
            return null;
        ItemStack result = r.getResultItem(registries);
        if (result.isEmpty())
            return null;
        ItemStack pattern = null;
        if (r instanceof CraftingRecipe) {
            ItemStack[] grid = grid(r);
            if (grid != null)
                pattern = PatternDetailsHelper.encodeCraftingPattern((RecipeHolder<CraftingRecipe>) holder, grid, result, true, false);
        } else if (r instanceof StonecutterRecipe && !r.getIngredients().isEmpty()) {
            ItemStack[] in = r.getIngredients().get(0).getItems();
            if (in.length > 0)
                pattern = PatternDetailsHelper.encodeStonecuttingPattern((RecipeHolder<StonecutterRecipe>) holder,
                        AEItemKey.of(in[0]), AEItemKey.of(result), true);
        }
        if (pattern != null && PatternDetailsHelper.decodePattern(pattern, level) != null)
            return pattern;
        return processing(r, result, level);
    }

    /** The 3×3 grid as the recipe is shaped (shapeless: in order); null when it does not fit. */
    private static ItemStack @Nullable [] grid(Recipe<?> r) {
        NonNullList<Ingredient> ings = r.getIngredients();
        ItemStack[] grid = new ItemStack[9];
        java.util.Arrays.fill(grid, ItemStack.EMPTY);
        int width = r instanceof ShapedRecipe s ? s.getWidth() : 3;
        if (width > 3 || ings.size() > 9)
            return null;
        for (int i = 0; i < ings.size(); i++) {
            Ingredient ing = ings.get(i);
            if (ing.isEmpty())
                continue;
            ItemStack[] options = ing.getItems();
            if (options.length == 0)
                return null;
            int slot = (i / width) * 3 + i % width;
            if (slot >= 9)
                return null;
            grid[slot] = options[0].copyWithCount(1);
        }
        return grid;
    }

    private static @Nullable ItemStack processing(Recipe<?> r, ItemStack result, ServerLevel level) {
        Map<AEItemKey, Long> in = new LinkedHashMap<>();
        Map<AEItemKey, Long> out = new LinkedHashMap<>();
        out.merge(AEItemKey.of(result), (long) result.getCount(), Long::sum);
        for (Ingredient ing : r.getIngredients()) {
            if (ing.isEmpty())
                continue;
            ItemStack[] options = ing.getItems();
            if (options.length == 0)
                return null;
            long count = Math.max(1, options[0].getCount());
            in.merge(AEItemKey.of(options[0]), count, Long::sum);
        }
        if (in.isEmpty())
            return null;
        List<GenericStack> inputs = new ArrayList<>();
        in.forEach((k, v) -> inputs.add(new GenericStack(k, v)));
        List<GenericStack> outputs = new ArrayList<>();
        out.forEach((k, v) -> outputs.add(new GenericStack(k, v)));
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(inputs, outputs);
        return PatternDetailsHelper.decodePattern(pattern, level) != null ? pattern : null;
    }
}
