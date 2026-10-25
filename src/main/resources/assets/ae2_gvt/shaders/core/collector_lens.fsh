#version 150

// Space distortion inside the graviton collector (the gravitons are invisible; only space bends):
// the world behind the panel swirls toward the core and is magnified near it, with rings running
// inward. Strongest at the core, nothing at the panel's edge. Sampler0 is a copy of the frame so
// far (SceneSnapshot). The panel's size on screen comes from the UV derivatives.

uniform float GameTime;

uniform sampler2D Sampler0;

in vec2 local;

out vec4 fragColor;

const float TAU = 6.2831853;

void main() {
    float r = length(local);
    if (r > 1.0) {
        discard;
    }
    float t = GameTime * 1200.0 / 2.2;                      // one ring every 2.2 s or so
    float fall = pow(1.0 - r, 1.5);
    float wave = sin(TAU * (2.5 * r + t));                  // phase grows outward -> rings move in
    float zoom = fall * (0.32 + 0.28 * wave);
    float twist = fall * 1.1 * (0.6 + 0.4 * wave);
    // to screen pixels, so the swirl is round on screen whatever the panel's shape
    vec2 pxPerUnit = vec2(1.0 / length(vec2(dFdx(local.x), dFdy(local.x))),
                          1.0 / length(vec2(dFdx(local.y), dFdy(local.y))));
    vec2 p = local * pxPerUnit;
    vec2 q = p * (1.0 + zoom);
    float c = cos(twist), s = sin(twist);
    q = vec2(q.x * c - q.y * s, q.x * s + q.y * c);
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 uv = (gl_FragCoord.xy + (q - p)) / size;
    fragColor = vec4(texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb, 1.0);
}
