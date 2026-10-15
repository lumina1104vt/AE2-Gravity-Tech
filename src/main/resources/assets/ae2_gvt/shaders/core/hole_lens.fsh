#version 330

// Gravitational lensing round a hole: redraw the world behind the panel bent. Sampler0 is a copy
// of the frame so far (SceneSnapshot).
//  - black hole: each pixel shows the scene from further out (light bent round the hole): the
//    background wraps round it, with an Einstein ring and a thin photon ring at the edge
//  - white hole: the opposite - light pushed out, so the view behind it shrinks - plus a white glow
// The panel's size on screen comes from the UV derivatives, so no screen-space data is passed in.

uniform sampler2D Sampler0;

in vec2 local;
in vec4 flags;

out vec4 fragColor;

const float EXTENT = 4.5;   // panel half-size in hole radii (HoleRenderer.LENS)
const float K = 3.61;       // lens strength: Einstein ring at sqrt(K) = 1.9 radii

void main() {
    float r = length(local);
    if (r < 1.0 || r > EXTENT) {
        discard;
    }
    bool white = flags.r > 0.5;
    vec2 pxPerUnit = vec2(1.0 / length(vec2(dFdx(local.x), dFdy(local.x))),
                          1.0 / length(vec2(dFdx(local.y), dFdy(local.y))));
    float scale = white ? 1.0 / (1.0 + K / (r * r)) : 1.0 + K / (r * r);
    scale = 1.0 + (scale - 1.0) * smoothstep(EXTENT, EXTENT * 0.6, r);   // fades out at the panel edge
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 uv = (gl_FragCoord.xy + local * pxPerUnit * (scale - 1.0)) / size;
    vec3 c = texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb;
    if (!white) {
        c = mix(c, vec3(1.0), exp(-pow((r - 1.9) / 0.12, 2.0)) * 0.55);    // Einstein ring
        c = mix(c, vec3(1.0), exp(-pow((r - 1.05) / 0.05, 2.0)) * 0.6);    // photon ring
    } else {
        c = mix(c, vec3(1.0), pow(clamp(1.0 - (r - 1.0) / 2.0, 0.0, 1.0), 2.0) * 0.75);
    }
    fragColor = vec4(c, 1.0);
}
