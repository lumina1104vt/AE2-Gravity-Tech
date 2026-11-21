#version 150

// Hexagram hub: the drawn cosmos sphere (see client HexagramHubRenderer).
// Entity vertex format; the normal feeds the bright rim only (the sky itself is in screen pixels).

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

void main() {
    // Some drivers number the vertex attributes by where the shader first uses them, not by the vertex format, so an attribute
    // this shader leaves out (or reads late) is fed the wrong data. Read all six at the top, in the format's order, and keep them alive.
    vec3 ae_p = Position; vec4 ae_c = Color; vec2 ae_t = UV0; ivec2 ae_o = UV1; ivec2 ae_l = UV2; vec3 ae_n = Normal;

    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    viewPos = vp.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    if (ae_o.x == 2147483647 && ae_l.x == 2147483647 && ae_n.x > 1000.0 && ae_c.a > 1000.0 && ae_t.x > 1.0e30 && ae_p.x > 1.0e30)
        gl_Position = vec4(2.0);   // never true: only so the attributes above are not optimised away
}
