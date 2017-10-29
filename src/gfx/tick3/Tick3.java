package gfx.tick3;

public class Tick3
{
    public static void main(String[] args)
    {
        OpenGLApplication app = null;

        try
        {
            app = new OpenGLApplication();
            app.run();
        }
        finally
        {
            if (app != null)
                app.stop();
        }
    }
}
