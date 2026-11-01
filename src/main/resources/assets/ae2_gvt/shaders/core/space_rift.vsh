#version 150

// The tear's panel (see RiftClient). ENTITY format: UV0 = position on the panel, -1 ... 1; Color.r = a
// value of its own for each tear, so no two are the same shape.

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 local;
out float seed;
out float perUnit;

const float PANEL_HALF_WIDTH = 3.5;     // blocks: RiftClient.WIDE

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    // fractions of the screen's height per panel unit, from the depth: see the fragment shader
    perUnit = ProjMat[1][1] * 0.5 / max(-view.z, 0.5) * PANEL_HALF_WIDTH;
    local = UV0;
    seed = Color.r;
}
