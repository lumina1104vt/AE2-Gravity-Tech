#version 150

// PROTOTYPE - Twin-Star Hub twin holes (see TwinHoleRenderTypes).
// Vertex data is packed into the vanilla ENTITY format:
//   UV0      = position in the pair frame, across the dividing plane (e1, e2)
//   UV1.x    = signed distance from the dividing plane * 1000 (+ = black side)
//   UV1.y    = proximity of the pair * 1000 (0 far apart .. 1000 touching / merged)
//   Normal   = surface normal (for the rim)

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec3 pairPos;
out float proximity;
out vec3 viewPos;
out vec3 viewNormal;

void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    viewPos = vp.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    pairPos = vec3(UV0, float(UV1.x) / 1000.0);
    proximity = float(UV1.y) / 1000.0;
}
