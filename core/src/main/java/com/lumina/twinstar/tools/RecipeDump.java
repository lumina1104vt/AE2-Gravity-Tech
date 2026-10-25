package com.lumina.twinstar.tools;

import appeng.api.stacks.AEItemKey;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lumina.twinstar.Twinstar;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes every recipe the server has loaded (all mods, datapacks and KubeJS changes of a modpack)
 * to a JSON file, for the offline engine benchmark ({@code gradlew :core:recipeBench}).
 * <ul>
 *   <li>{@code /twinstar dump_recipes [file]} (op level 2); default {@code <server dir>/twinstar/recipes.json}</li>
 *   <li>or start the server with {@code -Dtwinstar.dumpRecipesOnStart=<file>} (automation; add
 *   {@code -Dtwinstar.stopAfterDump=true} to shut the server down afterwards)</li>
 *   <li>dev automation: {@code -Dtwinstar.startupCommands="cmd;@40 cmd"} runs commands as the
 *   console after start ({@code @n}: n ticks later); with stopAfterDump the server stops once they
 *   and any pattern generation are done</li>
 * </ul>
 * Format 2: {@code items} = item names (index = id in the file), keys compared like the engine's
 * {@code ItemIndex} (item + data components; see {@link KeyNames}); each recipe has {@code in} =
 * [count, [item, alternatives…]] per distinct ingredient, with a third element {@code "container"}
 * when the item comes back unchanged, {@code out} and {@code rem} (other items left behind, e.g. the
 * bucket of a water bucket, a damaged tool) = [item, count]. Remainders are classified like
 * {@code PatternCompiler} does for AE2's crafting patterns. Pure A → A recipes are left out, like
 * {@code gen_patterns} does. Only what the vanilla recipe interface exposes is seen: item
 * ingredients and the main result; fluids, chances and extra outputs of mod machines are not.
 */
public final class RecipeDump {
    /** alternatives kept per ingredient (big tags such as "any planks" are cut) */
    private static final int MAX_ALTERNATIVES = 32;

