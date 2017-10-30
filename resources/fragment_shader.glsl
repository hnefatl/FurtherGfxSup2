#version 330

in vec3 frag_normal;    // fragment normal in world space
in vec2 uv;             // Texture coordinates
out vec3 colour;

const vec3 lightPosition = vec3(3, 2, 1);

uniform mat4 mvp_matrix;    // model-view-projection matrix

varying vec3 world_position;

void main()
{
    const vec3 BLACK = vec3(0, 0, 0);
    const vec3 BRICK = vec3(0.51, 0.12, 0.15);
    const vec3 MORTAR = vec3(1, 1, 1);

    const float kA = 0.1;
    const float kD = 0.75;

    // Determine the correct colour of the surface
    const float brickHeight = 0.125;
    const float brickWidth = 0.25;
    const float edgeWidth = 0.05;
    vec3 surfaceColour;

    float yMod = mod(uv.y, brickHeight)
    float xMod = mod(uv.x, brickWidth);
    int row = (int)(uv.y / brickHeight);
    if (row % 2 == 1) // Odd row, offset
        xMod += brickWidth / 2;

    if (yMod <= edgeWidth || yMod >= brickHeight - edgeWidth ||
        xMod <= edgeWidth || xMod >= brickWidth - edgeWidth)
    {
        surfaceColour = MORTAR;
    }
    else
        surfaceColour = BRICK;

    // Apply shading using the correct colour
    colour = BLACK;

    colour += kA * surfaceColour;

    vec3 n = normalize(frag_normal);
    vec3 l = normalize(lightPosition);
    float nl = dot(n, l);
    if (nl > 0)
        colour += kD * nl * surfaceColour;
}