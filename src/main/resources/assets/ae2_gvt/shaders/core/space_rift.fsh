#version 150

// A tear in space: an irregular upright slit with cracks branching off it, stars behind it, the
// world bent away from it round the edges and a thin bright rim. Its two edges are shaped apart, so it
// gapes unevenly. Sampler0 is a copy of the frame so far (SceneSnapshot).

uniform sampler2D Sampler0;
uniform float GameTime;

in vec2 local;
in float seed;
in float perUnit;

out vec4 fragColor;

const int BRANCHES = 6;
const float REACH = 0.4, PUSH = 0.05;     // on the panel, whose half width is 1

float hash(float x) {
    return fract(sin(x * 91.17 + seed * 37.3) * 43758.5453);
}

float hash2(vec2 c) {
    return fract(sin(dot(c, vec2(12.9898, 78.233))) * 43758.5453);
}

// a line of straight pieces, joined: what makes the edges jagged rather than wavy
float jag(float y, float cells, float k) {
    float p = y * cells + k, i = floor(p), f = p - i;
    return mix(hash(i), hash(i + 1.0), f) * 2.0 - 1.0;
}

float taper(float y) {
    return max(1.0 - y * y, 0.0);       // closes to a point at both ends
}

float spine(float y) {
    return (jag(y, 3.0, 2.0) * 0.2 + jag(y, 8.0, 5.0) * 0.08 + jag(y, 19.0, 9.0) * 0.025) * taper(y);
}

// signed distance to the slit: negative inside (only roughly a distance; good near the edge)
float slit(vec2 p, float t) {
    float c = spine(p.y);
    float breathe = 1.0 + 0.1 * sin(t * 0.8 + p.y * 5.0);
    float left = taper(p.y) * (0.03 + 0.1 * abs(jag(p.y, 6.0, 13.0)) + 0.03 * abs(jag(p.y, 17.0, 21.0))) * breathe;
    float right = taper(p.y) * (0.03 + 0.1 * abs(jag(p.y, 5.0, 31.0)) + 0.03 * abs(jag(p.y, 15.0, 37.0))) * breathe;
    return max(c - left - p.x, p.x - c - right);
}

// signed distance to the cracks branching off the slit, thinning towards their ends
float branches(vec2 p) {
    float d = 9.0;
    for (int i = 0; i < BRANCHES; i++) {
        float k = float(i) + 1.0;
        float y = (hash(k) - 0.5) * 1.5;
        float side = hash(k + 7.0) > 0.5 ? 1.0 : -1.0;
        vec2 a = vec2(spine(y), y);
        vec2 b = a + vec2(side * (0.2 + 0.45 * hash(k + 3.0)), (hash(k + 5.0) - 0.5) * 0.5);
        vec2 ab = b - a;
        float along = clamp(dot(p - a, ab) / dot(ab, ab), 0.0, 1.0);
        d = min(d, length(p - a - ab * along) - 0.022 * (1.0 - along));
    }
    return d;
}

void main() {
    float t = GameTime * 1200.0;
    vec2 p = local + vec2(jag(local.y, 29.0, 41.0), jag(local.x, 29.0, 43.0)) * 0.008;
    float body = slit(p, t);
    float d = min(body, branches(p));

    if (d < 0.0) {
        vec2 c = floor(gl_FragCoord.xy / 7.0);
        vec2 s = vec2(hash2(c), hash2(c + 3.1));
        vec2 o = fract(gl_FragCoord.xy / 7.0) - s;
        float star = pow(hash2(c + 9.7), 8.0) * exp(-dot(o, o) * 40.0);
        fragColor = vec4(vec3(0.015, 0.01, 0.03) + vec3(0.9, 0.9, 1.0) * star, 1.0);
        return;
    }
    // outside: the world is pushed away from the slit, less and less further out. Going by the main slit
    // only, and PUSH well under REACH: a push that falls off as fast as a pixel moves samples one spot for
    // a whole row, and the world smears into streaks
    float fall = 1.0 - smoothstep(0.0, REACH * taper(local.y) + 0.001, max(body, 0.0));
    float rim = exp(-d * d / 0.0004);
    if (fall <= 0.0 && rim < 0.01) {
        discard;
    }
    vec2 size = vec2(textureSize(Sampler0, 0));
    // from the depth, not from screen derivatives of local: those blow up under some renderers, and the
    // world is then pulled in from the edge of the screen, row by row. And never more than a little anyway
    float pxPerUnit = perUnit * size.y;
    float side = sign(local.x - spine(local.y));
    float push = min(fall * fall * PUSH * taper(local.y) * pxPerUnit, size.y * 0.04);
    vec2 uv = (gl_FragCoord.xy - vec2(side * push, 0.0)) / size;
    vec3 c = texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb;
    fragColor = vec4(c + vec3(0.75, 0.7, 1.0) * rim, 1.0);
}
