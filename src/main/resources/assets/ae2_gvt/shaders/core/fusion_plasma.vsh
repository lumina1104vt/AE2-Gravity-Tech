#version 150

// Fusion plasma (see FusionPlasmaRenderTypes). Vanilla ENTITY vertex format:
//   Color    = the star's colour for its temperature
//   Normal   = the point's direction from the star centre (NOT transformed by the pose; the pose
//              only translates, so it doubles as the surface normal)
//   UV0.x    = 0 star surface, 1 corona shell, 2 containment shell, 3 prominence ribbon,
//              4 eruption blob (its corner -1..1 in Normal.xy)
//   UV0.y    = corona: shell radius / star radius; containment shell: angle from the eruption's
//              hit; ribbon: position across it (-1..1)
//   Color.a  = containment shell: the hit's age (0 = none); ribbon: its fade

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

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
    // Some drivers number the vertex attributes by where the shader first uses them, not by the vertex format, so an attribute
    // this shader leaves out (or reads late) is fed the wrong data. Read all six at the top, in the format's order, and keep them alive.
    vec3 ae_p = Position; vec4 ae_c = Color; vec2 ae_t = UV0; ivec2 ae_o = UV1; ivec2 ae_l = UV2; vec3 ae_n = Normal;

    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    viewPos = vp.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    dir = Normal;
    starColour = Color;
    layer = UV0.x;
    shellScale = UV0.y;
    if (ae_o.x == 2147483647 && ae_l.x == 2147483647 && ae_n.x > 1000.0 && ae_c.a > 1000.0 && ae_t.x > 1.0e30 && ae_p.x > 1.0e30)
        gl_Position = vec4(2.0);   // never true: only so the attributes above are not optimised away
}
