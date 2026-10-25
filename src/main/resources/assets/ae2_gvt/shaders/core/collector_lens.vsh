#version 150

// Graviton collector distortion panel (see CollectorClient). ENTITY format:
//   UV0 = position on the panel, -1..1 each way (camera-facing, so x right / y up on screen)

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 local;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    local = UV0;
}
