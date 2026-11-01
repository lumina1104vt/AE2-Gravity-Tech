package com.lumina.ae2_gvt.storage;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import appeng.api.stacks.AEKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * What the black hole disks hold, kept with the world: a disk may hold more than an item could carry, so
 * the item has only its id. Amounts are saved as text, any number of digits.
 */
public final class BlackHoleDisks extends SavedData {
    private static final String NAME = "ae2_gvt_black_hole_disks";

    private final Map<UUID, Map<AEKey, BigInteger>> disks = new ConcurrentHashMap<>();

    private BlackHoleDisks() {
    }

    public static BlackHoleDisks of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(BlackHoleDisks::new, BlackHoleDisks::load), NAME);
    }

    Map<AEKey, BigInteger> contents(UUID id) {
        return disks.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
    }

    private static BlackHoleDisks load(CompoundTag tag, HolderLookup.Provider registries) {
        BlackHoleDisks data = new BlackHoleDisks();
        ListTag list = tag.getList("disks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag disk = list.getCompound(i);
            Map<AEKey, BigInteger> contents = data.contents(disk.getUUID("id"));
            ListTag stored = disk.getList("stored", Tag.TAG_COMPOUND);
            for (int j = 0; j < stored.size(); j++) {
                CompoundTag entry = stored.getCompound(j);
                AEKey key = AEKey.fromTagGeneric(registries, entry.getCompound("key"));
                if (key == null)
                    continue;
                try {
                    BigInteger amount = new BigInteger(entry.getString("amount"));
                    if (amount.signum() > 0)
                        contents.put(key, amount);
                } catch (NumberFormatException ignored) {
                    // a broken entry is dropped rather than the whole disk
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        disks.forEach((id, contents) -> {
            if (contents.isEmpty())
                return;
            ListTag stored = new ListTag();
            contents.forEach((key, amount) -> {
                CompoundTag entry = new CompoundTag();
                entry.put("key", key.toTagGeneric(registries));
                entry.putString("amount", amount.toString());
                stored.add(entry);
            });
            CompoundTag disk = new CompoundTag();
            disk.putUUID("id", id);
            disk.put("stored", stored);
            list.add(disk);
        });
        tag.put("disks", list);
        return tag;
    }
}