    private RecipeDump() {
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("twinstar").requires(s -> s.hasPermission(2))
                .then(Commands.literal("dump_recipes")
                        .executes(ctx -> dump(ctx.getSource(), null))
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                .executes(ctx -> dump(ctx.getSource(), StringArgumentType.getString(ctx, "file"))))));
    }

    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (Boolean.getBoolean("twinstar.genPatternsCheck"))
            Twinstar.LOGGER.info(PatternGen.check(server.overworld()));
        // dev automation: commands run as the console, ';' between them; "@40 cmd" runs 40 ticks later
        String commands = System.getProperty("twinstar.startupCommands", "");
        for (String c : commands.split(";")) {
            String cmd = c.trim();
            if (cmd.isEmpty())
                continue;
            int delay = 0;
            if (cmd.startsWith("@")) {
                int space = cmd.indexOf(' ');
                delay = Integer.parseInt(cmd.substring(1, space));
                cmd = cmd.substring(space + 1).trim();
            }
            SCHEDULED.add(new Scheduled(server.getTickCount() + delay, cmd));
        }
        stopWhenDone = Boolean.getBoolean("twinstar.stopAfterDump");
        String file = System.getProperty("twinstar.dumpRecipesOnStart");
        if (file == null || file.isBlank())
            return;
        try {
            Result r = write(server, Path.of(file));
            Twinstar.LOGGER.info("[雙星] 合成表已匯出：{} 條（略過 {} 條、純粹 A 變 A {} 條）→ {}", r.written, r.skipped,
                    r.selfRecipes, r.file.toAbsolutePath());
        } catch (IOException e) {
            Twinstar.LOGGER.error("[雙星] 合成表匯出失敗", e);
        }
    }

    private record Scheduled(int tick, String command) {
    }

    private static final List<Scheduled> SCHEDULED = new ArrayList<>();
    private static boolean stopWhenDone;

    /** Runs the scheduled startup commands; with stopAfterDump, stops once they and any pattern generation are done. */
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();
        for (int i = 0; i < SCHEDULED.size(); i++) {
            Scheduled s = SCHEDULED.get(i);
            if (s.tick() <= now) {
                SCHEDULED.remove(i--);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), s.command());
            }
        }
        if (stopWhenDone && SCHEDULED.isEmpty() && PatternGen.isIdle()) {
            stopWhenDone = false;
            server.halt(false);
        }
    }

    private static int dump(CommandSourceStack source, String file) {
        MinecraftServer server = source.getServer();
        Path path = file != null ? Path.of(file) : server.getServerDirectory().resolve("twinstar").resolve("recipes.json");
        try {
            Result r = write(server, path);
            source.sendSuccess(() -> Component.literal("[雙星] 合成表已匯出：" + r.written + " 條（略過 " + r.skipped
                    + " 條特殊或無法讀取的配方、" + r.selfRecipes + " 條純粹 A 變 A 的配方）→ " + r.file.toAbsolutePath()), true);
            return r.written;
        } catch (IOException e) {
            source.sendFailure(Component.literal("[雙星] 匯出失敗：" + e.getMessage()));
            return 0;
        }
    }

    private record Result(int written, int skipped, int selfRecipes, Path file) {
    }

    private static Result write(MinecraftServer server, Path path) throws IOException {
        RegistryAccess registries = server.registryAccess();
        Map<AEItemKey, Integer> ids = new LinkedHashMap<>();
        JsonArray recipes = new JsonArray();
        int skipped = 0;
        int selfRecipes = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            JsonObject json;
            try {
                if (PatternGen.pureSelfRecipe(holder.value(), registries)) {
                    selfRecipes++;      // gen_patterns does not make these either
                    continue;
                }
                json = recipe(holder, registries, ids);
            } catch (RuntimeException e) {
                json = null;    // a mod's recipe that cannot answer outside its machine
            }
            if (json == null) {
                skipped++;
                continue;
            }
            recipes.add(json);
        }
        JsonArray items = new JsonArray();
        for (AEItemKey key : ids.keySet())
            items.add(KeyNames.of(key, registries));
        JsonObject root = new JsonObject();
        root.addProperty("format", 2);
        root.add("items", items);
        root.add("recipes", recipes);

        Path parent = path.toAbsolutePath().getParent();
        if (parent != null)
            Files.createDirectories(parent);
        Files.writeString(path, new GsonBuilder().create().toJson(root), StandardCharsets.UTF_8);
        return new Result(recipes.size(), skipped, selfRecipes, path);
    }

    private static JsonObject recipe(RecipeHolder<?> holder, RegistryAccess registries, Map<AEItemKey, Integer> ids) {
        Recipe<?> r = holder.value();
        if (r.isSpecial())
            return null;    // fireworks, map cloning, …: the result depends on the actual items
        ItemStack result = r.getResultItem(registries);
        NonNullList<Ingredient> ingredients = r.getIngredients();
        if (result.isEmpty() || ingredients.isEmpty())
            return null;

        // identical ingredients (the same option list) are one input with a count
        Map<List<Integer>, Integer> inputs = new LinkedHashMap<>();
        Set<List<Integer>> containers = new HashSet<>();
        Map<Integer, Integer> remainders = new HashMap<>();
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty())
                continue;   // an empty grid cell
            ItemStack[] options = ing.getItems();
            if (options.length == 0)
                return null;    // a tag with no items in this pack: nothing can fill it
            List<Integer> key = new ArrayList<>();
            for (ItemStack o : options) {
                int id = id(o, ids);
                if (!key.contains(id))
                    key.add(id);
                if (key.size() >= MAX_ALTERNATIVES)
                    break;
            }
            int count = Math.max(1, options[0].getCount());
            inputs.merge(key, count, Integer::sum);
            // like PatternCompiler: the same key back makes a container, anything else is a remainder
            ItemStack rest = options[0].getCraftingRemainingItem();
            if (rest.isEmpty())
                continue;
            if (AEItemKey.of(rest).equals(AEItemKey.of(options[0])))
                containers.add(key);
            else
                remainders.merge(id(rest, ids), rest.getCount() * count, Integer::sum);
        }
        if (inputs.isEmpty())
            return null;

        JsonObject json = new JsonObject();
        json.addProperty("id", holder.id().toString());
        ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(r.getType());
        json.addProperty("type", type == null ? "?" : type.toString());
        JsonArray in = new JsonArray();
        inputs.forEach((options, count) -> {
            JsonArray slot = new JsonArray();
            slot.add(count);
            JsonArray opts = new JsonArray();
            options.forEach(opts::add);
            slot.add(opts);
            if (containers.contains(options))
                slot.add("container");
            in.add(slot);
        });
        json.add("in", in);
        JsonArray out = new JsonArray();
        out.add(pair(id(result, ids), result.getCount()));
        json.add("out", out);
        if (!remainders.isEmpty()) {
            JsonArray rem = new JsonArray();
            remainders.forEach((item, count) -> rem.add(pair(item, count)));
            json.add("rem", rem);
        }
        return json;
    }

    private static JsonArray pair(int item, int count) {
        JsonArray a = new JsonArray();
        a.add(item);
        a.add(count);
        return a;
    }

    private static int id(ItemStack stack, Map<AEItemKey, Integer> ids) {
        return ids.computeIfAbsent(AEItemKey.of(stack), k -> ids.size());
    }
}
