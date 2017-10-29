#version 330

in vec3 position;           // vertex position in local space
in vec3 normal;             // vertex normal in local space

out vec3 frag_normal;       // fragment normal in world space

uniform mat4 mvp_matrix;    // model-view-projection matrix

uniform float time;

varying vec3 world_position;

mat3 rotate(float angle)
{
    float s = sin(angle);
    float c = cos(angle);

    return mat3( c, 0, s,
                 0, 1, 0,
                -s, 0, c);
}

void main()
{
    // Rotate vertex
    vec3 p = rotate(time) * position;

    frag_normal = rotate(time) * normal;
    world_position = p;

    // Project vertex
    gl_Position = mvp_matrix * vec4(p, 1.0);
}