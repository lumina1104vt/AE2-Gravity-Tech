#version 150

// Hole lens panel (see HoleRenderTypes / HoleRenderer). ENTITY format:
//   UV0      = position on the panel, in hole radii (camera-facing, so x right / y up on screen)
//   Color.r  = 1 for a white hole

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 local;
out vec4 flags;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    local = UV0;
    flags = Color;
}
