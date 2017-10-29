#version 330

in vec3 frag_normal;    // fragment normal in world space
out vec3 colour;

const vec3 lightPosition = vec3(3, 2, 1);

uniform mat4 mvp_matrix;    // model-view-projection matrix

varying vec3 world_position;

void main()
{
    const vec3 BLACK = vec3(0, 0, 0);

    // Radius of a circle whose exterior matches the wires of the cube
    const float circleRadius = 1.379;
    if (length(world_position.xy) >= circleRadius ||
        length(world_position.yz) >= circleRadius ||
        length(world_position.xz) >= circleRadius)
    {
        colour = world_position;
    }
    else
        discard;
}