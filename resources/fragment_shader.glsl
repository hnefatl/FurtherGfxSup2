#version 330

in vec3 frag_normal;    // fragment normal in world space
out vec3 colour;

const vec3 lightPosition = vec3(3, 2, 1);

uniform mat4 mvp_matrix;    // model-view-projection matrix

varying vec3 world_position;

void main()
{
    const vec3 BLACK = vec3(0, 0, 0);

    int incidentEdges = 0;
    incidentEdges += abs(world_position.x) >= 0.9 ? 1 : 0;
    incidentEdges += abs(world_position.y) >= 0.9 ? 1 : 0;
    incidentEdges += abs(world_position.z) >= 0.9 ? 1 : 0;

    if (incidentEdges >= 2)        
        colour = BLACK;
    else
        discard;
}