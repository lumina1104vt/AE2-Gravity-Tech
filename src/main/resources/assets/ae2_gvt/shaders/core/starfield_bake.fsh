#version 150

// The sky of the Shattered Starfield: stars and faint nebula, broken into plates along thin glowing cracks; each plate
// shows the stars a little shifted, so the sky itself looks shattered. Far too much for every pixel of every frame: drawn
// once into the six faces of a box (RiftClient); alpha holds the glow of the cracks, which starfield_sky makes pulse.

in vec3 direction;

out vec4 fragColor;

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

vec3 hash3(vec3 p) {
    return vec3(hash(p), hash(p + 17.3), hash(p + 41.9));
}

float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1, 0, 0)), u.x), mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), u.x), u.y),
               mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), u.x), mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), u.x), u.y), u.z);
}

float stars(vec3 d, float cells, float size) {
    vec3 p = d * cells, c = floor(p);
    vec3 o = p - c - hash3(c);
    float b = pow(hash(c + 5.1), 12.0);
    return b * exp(-dot(o, o) / size);
}

void main() {
    vec3 d = normalize(direction);
    // the plates: nearest of a few scattered points; the gap to the second nearest is 0 on a crack
    vec3 p = d * 2.6, c = floor(p);
    float first = 9.0, second = 9.0;
    vec3 plate = vec3(0.0);
    for (int x = -1; x <= 1; x++)
        for (int y = -1; y <= 1; y++)
            for (int z = -1; z <= 1; z++) {
                vec3 cell = c + vec3(x, y, z);
                float dist = length(p - cell - hash3(cell));
                if (dist < first) {
                    second = first;
                    first = dist;
                    plate = cell;
                } else if (dist < second) {
                    second = dist;
                }
            }
    vec3 shifted = normalize(d + (hash3(plate) - 0.5) * 0.14);
    float nebula = noise(shifted * 3.0) * noise(shifted * 6.0 + 3.0);
    vec3 col = vec3(0.012, 0.01, 0.03) + vec3(0.10, 0.06, 0.20) * nebula * nebula * 3.0;
    col += vec3(1.0) * (stars(shifted, 90.0, 0.04) + stars(shifted + 2.0, 160.0, 0.06));
    float gap = second - first;
    col *= smoothstep(0.0, 0.03, gap) * 0.5 + 0.5;                                   // dark along the crack
    fragColor = vec4(col, exp(-gap * gap / 0.0002));
}
