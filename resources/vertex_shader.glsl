#version 330

in vec3 position;           // vertex position in local space
in vec3 normal;             // vertex normal in local space

out vec3 frag_normal;       // fragment normal in world space

uniform mat4 mvp_matrix;    // model-view-projection matrix

uniform float time;

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
    frag_normal = normal;
    
    // Rotate vertex
    vec3 p = rotate(time) * position;

    // Project vertex
    gl_Position = mvp_matrix * vec4(p, 1.0);
}