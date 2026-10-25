#version 150

// Fusion plasma: a small star, coloured by its temperature (exaggerated blackbody path, picked on
// the CPU and passed in as the vertex colour).
//  - surface: boiling granulation - bright cells with thin dark lanes (3D cellular noise on the
//    sphere, two scales, drifting slowly), limb darkening toward the edge
//  - corona (the far half of a larger shell, translucent): a soft glow just outside the disc,
//    flickering a little
//  - containment shell (layer 2, translucent): almost invisible - a faint rim and a fine lattice -
//    until an eruption hits it: a flash at the hit and a ring running out over the shell.
//    UV0.y = angle from the hit point, colour alpha = the hit's age (0 = none)
//  - prominence fibres and threads (layer 3, added light): thin ribbons, soft across
//    (UV0.y = -1..1), hot-white in the middle, faded by the colour alpha
//  - eruption clumps and flash (layer 4, added light): round soft spots; the corner position
//    (-1..1) comes in the normal

uniform float GameTime;

in vec3 dir;
in vec4 starColour;
in float layer;
in float shellScale;
in vec3 viewPos;
in vec3 viewNormal;

out vec4 fragColor;

vec3 hash33(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)), dot(p, vec3(269.5, 183.3, 246.1)), dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(p) * 43758.5453);
}

// F2 - F1 of 3D cellular noise: 0 on the lanes between cells, larger in the cells; the feature
// points wobble with time so the cells boil
float lanes(vec3 p, float t) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    float f1 = 8.0, f2 = 8.0;
    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 c = vec3(x, y, z);
                vec3 h = hash33(i + c);
                vec3 o = 0.5 + 0.38 * sin(t * (0.6 + h * 0.8) + 6.2831 * h);
                float d = length(c + o - f);
                if (d < f1) {
                    f2 = f1;
                    f1 = d;
                } else if (d < f2) {
                    f2 = d;
                }
            }
        }
    }
    return f2 - f1;
}

// the star's ramp round its colour: dark limb, the colour, white-hot granule middles
vec3 ramp(vec3 c, float v) {
    vec3 dark = c * 0.2, mid = c * 0.62, hot = c + (1.0 - c) * 0.45, top = c + (1.0 - c) * 0.85;
    if (v < 0.35) return mix(dark, mid, v / 0.35);
    if (v < 0.65) return mix(mid, c, (v - 0.35) / 0.3);
    if (v < 0.85) return mix(c, hot, (v - 0.65) / 0.2);
    return mix(hot, top, clamp((v - 0.85) / 0.15, 0.0, 1.0));
}

void main() {
    float t = GameTime * 1200.0;          // seconds-ish (GameTime is the fraction of the day)
    vec3 c = starColour.rgb;
    float ndv = clamp(abs(dot(normalize(viewNormal), normalize(-viewPos))), 0.0, 1.0);

    if (layer < 0.5) {
        vec3 d = normalize(dir);
        float g1 = 1.0 - exp(-lanes(d * 9.0, t * 0.5) * 6.0);
        float g2 = 1.0 - exp(-lanes(d * 23.0 + 3.1, t * 0.8) * 6.0);
        float g = 0.7 * g1 + 0.3 * g2;
        float limb = 0.55 + 0.45 * sqrt(ndv);
        fragColor = vec4(ramp(c, limb * (0.62 + 0.42 * g)), 1.0);
    } else if (layer > 3.5) {
        float d2 = dot(dir.xy, dir.xy);
        float fall = exp(-d2 * 3.125);                              // gaussian, ~0 at the quad's edge
        vec3 hot = c + (1.0 - c) * (0.3 + 0.5 * fall);
        fragColor = vec4(hot, clamp(fall * starColour.a, 0.0, 1.0));
    } else if (layer > 2.5) {
        float v = shellScale;                                       // across the ribbon
        float core = exp(-v * v * 4.0);
        float flick = 0.85 + 0.15 * sin(t * 9.0 + dir.x * 3.0);
        vec3 hot = c + (1.0 - c) * (0.35 + 0.5 * core);
        fragColor = vec4(hot, clamp((1.0 - v * v) * starColour.a * flick, 0.0, 1.0));
    } else if (layer > 1.5) {
        float rim = pow(1.0 - ndv, 4.0) * 0.22;
        vec3 d = normalize(dir);
        float lat = abs(sin(atan(d.z, d.x) * 12.0)) * abs(sin(acos(clamp(d.y, -1.0, 1.0)) * 12.0));
        float lattice = smoothstep(0.985, 1.0, 1.0 - lat) * 0.06;
        float glow = 0.0;
        float age = starColour.a;
        if (age > 0.0) {
            float ang = shellScale;
            float front = age * 2.6;                                  // the ring runs out over the shell
            float ring = exp(-pow((ang - front) * 7.0, 2.0)) * (1.0 - age);
            float flash = exp(-ang * ang * 18.0) * pow(1.0 - age, 3.0) * 1.4;
            float wake = smoothstep(front, 0.0, ang) * (1.0 - age) * 0.18;
            glow = ring * 0.9 + flash + wake;
        }
        vec3 col = mix(vec3(1.0), c, 0.45);
        fragColor = vec4(col, clamp(rim + lattice + glow, 0.0, 1.0));
    } else {
        // the far half of the shell (see the renderer): over the disc the star hides it by depth.
        // b = this view ray's distance from the centre in star radii; it only shapes the falloff,
        // so being slightly off under view bobbing doesn't show
        float b = max(1.0, sqrt(max(0.0, 1.0 - ndv * ndv)) * shellScale);
        float a = atan(dir.y, dir.x);
        float flick = 1.0 + 0.08 * sin(a * 7.0 + t) * sin(a * 3.0 - t * 2.0);
        float glow = pow(clamp((shellScale - b) / (shellScale - 1.0), 0.0, 1.0), 2.2) * flick;
        fragColor = vec4(c + (1.0 - c) * 0.35, clamp(glow * 0.85, 0.0, 1.0));
    }
}
