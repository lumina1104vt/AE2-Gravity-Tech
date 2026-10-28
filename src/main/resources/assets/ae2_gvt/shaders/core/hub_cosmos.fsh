#version 150

// Hexagram hub: the inside of the sphere seen from outside - stars and violet / blue nebula fixed
// to the screen like the black hole's (they do not shift as the player moves), brighter towards the
// sphere's silhouette. Only the far (inner) wall is
// drawn, so the twin holes in the middle stay in front of it.

uniform float GameTime;

in vec3 viewPos;
in vec3 viewNormal;

out vec4 fragColor;

float hash13(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

float vnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash13(i), hash13(i + vec3(1, 0, 0)), u.x), mix(hash13(i + vec3(0, 1, 0)), hash13(i + vec3(1, 1, 0)), u.x), u.y),
               mix(mix(hash13(i + vec3(0, 0, 1)), hash13(i + vec3(1, 0, 1)), u.x), mix(hash13(i + vec3(0, 1, 1)), hash13(i + vec3(1, 1, 1)), u.x), u.y), u.z);
}

float fbm(vec3 p) {
    float sum = 0.0, amp = 0.5;
    for (int o = 0; o < 5; o++) {
        sum += vnoise(p) * amp;
        p = p * 2.03 + 7.1;
        amp *= 0.5;
    }
    return sum / 0.97;
}

float hash21(vec2 c, float k) {
    return fract(sin(c.x * 12.9898 + c.y * 78.233 + k * 37.719) * 43758.5453);
}

// the black hole's starfield (hole_body.fsh): three layers of stars in screen pixels, drifting slowly
float starfield(vec2 px, float t) {
    float val = 0.0;
    const vec4 LAYERS[3] = vec4[3](vec4(6.0, 1.2, 0.4, 170.0), vec4(10.0, 0.7, -0.5, 230.0), vec4(17.0, 0.35, 0.2, 255.0));
    const float SIZES[3] = float[3](0.7, 1.1, 1.8);
    for (int l = 0; l < 3; l++) {
        float cell = LAYERS[l].x;
        vec2 uv = px / cell + LAYERS[l].yz * t * 0.1 + vec2(float(l) * 3.1, float(l) * 1.7);
        vec2 c = floor(uv);
        vec2 star = vec2(hash21(c, float(l) + 1.0), hash21(c, float(l) + 11.0));
        float b = pow(hash21(c, float(l) + 21.0), 3.0);
        vec2 d = (uv - c - star) * cell;
        float twinkle = 0.75 + 0.25 * sin(t * 3.0 + hash21(c, float(l) + 31.0) * 40.0);
        val += LAYERS[l].w / 255.0 * b * twinkle * exp(-dot(d, d) / SIZES[l]);
    }
    return clamp(val, 0.0, 1.0);
}

void main() {
    float t = GameTime * 1200.0;   // seconds
    // everything lives in screen pixels like the black hole: it stays put however the player moves
    vec2 px = gl_FragCoord.xy;
    float neb = fbm(vec3(px / 260.0, t * 0.02));
    float wisps = fbm(vec3(px / 120.0 + 31.0, t * 0.03));
    vec3 col = vec3(0.012, 0.008, 0.035);
    col += vec3(0.30, 0.10, 0.55) * pow(neb, 3.0) * 1.6;
    col += vec3(0.08, 0.20, 0.55) * pow(wisps, 4.0) * 1.4;
    col += vec3(0.95, 0.95, 1.0) * starfield(px, t);

    // brighter rim where the sphere turns away from the eye
    float ndv = abs(dot(normalize(viewNormal), normalize(-viewPos)));
    col += vec3(0.35, 0.25, 0.6) * pow(1.0 - ndv, 4.0);
    fragColor = vec4(col, 1.0);
}
