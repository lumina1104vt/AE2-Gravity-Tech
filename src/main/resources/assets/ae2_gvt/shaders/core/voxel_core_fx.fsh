#version 150

// Ports of the draft's fragment shaders (3d.html), one branch per effect.

uniform float GameTime;

in vec3 viewPos;
in vec3 viewNormal;
in vec3 worldPos;
in vec3 worldNormal;
in vec4 vertexColor;
in vec2 coord;
flat in ivec3 data;

out vec4 fragColor;

const float LOAD = 0.78;   // the draft's default compute load
// the draft glows against black space; in a lit world the same numbers wash everything out
const float DIM = 0.5;     // how much of the added light is kept

vec3 bridgeColor(float t, vec3 a, vec3 b, vec3 c) {
    return t < 0.5 ? mix(a, b, smoothstep(0.05, 0.5, t)) : mix(b, c, smoothstep(0.5, 0.95, t));
}

// ---- the starry sky seen in the black hole; it hangs on the view direction, so it has depth like a window ----
vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

vec3 turn(vec3 d, float a) {         // about the polar axis
    float c = cos(a), s = sin(a);
    return vec3(c * d.x - s * d.z, d.y, s * d.x + c * d.z);
}

// One layer of stars on a cube round the viewer. x = brightness, y = which tint (0..1), z = the four rays
// of the bigger ones.
vec3 starLayer(vec3 d, float scale, float amount, float time) {
    vec3 a = abs(d);
    vec2 uv;
    float face;
    if (a.x >= a.y && a.x >= a.z) { uv = d.yz / a.x; face = d.x > 0.0 ? 1.0 : 2.0; }
    else if (a.y >= a.z) { uv = d.xz / a.y; face = d.y > 0.0 ? 3.0 : 4.0; }
    else { uv = d.xy / a.z; face = d.z > 0.0 ? 5.0 : 6.0; }
    float pixel = length(fwidth(d)) * scale;      // one screen pixel, in cells
    vec2 p = uv * scale;
    vec3 h = hash33(vec3(floor(p) + 64.0, face * 17.0 + scale));
    if (h.x > amount) return vec3(0.0);
    vec3 k = hash33(h * 91.7 + 5.0);
    float size = 0.06 + 0.10 * k.x * k.x;
    float drawn = max(size, pixel * 0.7);         // never thinner than a pixel, or it flickers
    vec2 at = p - floor(p) - 0.25 - 0.5 * h.yz;
    float dist = length(at);
    float twinkle = 0.72 + 0.28 * sin(time * (0.8 + 2.2 * k.y) + k.z * 6.2832);
    vec2 q = abs(at);
    float thin = max(0.022, pixel * 0.5);
    float rays = max((1.0 - smoothstep(0.0, thin, q.y)) * max(0.0, 1.0 - q.x / 0.25),
                     (1.0 - smoothstep(0.0, thin, q.x)) * max(0.0, 1.0 - q.y / 0.25)) * step(0.095, size);
    return vec3((1.0 - smoothstep(drawn * 0.5, drawn * 1.5, dist)) * min(1.0, size / drawn + 0.25) * twinkle, k.z,
                rays * twinkle);
}

// a very faint cloud behind the stars
float nebula(vec3 d) {
    float n = sin(d.x * 5.1 + sin(d.y * 4.3)) * sin(d.y * 4.7 + sin(d.z * 5.9)) * sin(d.z * 4.1 + sin(d.x * 6.7));
    return smoothstep(0.0, 0.8, n);
}

