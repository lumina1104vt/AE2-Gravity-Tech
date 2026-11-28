#version 150

// Black / white hole computing core: all its effects in one program (client VoxelCoreRenderer).
// Entity vertex format. UV1 = (mode, parameter), UV2.x = a second parameter, UV0 = the effect's own coordinates.

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec3 viewPos;
out vec3 viewNormal;
out vec3 worldPos;      // from the camera, world axes (the pose is applied on the CPU)
out vec3 worldNormal;
out vec4 vertexColor;
out vec2 coord;
flat out ivec3 data;

void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    viewPos = vp.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    worldPos = Position;
    worldNormal = Normal;
    vertexColor = Color;
    coord = UV0;
    data = ivec3(UV1, UV2.x);
}
