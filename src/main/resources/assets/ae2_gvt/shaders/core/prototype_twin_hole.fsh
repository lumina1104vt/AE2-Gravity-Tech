#version 150

// PROTOTYPE - Twin-Star Hub twin holes. Port of hole_shader / starfield from the offline mock.
//  - inside: black hole = screen-space starfield (stars live in screen pixels like the End portal,
//    so they don't turn with the surface); white hole = the inverse, dark stars on white
//  - where they meet: a gradient band with mutual contamination - noise tendrils and specks of
//    each colour reaching into the other, twisted in opposite directions on the two sides
//  - rim: bright edge on black, dark edge on white, so the spheres still read as spheres

uniform float GameTime;

in vec3 pairPos;
in float proximity;
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
    float t = GameTime * 1200.0;   // GameTime is the fraction of the day cycle
    float prox = proximity;
    float h = pairPos.z;
    float rr = length(pairPos.xy);
    float a = atan(pairPos.y, pairPos.x);

    // opposite spins: two noise layers each turning rigidly - black side counter-clockwise, white
    // side clockwise - blended across the divide. (Warping one field by side * time instead would
    // shear without bound as time grows and break the divide up into fine noise.)
    float side = tanh(h / 0.6);
    float wBlack = 0.5 + 0.5 * side;
    float a1 = a + side * 1.1 * exp(-abs(h) / 1.3) * prox;   // fixed vortex twist at the divide

    float angB = a1 + t * 0.55;
    vec3 qB = vec3(rr * cos(angB), rr * sin(angB), h * 1.4);
    float angW = a1 - t * 0.55;
    vec3 qW = vec3(rr * cos(angW), rr * sin(angW), h * 1.4);
    vec3 drift = vec3(0.0, 0.0, t * 0.6);

    float tendrils = mix(fbm(qW * 0.9 + drift, 4), fbm(qB * 0.9 + drift, 4), wBlack) - 0.5;
    float specks = mix(fbm(qW * 3.2, 3), fbm(qB * 3.2, 3), wBlack) - 0.5;
    float field = h + prox * (2.4 * tendrils + 1.1 * specks);
    float w = 0.15 + 0.85 * prox;
    float x = clamp(0.5 + 0.5 * field / w, 0.0, 1.0);
    float black = x * x * (3.0 - 2.0 * x);

    float stars = starfield(gl_FragCoord.xy, t);
    float ndv = abs(dot(normalize(viewNormal), normalize(-viewPos)));
    float rim = pow(1.0 - ndv, 3.0);
    float darkSide = clamp(stars + rim * 140.0 / 255.0, 0.0, 1.0);
    float lightSide = clamp(1.0 - stars - rim * 110.0 / 255.0, 0.0, 1.0);
    float g = mix(lightSide, darkSide, black);
    fragColor = vec4(vec3(g), 1.0);
}