void main() {
    float time = GameTime * 1200.0;   // seconds
    int mode = data.x;
    float facing = dot(normalize(viewNormal), -normalize(viewPos));

    if (mode == 0) {                 // black hole: black, with a photon ring at the rim. coord.y = local height
        float ndotv = max(facing, 0.0);
        float photon = pow(1.0 - ndotv, 5.5);
        float rim = pow(1.0 - ndotv, 2.8) * 0.35;
        float ripple = 0.85 + 0.15 * sin(time * 4.5 + coord.y * 2.2);
        vec3 ring = mix(vec3(0.95, 0.32, 0.05), vec3(1.0, 0.80, 0.42), photon);
        // the sky behind, pulled outwards near the rim as the hole bends its light
        vec3 d = normalize(worldPos);
        if (facing >= 0.0) d = normalize(d + normalize(worldNormal) * pow(1.0 - ndotv, 2.0) * 0.6);
        d = turn(d, time * 0.006);
        vec3 big = starLayer(d, 22.0, 0.24, time), small = starLayer(d, 52.0, 0.17, time);
        vec3 tint = big.y < 0.6 ? vec3(0.92, 0.95, 1.0) : big.y < 0.82 ? vec3(0.55, 0.82, 1.0) : vec3(1.0, 0.80, 0.50);
        vec3 sky = tint * (big.x * 0.95 + big.z * 0.55) + vec3(0.70, 0.72, 0.90) * small.x * 0.5
                + vec3(0.140, 0.050, 0.018) * nebula(d) + vec3(0.075, 0.016, 0.040) * nebula(d.zxy * 1.7);
        fragColor = vec4(sky + ring * (photon * (2.6 + LOAD * 1.2) + rim) * ripple, 1.0);
        // seen from inside, every point would count as the rim and glow white: inside is only the sky
        if (facing < 0.0) fragColor = vec4(sky, 1.0);
    } else if (mode == 1) {          // white hole: the black one inside out
        float ndotv = max(facing, 0.0);
        // the same all the way round (horizontal bands made it a gas giant): a soft white heart, silver blue towards the
        // rim and a bright pale ring on it. Not near-pure white, which is harsh to look at.
        vec3 col = mix(vec3(0.62, 0.70, 0.82), vec3(0.87, 0.87, 0.86), smoothstep(0.15, 0.95, ndotv));
        col *= 1.0 - 0.05 * (0.5 + 0.5 * sin((1.0 - ndotv) * 15.0 - time * 2.4));   // faint waves going outwards
        col = mix(col, vec3(0.84, 0.92, 1.0), pow(1.0 - ndotv, 3.0));
        if (facing < 0.0) col = vec3(0.55, 0.59, 0.66);
        fragColor = vec4(col, 1.0);
    } else if (mode == 2) {          // corona: only the far wall of its sphere. parameter = intensity * 100
        if (facing > 0.0) discard;
        // fades out towards its rim (the draft draws a flat, hard-edged disc)
        fragColor = vec4(vertexColor.rgb, pow(-facing, 2.0) * float(data.y) / 100.0 * 0.45);
        if (vertexColor.g > 0.8) fragColor.a *= 0.4;   // the white hole's two (white and pale blue) add up: keep them faint
    } else if (mode == 3) {          // blocky disk. coord = position in the disk's plane; parameter 0 black, 1 white
        bool white = data.y == 1;
        float flow = white ? -1.0 : 1.0;
        float inner = white ? 7.6 : 8.2;
        float outer = white ? 19.5 : 22.0;
        vec3 near = white ? vec3(1.0) : vec3(1.0, 0.56, 0.18);
        vec3 far = white ? vec3(0.28, 0.52, 0.95) : vec3(0.70, 0.13, 0.03);

        vec2 cell = floor(coord) + 0.5;
        float r = length(cell);
        if (r < inner || r > outer) discard;
        float n = clamp((r - inner) / (outer - inner), 0.0, 1.0);
        float angle = atan(cell.y, cell.x);
        float s1 = sin(angle * 4.0 + n * 15.0 * flow - time * 3.2 * flow);
        float s2 = sin(angle * 8.0 - n * 9.0 * flow - time * 2.0 * flow);
        float density = floor(clamp(0.48 + 0.34 * s1 + 0.18 * s2, 0.0, 1.0) * 5.0) / 5.0;
        vec2 f = fract(coord);
        float border = step(0.08, f.x) * step(f.x, 0.92) * step(0.08, f.y) * step(f.y, 0.92);
        float fade = smoothstep(0.0, 0.15, n) * (1.0 - smoothstep(0.75, 1.0, n));
        float hot = pow(1.0 - n, 2.6) * 1.25;
        vec3 col = mix(near, far, floor(n * 6.0) / 6.0);
        col *= (0.85 + hot + LOAD * 0.5) * (0.82 + 0.18 * border);
        if (!white) col *= 0.7;    // at full strength the orange clips to a muddy yellow-green
        float alpha = clamp((density * 0.85 + hot * 0.4) * fade, 0.0, 0.92);
        if (alpha < 0.08) discard;
        fragColor = vec4(col, alpha);
    } else if (mode == 4) {          // wormhole bridge. coord = (around 0..1, black 0 .. white 1); parameter 0 sheath, 1 core
        float t = coord.y, a = coord.x * 6.2831853;
        // everything runs from the black hole to the white one
        float strand = pow(0.5 + 0.5 * sin(3.0 * a + t * 26.0 - time * 3.4 + 1.5 * sin(t * 11.0 - time * 1.7)), 5.0);
        float fine = pow(0.5 + 0.5 * sin(-5.0 * a + t * 47.0 - time * 5.6 + sin(2.0 * a + t * 7.0)), 9.0);
        float pulse = pow(0.5 + 0.5 * sin(t * 15.0 - time * 5.0), 10.0);
        vec3 col = bridgeColor(t, vec3(0.98, 0.42, 0.08), vec3(0.70, 0.45, 0.95), vec3(0.80, 0.92, 1.0));
        float fade = smoothstep(0.0, 0.09, t) * (1.0 - smoothstep(0.91, 1.0, t));
        float soft = pow(abs(facing), 1.5);          // no hard outline: it thins out towards the sides
        if (data.y == 1) {
            col = mix(col, vec3(1.0), 0.30 + 0.25 * pulse);
            fragColor = vec4(col, clamp((0.50 + 0.40 * pulse + 0.20 * strand) * soft * fade, 0.0, 0.95));
        } else {
            col = mix(col, vec3(1.0), fine * 0.35);
            fragColor = vec4(col * 1.2, clamp((0.24 + 0.80 * strand + 0.70 * fine + 0.35 * pulse) * soft * fade, 0.0, 0.9));
        }
    } else if (mode == 5) {          // helical filament. coord.x = black 0 .. white 1; parameter = filament number
        float f = float(data.y);
        float phase = f * 1.15;
        float speed = 1.8 + mod(f, 3.0) * 0.55;
        float t = coord.x;
        float bright = pow(0.5 + 0.5 * sin(t * 22.0 + time * speed * (1.2 + LOAD) + phase), 2.4);
        vec3 col = bridgeColor(t, vec3(1.0, 0.55, 0.18), vec3(0.78, 0.55, 1.0), vec3(0.82, 0.93, 1.0));
        float fade = smoothstep(0.0, 0.08, t) * (1.0 - smoothstep(0.92, 1.0, t));
        fragColor = vec4(col * (0.85 + bright * 0.45), clamp((0.18 + bright * 0.70) * fade, 0.0, 0.9));
    } else if (mode == 7) {          // polar jet. coord = (around 0..1, pole 0 .. tip 1); parameter 0 sheath, 1 core
        float t = coord.y, a = coord.x * 6.2831853;
        float streak = pow(0.5 + 0.5 * sin(3.0 * a + t * 30.0 - time * 7.0 + sin(t * 13.0 - time * 2.0)), 4.0);
        float knot = pow(0.5 + 0.5 * sin(t * 34.0 - time * 6.5), 12.0);     // bright clumps travelling outwards
        float fade = smoothstep(0.0, 0.05, t) * pow(1.0 - t, 1.3);          // dies away instead of ending
        if (data.y == 1) {
            vec3 col = mix(vertexColor.rgb, vec3(1.0), 0.40 + 0.35 * knot);
            fragColor = vec4(col, clamp((0.75 + 0.50 * knot) * pow(abs(facing), 0.8) * fade * 1.5, 0.0, 0.95));
        } else {
            float alpha = (0.16 + 0.60 * streak + 0.55 * knot) * pow(abs(facing), 1.6) * fade;
            fragColor = vec4(vertexColor.rgb * 1.15, clamp(alpha * 1.3, 0.0, 0.85));
        }
    } else {                         // plain colour
        fragColor = vertexColor;
    }
    if (mode >= 3) fragColor.a *= DIM;   // everything added except the coronas, which are set above
}
