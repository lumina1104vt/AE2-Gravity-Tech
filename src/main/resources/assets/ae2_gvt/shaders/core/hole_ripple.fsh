#version 150

// A gravitational-wave ripple round a hole: a clear spherical shell that only bends the world behind
// it. Seen face-on the shell does nothing; in a band just inside its rim the world is pushed out
// along the shell's normal, back to nothing at the rim itself (no hard edge), so on screen it is a
// clear ring that warps what is behind it. Sampler0 is
// a copy of the frame so far (SceneSnapshot). The push is in blocks, turned to pixels by the depth,
// so a far ripple warps less.

uniform sampler2D Sampler0;
uniform mat4 ProjMat;

in vec3 viewPos;
in vec3 viewNormal;
in vec2 params;

out vec4 fragColor;

void main() {
    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float facing = abs(dot(n, v));     // 0 at the rim, 1 face-on
    float edge = smoothstep(0.06, 0.28, facing) * (1.0 - smoothstep(0.28, 0.7, facing));
    float push = params.x * edge;
    float glow = params.y * edge;
    if (abs(push) < 0.0005 && glow < 0.003) {
        discard;
    }
    vec2 dir = length(n.xy) > 0.0001 ? normalize(n.xy) : vec2(0.0);
    vec2 size = vec2(textureSize(Sampler0, 0));
    float pxPerBlock = ProjMat[1][1] * 0.5 * size.y / max(-viewPos.z, 0.5);
    vec2 uv = (gl_FragCoord.xy - dir * push * pxPerBlock) / size;
    vec3 c = texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb;
    fragColor = vec4(c + vec3(glow), 1.0);
}
