#version 150

// One face of the sky's box, filling the target; Position is the view direction.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec3 direction;

void main() {
    gl_Position = vec4(UV0 * 2.0 - 1.0, 0.0, 1.0);
    direction = Position;
}
