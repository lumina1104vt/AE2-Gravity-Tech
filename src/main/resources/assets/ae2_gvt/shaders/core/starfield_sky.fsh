#version 150

// The sky of the Shattered Starfield, read from its baked faces (starfield_bake); only the glow of the cracks moves.

uniform sampler2D Sampler0;
uniform float GameTime;

in vec3 direction;
in vec2 uv;

out vec4 fragColor;

void main() {
    vec4 sky = texture(Sampler0, uv);
    float t = GameTime * 1200.0;
    vec3 d = normalize(direction);
    fragColor = vec4(sky.rgb + vec3(0.45, 0.4, 0.8) * sky.a * (0.6 + 0.2 * sin(t + dot(d, vec3(5.0, 7.0, 3.0)))), 1.0);
}
