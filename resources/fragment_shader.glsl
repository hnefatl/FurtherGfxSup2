#version 330

in vec3 frag_normal;    // fragment normal in world space
out vec3 colour;

const vec3 lightPosition = vec3(3, 2, 1);

uniform mat4 mvp_matrix;    // model-view-projection matrix

varying vec3 world_position;

void main()
{
    if (length(world_position) <= 1.3)
    {
        colour = vec3(1, 0, 0);
        return;
    }

    const float kA = 0.1;
    const float kD = 0.75;

    const vec3 WHITE = vec3(1, 1, 1);
    const vec3 BLACK = vec3(0, 0, 0);
    const vec3 BLUE = vec3(0, 0, 1);

    colour = BLACK;

    colour += kA * BLUE;

    vec3 n = normalize(frag_normal);
    vec3 l = normalize(lightPosition - world_position);
    float nl = dot(n, l);
    if (nl > 0)
        colour += kD * nl * BLUE;
}