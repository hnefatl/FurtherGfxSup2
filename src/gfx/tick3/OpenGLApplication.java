package gfx.tick3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class OpenGLApplication {

    // Vertical field of view
    private static final float FOV_Y = (float) Math.toRadians(50);
    private static final float HEIGHTMAP_SCALE = 3.0f;

    // Width and height of renderer in pixels
    protected static int WIDTH = 800, HEIGHT = 600;

    // Size of height map in world units
    private static float MAP_SIZE = 10;
    private Camera camera;
    private long window;

    private ShaderProgram shaders;
    private float[][] heightmap;
    private int no_of_triangles;
    private int vertexArrayObj;

    // Callbacks for input handling
    private GLFWCursorPosCallback cursor_cb;
    private GLFWScrollCallback scroll_cb;
    private GLFWKeyCallback key_cb;

    // Filenames for vertex and fragment shader source code
    private final String VSHADER_FN = "resources/vertex_shader.glsl";
    private final String FSHADER_FN = "resources/fragment_shader.glsl";

    private long startTime = -1;

    // OpenGL setup - do not touch this method!
    public void initializeOpenGL() {

        if (glfwInit() != true)
            throw new RuntimeException("Unable to initialize the graphics runtime.");

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Ensure that the right version of OpenGL is used (at least 3.2)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // Use CORE OpenGL profile without depreciated functions
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE); // Make it forward compatible

        window = glfwCreateWindow(WIDTH, HEIGHT, "Tick 3", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the application window.");

        GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (mode.width() - WIDTH) / 2, (mode.height() - HEIGHT) / 2);
        glfwMakeContextCurrent(window);
        createCapabilities();

        // Enable v-sync
        glfwSwapInterval(1);

        // Cull back-faces of polygons
        glDisable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Do depth comparisons when rendering
        glEnable(GL_DEPTH_TEST);

        // Create camera, and setup input handlers
        camera = new Camera((double) WIDTH / HEIGHT, FOV_Y);
        initializeInputs();

        // Create shaders and attach to a ShaderProgram
        Shader vertShader = new Shader(GL_VERTEX_SHADER, VSHADER_FN);
        Shader fragShader = new Shader(GL_FRAGMENT_SHADER, FSHADER_FN);
        shaders = new ShaderProgram(vertShader, fragShader, "colour");

        // Initialize mesh data in CPU memory
        float vertPositions[] = initializeVertexPositions();
        int indices[] = initializeVertexIndices();
        float vertNormals[] = initializeVertexNormals();
        no_of_triangles = indices.length;

        // Load mesh data onto GPU memory
        loadDataOntoGPU( vertPositions, indices, vertNormals );
    }

    private void initializeInputs() {

        // Callback for: when dragging the mouse, rotate the camera
        cursor_cb = new GLFWCursorPosCallback() {
            private double prevMouseX, prevMouseY;

            public void invoke(long window, double mouseX, double mouseY) {
                boolean dragging = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
                if (dragging) {
                    camera.rotate(mouseX - prevMouseX, mouseY - prevMouseY);
                }
                prevMouseX = mouseX;
                prevMouseY = mouseY;
            }
        };

        // Callback for: when scrolling, zoom the camera
        scroll_cb = new GLFWScrollCallback() {
            public void invoke(long window, double dx, double dy) {
                camera.zoom(dy > 0);
            }
        };

        // Callback for keyboard controls: "W" - wireframe, "P" - points, "S" - take screenshot
        key_cb = new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_W && action == GLFW_PRESS) {
                    glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                    glDisable(GL_CULL_FACE);
                } else if (key == GLFW_KEY_P && action == GLFW_PRESS) {
                    glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
                } else if (key == GLFW_KEY_S && action == GLFW_RELEASE) {
                    takeScreenshot("screenshot.png");
                } else if (action == GLFW_RELEASE) {
                    glDisable(GL_CULL_FACE);
                    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                }
            }
        };

        // Set callbacks on the window
        glfwSetCursorPosCallback(window, cursor_cb);
        glfwSetScrollCallback(window, scroll_cb);
        glfwSetKeyCallback(window, key_cb);
    }

    // Vertex, Index, and Normal data from
    // http://www.songho.ca/opengl/gl_vertexarray.html#
    public float[] initializeVertexPositions()
	{
        return new float[]
        {
            1, 1, 1,  -1, 1, 1,  -1,-1, 1,   1,-1, 1,   // v0,v1,v2,v3 (front)
            1, 1, 1,   1,-1, 1,   1,-1,-1,   1, 1,-1,   // v0,v3,v4,v5 (right)
            1, 1, 1,   1, 1,-1,  -1, 1,-1,  -1, 1, 1,   // v0,v5,v6,v1 (top)
           -1, 1, 1,  -1, 1,-1,  -1,-1,-1,  -1,-1, 1,   // v1,v6,v7,v2 (left)
           -1,-1,-1,   1,-1,-1,   1,-1, 1,  -1,-1, 1,   // v7,v4,v3,v2 (bottom)
            1,-1,-1,  -1,-1,-1,  -1, 1,-1,   1, 1,-1    // v4,v7,v6,v5 (back)
        };
    }
    public int[] initializeVertexIndices()
	{
        return new int[] {
            0, 1, 2,   2, 3, 0,      // front
            4, 5, 6,   6, 7, 4,      // right
            8, 9,10,  10,11, 8,      // top
           12,13,14,  14,15,12,      // left
           16,17,18,  18,19,16,      // bottom
           20,21,22,  22,23,20       // back
        };
    }
    public float[] initializeVertexNormals()
	{
        return new float[]
        {
            0, 0, 1,   0, 0, 1,   0, 0, 1,   0, 0, 1,   // v0,v1,v2,v3 (front)
            1, 0, 0,   1, 0, 0,   1, 0, 0,   1, 0, 0,   // v0,v3,v4,v5 (right)
            0, 1, 0,   0, 1, 0,   0, 1, 0,   0, 1, 0,   // v0,v5,v6,v1 (top)
           -1, 0, 0,  -1, 0, 0,  -1, 0, 0,  -1, 0, 0,   // v1,v6,v7,v2 (left)
            0,-1, 0,   0,-1, 0,   0,-1, 0,   0,-1, 0,   // v7,v4,v3,v2 (bottom)
            0, 0,-1,   0, 0,-1,   0, 0,-1,   0, 0,-1    // v4,v7,v6,v5 (back)
        };
    }

    public void loadDataOntoGPU( float[] vertPositions, int[] indices, float[] vertNormals ) {

        int shaders_handle = shaders.getHandle();

        vertexArrayObj = glGenVertexArrays(); // Get a OGL "name" for a vertex-array object
        glBindVertexArray(vertexArrayObj); // Create a new vertex-array object with that name

        // ---------------------------------------------------------------
        // LOAD VERTEX POSITIONS
        // ---------------------------------------------------------------

        // Construct the vertex buffer in CPU memory
        FloatBuffer vertex_buffer = BufferUtils.createFloatBuffer(vertPositions.length);
        vertex_buffer.put(vertPositions); // Put the vertex array into the CPU buffer
        vertex_buffer.flip(); // "flip" is used to change the buffer from read to write mode

        int vertex_handle = glGenBuffers(); // Get an OGL name for a buffer object
        glBindBuffer(GL_ARRAY_BUFFER, vertex_handle); // Bring that buffer object into existence on GPU
        glBufferData(GL_ARRAY_BUFFER, vertex_buffer, GL_STATIC_DRAW); // Load the GPU buffer object with data

        // Get the locations of the "position" vertex attribute variable in our ShaderProgram
        int position_loc = glGetAttribLocation(shaders_handle, "position");
        if (position_loc != -1)
		{

            // Specifies where the data for "position" variable can be accessed
            glVertexAttribPointer(position_loc, 3, GL_FLOAT, false, 0, 0);

            // Enable that vertex attribute variable
            glEnableVertexAttribArray(position_loc);
		}
		
        // ---------------------------------------------------------------
        // LOAD VERTEX NORMALS
        // ---------------------------------------------------------------

		FloatBuffer normal_buffer = BufferUtils.createFloatBuffer(vertNormals.length);
		normal_buffer.put(vertNormals).flip();
		
		int normal_handle = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, normal_handle);
		glBufferData(GL_ARRAY_BUFFER, normal_buffer, GL_STATIC_DRAW);
		
		int normal_loc = glGetAttribLocation(shaders_handle, "normal");
		
		if (normal_loc != -1)
		{
			glVertexAttribPointer(normal_loc, 3, GL_FLOAT, false, 0, 0);
			glEnableVertexAttribArray(normal_loc);
		}

        // ---------------------------------------------------------------
        // LOAD VERTEX INDICES
        // ---------------------------------------------------------------

        IntBuffer index_buffer = BufferUtils.createIntBuffer(indices.length);
        index_buffer.put(indices).flip();
        int index_handle = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, index_handle);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, index_buffer, GL_STATIC_DRAW);

        // Finally, check for OpenGL errors
        checkError();
    }


    public void run() {

        initializeOpenGL();

        while (glfwWindowShouldClose(window) != true) {
            render();
        }
    }

    public void render() {

        // Step 1: Pass a new model-view-projection matrix to the vertex shader

        Matrix4f mvp_matrix; // Model-view-projection matrix
        mvp_matrix = new Matrix4f(camera.getProjectionMatrix()).mul(camera.getViewMatrix());

        int mvp_location = glGetUniformLocation(shaders.getHandle(), "mvp_matrix");
        if (mvp_location != -1)
        {
            FloatBuffer mvp_buffer = BufferUtils.createFloatBuffer(16);
            mvp_matrix.get(mvp_buffer);
            glUniformMatrix4fv(mvp_location, false, mvp_buffer);
        }
				
        int camera_loc = glGetUniformLocation(shaders.getHandle(), "camera");
        if (camera_loc != -1)
            glUniform3f(camera_loc, camera.getCameraPosition().x, camera.getCameraPosition().y, camera.getCameraPosition().z);

        // Add time attribute
        int time_location = glGetUniformLocation(shaders.getHandle(), "time");
        if (time_location != -1)
        {
            if (startTime == -1)
                startTime = System.currentTimeMillis();

            float seconds = ((float)(System.currentTimeMillis() - startTime)) / 1000f;
            glUniform1f(time_location, seconds);
        }
		
        // Step 2: Clear the buffer

        glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // Set the background colour to dark gray
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Step 3: Draw our VertexArray as triangles

        glBindVertexArray(vertexArrayObj); // Bind the existing VertexArray object
        glDrawElements(GL_TRIANGLES, no_of_triangles, GL_UNSIGNED_INT, 0); // Draw it as triangles
        glBindVertexArray(0);              // Remove the binding

        // Step 4: Swap the draw and back buffers to display the rendered image

        glfwSwapBuffers(window);
        glfwPollEvents();
        checkError();
    }

    public void takeScreenshot(String output_path) {
        int bpp = 4;

        glReadBuffer(GL_FRONT);
        ByteBuffer buffer = BufferUtils.createByteBuffer(WIDTH * HEIGHT * bpp);
        glReadPixels(0, 0, WIDTH, HEIGHT, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        checkError();

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < WIDTH; ++i) {
            for (int j = 0; j < HEIGHT; ++j) {
                int index = (i + WIDTH * (HEIGHT - j - 1)) * bpp;
                int r = buffer.get(index + 0) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int b = buffer.get(index + 2) & 0xFF;
                image.setRGB(i, j, 0xFF << 24 | r << 16 | g << 8 | b);
            }
        }
        try {
            ImageIO.write(image, "png", new File(output_path));
        } catch (IOException e) {
            throw new RuntimeException("failed to write output file - ask for a demonstrator");
        }
    }

    public void stop() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void checkError() {
        int error = glGetError();
        if (error != GL_NO_ERROR)
            throw new RuntimeException("OpenGL produced an error (code " + error + ") - ask for a demonstrator");
    }
}
