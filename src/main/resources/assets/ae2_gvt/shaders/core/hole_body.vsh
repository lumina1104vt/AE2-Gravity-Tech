#version 330

// Hole body and accretion disc (see HoleRenderTypes / HoleRenderer). ENTITY format:
//   Color.r  = 1 for a white hole, Color.g = 1 for the disc
//   UV0      = disc: position in the disc's plane, in hole radii
//   Normal   = sphere: the point's direction from the centre (raw, the pose only translates)

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 flags;
out vec2 local;
out vec3 viewPos;
out vec3 viewNormal;

void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    viewPos = vp.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    flags = Color;
    local = UV0;
}
