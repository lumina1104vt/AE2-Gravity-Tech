#version 150

// Hole body and accretion disc.
//  - body: the twin-star hub's hole look - stars fixed to the screen inside the sphere (dark
//    stars on white for the white hole), a rim so it still reads as a sphere
//  - disc: black hole - violet-white matter glowing as it spirals in; white hole - dark matter
//    thrown out, turning the other way

uniform float GameTime;

in vec4 flags;
in vec2 local;
in vec3 viewPos;
in vec3 viewNormal;

out vec4 fragColor;

const float DISC_IN = 1.25, DISC_OUT = 2.7;   // HoleRenderer

float hash13(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

float vnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i);
    float n100 = hash13(i + vec3(1, 0, 0));
    float n010 = hash13(i + vec3(0, 1, 0));
    float n110 = hash13(i + vec3(1, 1, 0));
    float n001 = hash13(i + vec3(0, 0, 1));
    float n101 = hash13(i + vec3(1, 0, 1));
    float n011 = hash13(i + vec3(0, 1, 1));
    float n111 = hash13(i + vec3(1, 1, 1));
    return mix(mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
               mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y), u.z);
}

float fbm(vec3 p, int octaves) {
    float sum = 0.0, amp = 0.5, total = 0.0;
    for (int o = 0; o < octaves; o++) {
        sum += vnoise(p) * amp;
        total += amp;
        p = p * 2.03 + 7.1;
        amp *= 0.5;
    }
    return sum / total;
}

float hash21(vec2 c, float k) {
    return fract(sin(c.x * 12.9898 + c.y * 78.233 + k * 37.719) * 43758.5453);
}

// three parallax layers of stars in screen pixels, drifting slowly, plus faint nebula
float starfield(vec2 px, float t) {
    float n = fbm(vec3(px / 90.0, t * 0.05), 3);
    float val = (2.0 + 10.0 * n * n) / 255.0;
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
    float t = GameTime * 1200.0;
    bool white = flags.r > 0.5;
    if (flags.g < 0.5) {
        float stars = starfield(gl_FragCoord.xy, t);
        float ndv = abs(dot(normalize(viewNormal), normalize(-viewPos)));
        float rim = pow(1.0 - ndv, 3.0);
        float g = white ? clamp(1.0 - stars - rim * 110.0 / 255.0, 0.0, 1.0) : clamp(stars + rim * 140.0 / 255.0, 0.0, 1.0);
        fragColor = vec4(vec3(g), 1.0);
        return;
    }
    float rd = length(local);
    float ang = atan(local.y, local.x);
    float spin = white ? -1.0 : 1.0;
    float swirl = 0.5 + 0.5 * sin(ang * 3.0 - rd * 2.2 + spin * t * 2.0);
    float fall = clamp((DISC_OUT - rd) / (DISC_OUT - DISC_IN), 0.0, 1.0);
    float inner = smoothstep(DISC_IN, DISC_IN + 0.15, rd);
    if (!white) {
        float b = pow(fall, 1.5) * (0.55 + 0.45 * swirl);
        vec3 col = vec3(1.0, 0.78 * pow(b, 0.3), 1.0) * (0.55 + 0.45 * b);
        fragColor = vec4(col, clamp(b * 1.4, 0.0, 1.0) * inner);
    } else {
        fragColor = vec4(0.12, 0.12, 0.16, clamp(swirl * fall * 0.9, 0.0, 1.0) * inner);
    }
}
