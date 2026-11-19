#version 150

// Gravitational lensing round a hole: redraw the world behind the panel bent. Sampler0 is the frame
// so far, Sampler1 its depth (LensPass); the bent picture goes to a target of its own, laid over the frame after.
//  - black hole: a true lens - each pixel at r shows the scene at r - K / r, so what is behind the
//    hole is stretched round it, not along the radius - with an Einstein ring and a thin photon ring
//  - white hole: the opposite - a magnifier: near the hole each pixel shows a smaller piece of the
//    scene (ZOOM times larger), easing back to no change at the panel edge; never flipped or
//    folded - plus a tight white glow
// The panel's size on screen comes from the UV derivatives, so no screen-space data is passed in.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;   // depth of the frame itself (full size)
uniform mat4 ProjMat;

// The frame is read as it is, and its texture is not filtered: smooth it by hand.
vec3 linear(vec2 uv, vec2 size) {
    vec2 p = uv * size - 0.5;
    ivec2 i = ivec2(floor(p));
    vec2 f = fract(p);
    ivec2 hi = ivec2(size) - 1;
    vec3 a = texelFetch(Sampler0, clamp(i, ivec2(0), hi), 0).rgb;
    vec3 b = texelFetch(Sampler0, clamp(i + ivec2(1, 0), ivec2(0), hi), 0).rgb;
    vec3 c = texelFetch(Sampler0, clamp(i + ivec2(0, 1), ivec2(0), hi), 0).rgb;
    vec3 d = texelFetch(Sampler0, clamp(i + ivec2(1, 1), ivec2(0), hi), 0).rgb;
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

in vec2 local;
in vec4 flags;

out vec4 fragColor;

const float EXTENT = 7.0;   // panel half-size in hole radii (HoleRenderer.LENS)
const float K = 3.61;       // lens strength: Einstein ring at sqrt(K) = 1.9 radii
const float ZOOM = 2.5;     // white hole: how much larger things look near it
const float BEHIND_MIN = 0.5;    // blocks behind the hole's plane where the bending starts ...
const float BEHIND_FULL = 5.0;   // ... and where it is complete
const float IN_FRONT = 2.5;      // blocks: what is nearer than the hole by more than this hides the panel (a floor just under the hole does not)

void main() {
    float r = length(local);
    if (r < 1.0 || r > EXTENT) {
        discard;
    }
    bool white = flags.r > 0.5;
    if (flags.g > 0.5) {
        // lowest detail: no copy of the frame to bend, only the rings over the scene
        float a = white ? pow(clamp(1.0 - (r - 1.0) / 0.8, 0.0, 1.0), 2.0) * 0.6
                        : max(exp(-pow((r - 1.9) / 0.12, 2.0)) * 0.55, exp(-pow((r - 1.05) / 0.05, 2.0)) * 0.6);
        if (a < 0.004) {
            discard;
        }
        fragColor = vec4(1.0, 1.0, 1.0, a);
        return;
    }
    vec2 pxPerUnit = vec2(1.0 / length(vec2(dFdx(local.x), dFdy(local.x))),
                          1.0 / length(vec2(dFdx(local.y), dFdy(local.y))));
    float fade = 1.0 - smoothstep(EXTENT * 0.3, EXTENT, r);
    float scale = white ? mix(1.0 / ZOOM, 1.0, smoothstep(1.5, EXTENT, r))
                        : 1.0 - K / (r * r) * fade * fade;               // nothing left at the panel edge
    vec2 size = vec2(textureSize(Sampler1, 0));
    vec2 plain = gl_FragCoord.xy / size;
    // drawn into a target of its own, which has no depth test: what is clearly nearer than the panel stays as it is (left clear).
    // Only clearly: the floor just under a hole is a little nearer than its centre, and must not cut the rings in half.
    float a = ProjMat[2][2], b = ProjMat[3][2];
    float panelDist = b / (gl_FragCoord.z * 2.0 - 1.0 + a);
    float behindHere = b / (texture(Sampler1, plain).r * 2.0 - 1.0 + a) - panelDist;
    if (behindHere < -IN_FRONT) {
        discard;
    }
    vec2 uv = clamp((gl_FragCoord.xy + local * pxPerUnit * (scale - 1.0)) / size, vec2(0.0), vec2(1.0));
    // The panel is a flat sheet through the hole, so a block standing next to the hole would be torn
    // in two (its far half bent, its near half not), and one in front of the hole would be dragged
    // into the bent view. Only what is well behind the hole is bent: how far behind it is (in blocks)
    // fades the bending in, for both the pixel shown and the pixel it would be taken from.
    float behindThere = b / (texture(Sampler1, uv).r * 2.0 - 1.0 + a) - panelDist;
    float bend = smoothstep(BEHIND_MIN, BEHIND_FULL, min(behindHere, behindThere));
    vec3 c = linear(mix(plain, uv, bend), size);
    if (!white) {
        c = mix(c, vec3(1.0), exp(-pow((r - 1.9) / 0.12, 2.0)) * 0.55);    // Einstein ring
        c = mix(c, vec3(1.0), exp(-pow((r - 1.05) / 0.05, 2.0)) * 0.6);    // photon ring
    } else {
        c = mix(c, vec3(1.0), pow(clamp(1.0 - (r - 1.0) / 0.8, 0.0, 1.0), 2.0) * 0.6);
    }
    fragColor = vec4(c, 1.0);
}
