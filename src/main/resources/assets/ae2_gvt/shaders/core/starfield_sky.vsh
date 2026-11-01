#version 150

// The sky of the Shattered Starfield (see RiftClient): a box round the camera; Position is the view direction.

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec3 direction;
out vec2 uv;

void main() {
    // at the far plane, and depth-tested: behind all that is drawn, whenever the renderer gets to the sky
    gl_Position = (ProjMat * mat4(mat3(ModelViewMat)) * vec4(Position, 1.0)).xyww;
    direction = Position;
    uv = UV0;
}
