package com.lumina.ae2_gvt.client;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws in the world with the matrices the world itself was drawn with. Everything the mod puts in the frame after the world (holes,
 * ripples, the collector's and the rift's distortion, the link tool) is given in blocks from the camera, so it is placed right only if
 * the model-view matrix is the camera's rotation and the projection is the world's. Vanilla leaves both in RenderSystem at that point,
 * but a mod that changes them for its own pass and does not put them back (or draws in between) leaves ours drawn somewhere else - a hole
 * turned up away from its block. So both are set from the event, which carries what the level was drawn with, for the draw, and put
 * back after it.
 */
final class LevelPass {
    private LevelPass() {
    }

    static void run(RenderLevelStageEvent event, Runnable draw) {
        Matrix4fStack view = RenderSystem.getModelViewStack();
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        view.pushMatrix();
        view.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        try {
            draw.run();
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
        }
    }
}
