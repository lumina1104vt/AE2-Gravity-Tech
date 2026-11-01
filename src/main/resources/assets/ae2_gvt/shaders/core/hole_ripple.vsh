#version 150

// Hole ripple shell (see HoleRipples). ENTITY format:
//   Normal = the shell's outward normal
//   UV0    = x: how far the world is pushed at the shell's rim, in blocks (negative pulls in)
//            y: how much the rim is brightened

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
out vec2 params;

void main() {
    vec4 p = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * p;
    viewPos = p.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    params = UV0;
}
