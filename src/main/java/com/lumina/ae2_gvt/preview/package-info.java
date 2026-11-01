/**
 * ====================================================================================
 *  PROTOTYPE - Twin-Star Hub animation preview. NOT the real implementation.
 * ====================================================================================
 * <p>
 * A visual mock-up so the formation / idle animation can be judged in game before the real
 * controller is written. It has a simple structure check so the animation only appears on a
 * complete hub, but no AE2 network node and no real behaviour.
 * <p>
 * The layout is read straight from the structure saved with a structure block as
 * {@code ae2_gvt:calculator} (bundled in data/ae2_gvt/structure; a copy saved in the world wins): re-save it and the next check (every 2 s, or a right-click)
 * uses it - controller spot, sphere size and what gets hidden are all worked out from it (see
 * {@code TwinStarBlueprint}).
 * <p>
 * How to use: build the 2x2x2 controller out of eight "[PROTOTYPE] Twin-Star Hub Preview" blocks
 * in the controller spot (the structure's own preview blocks, or the empty 2x2x2 at the bottom of
 * its centre axis). The controller is found first, then the
 * structure is checked around its centre (any facing works - the layout is 4-fold symmetric).
 * The animation appears once everything matches, centred on the tinted-glass sphere.
 * <ul>
 *   <li>right-click any controller block: re-check; opens the hub GUI when formed</li>
 *   <li>sneak + right-click: toggle idle / busy (AE2 crafting activity) to see the speed change</li>
 * </ul>
 * While formed, the placeholder blocks (sky-stone bricks, mysterious cubes, tinted glass, end rods,
 * wireless access points, and paradox blocks / structure frames on the sphere) are swapped for
 * invisible stand-ins ({@code prototype_hub_frame}) and swapped back when the hub breaks apart. Break a
 * controller block (un-forming the hub) BEFORE deleting this package, or the stand-ins turn into
 * unknown blocks and are lost.
 * <p>
 * Delete this whole package (plus the one PROTOTYPE line in {@code AE2GravityTech}, the
 * {@code prototype_*} assets, {@code tools/prototype_*.py} and the PROTOTYPE line in {@code META-INF/accesstransformer.cfg})
 * once the real Twin-Star Hub controller exists.
 */
package com.lumina.ae2_gvt.preview;
