#version 330

in vec3 position;           // vertex position in local space
in vec3 normal;             // vertex normal in local space

out vec3 frag_normal;       // fragment normal in world space

uniform mat4 mvp_matrix;    // model-view-projection matrix

void main()
{
    frag_normal = normal;

    gl_Position = mvp_matrix * vec4(position, 1.0);
}