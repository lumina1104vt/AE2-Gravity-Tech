#version 150

// Gravitational lensing round a hole: redraw the world behind the panel bent. Sampler0 is a copy
// of the frame so far (SceneSnapshot).
//  - black hole: a true lens - each pixel at r shows the scene at r - K / r, so what is behind the
//    hole is stretched round it, not along the radius - with an Einstein ring and a thin photon ring
//  - white hole: the opposite - a magnifier: near the hole each pixel shows a smaller piece of the
//    scene (ZOOM times larger), easing back to no change at the panel edge; never flipped or
//    folded - plus a tight white glow
// The panel's size on screen comes from the UV derivatives, so no screen-space data is passed in.

uniform sampler2D Sampler0;

in vec2 local;
in vec4 flags;

out vec4 fragColor;

const float EXTENT = 7.0;   // panel half-size in hole radii (HoleRenderer.LENS)
const float K = 3.61;       // lens strength: Einstein ring at sqrt(K) = 1.9 radii
const float ZOOM = 2.5;     // white hole: how much larger things look near it

void main() {
    float r = length(local);
    if (r < 1.0 || r > EXTENT) {
        discard;
    }
    bool white = flags.r > 0.5;
    vec2 pxPerUnit = vec2(1.0 / length(vec2(dFdx(local.x), dFdy(local.x))),
                          1.0 / length(vec2(dFdx(local.y), dFdy(local.y))));
    float fade = 1.0 - smoothstep(EXTENT * 0.3, EXTENT, r);
    float scale = white ? mix(1.0 / ZOOM, 1.0, smoothstep(1.5, EXTENT, r))
                        : 1.0 - K / (r * r) * fade * fade;               // nothing left at the panel edge
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 uv = (gl_FragCoord.xy + local * pxPerUnit * (scale - 1.0)) / size;
    vec3 c = texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb;
    if (!white) {
        c = mix(c, vec3(1.0), exp(-pow((r - 1.9) / 0.12, 2.0)) * 0.55);    // Einstein ring
        c = mix(c, vec3(1.0), exp(-pow((r - 1.05) / 0.05, 2.0)) * 0.6);    // photon ring
    } else {
        c = mix(c, vec3(1.0), pow(clamp(1.0 - (r - 1.0) / 0.8, 0.0, 1.0), 2.0) * 0.6);
    }
    fragColor = vec4(c, 1.0);
}
