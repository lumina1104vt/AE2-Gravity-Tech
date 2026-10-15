package com.lumina.ae2_gvt.prototype.hole;

import com.lumina.ae2_gvt.AE2GravityTech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Black and white holes. Only black holes will spawn naturally; both can be made artificially
 * ({@link HoleCollapse}). Placeholder: no natural spawning, no crystal growth yet - summon one with
 * /summon ae2_gvt:black_hole or ae2_gvt:white_hole.
 */
public final class GravityHole {
    /** Hole radius in blocks (the entity box is twice that). */
    public static final float RADIUS = 0.35f;

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AE2GravityTech.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GravityHoleEntity>> BLACK_HOLE = hole("black_hole");
    public static final DeferredHolder<EntityType<?>, EntityType<GravityHoleEntity>> WHITE_HOLE = hole("white_hole");

    private GravityHole() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<GravityHoleEntity>> hole(String id) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, id));
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.<GravityHoleEntity>of(GravityHoleEntity::new, MobCategory.MISC)
                .sized(RADIUS * 2, RADIUS * 2).fireImmune().clientTrackingRange(10).build(key));
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
