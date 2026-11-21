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
    // Some drivers number the vertex attributes by where the shader first uses them, not by the vertex format, so an attribute
    // this shader leaves out (or reads late) is fed the wrong data. Read all six at the top, in the format's order, and keep them alive.
    vec3 ae_p = Position; vec4 ae_c = Color; vec2 ae_t = UV0; ivec2 ae_o = UV1; ivec2 ae_l = UV2; vec3 ae_n = Normal;

    gl_Position = vec4(UV0 * 2.0 - 1.0, 0.0, 1.0);
    direction = Position;
    if (ae_o.x == 2147483647 && ae_l.x == 2147483647 && ae_n.x > 1000.0 && ae_c.a > 1000.0 && ae_t.x > 1.0e30 && ae_p.x > 1.0e30)
        gl_Position = vec4(2.0);   // never true: only so the attributes above are not optimised away
}
