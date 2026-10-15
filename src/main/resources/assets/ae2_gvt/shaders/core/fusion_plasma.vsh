#version 330

// Fusion plasma (see FusionPlasmaRenderTypes). Vanilla ENTITY vertex format:
//   Color    = the star's colour for its temperature
//   Normal   = the point's direction from the star centre (NOT transformed by the pose; the pose
//              only translates, so it doubles as the surface normal)
//   UV0.x    = 0 star surface, 1 corona shell, 2 containment shell, 3 prominence ribbon,
//              4 eruption blob (its corner -1..1 in Normal.xy)
//   UV0.y    = corona: shell radius / star radius; containment shell: angle from the eruption's
//              hit; ribbon: position across it (-1..1)
//   Color.a  = containment shell: the hit's age (0 = none); ribbon: its fade

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec3 dir;
out vec4 starColour;
out float layer;
out float shellScale;
out vec3 viewPos;
out vec3 viewNormal;

void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    viewPos = vp.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    dir = Normal;
    starColour = Color;
    layer = UV0.x;
    shellScale = UV0.y;
}
