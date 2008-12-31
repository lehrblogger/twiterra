/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.render.DrawContext;

import javax.media.opengl.glu.GLU;
import javax.media.opengl.GL;

/**
 * @author dcollins
 * @version $Id: ViewSupport.java 4927 2008-04-04 21:27:45Z dcollins $
 */
public class ViewSupport
{
    private final GLU glu = new GLU();

    public ViewSupport()
    {
    }

    public void loadGLViewState(DrawContext dc, Matrix modelview, Matrix projection)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGL() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGLIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }
        if (modelview == null)
        {
            Logging.logger().fine("nullValue.ModelViewIsNull");
        }
        if (projection == null)
        {
            Logging.logger().fine("nullValue.ProjectionIsNull");
        }

        double[] matrixArray = new double[16];        

        GL gl = dc.getGL();
        // Store the current matrix-mode state.
        int[] matrixMode = new int[1];
        gl.glGetIntegerv(GL.GL_MATRIX_MODE, matrixMode, 0);

        // Apply the model-view matrix to the current OpenGL context.
        gl.glMatrixMode(GL.GL_MODELVIEW);
        if (modelview != null)
        {
            modelview.toArray(matrixArray, 0, false);
            gl.glLoadMatrixd(matrixArray, 0);
        }
        else
        {
            gl.glLoadIdentity();
        }

        // Apply the projection matrix to the current OpenGL context.
        gl.glMatrixMode(GL.GL_PROJECTION);
        if (projection != null)
        {
            projection.toArray(matrixArray, 0, false);
            gl.glLoadMatrixd(matrixArray, 0);
        }
        else
        {
            gl.glLoadIdentity();
        }

        // Restore matrix-mode state.
        gl.glMatrixMode(matrixMode[0]);
    }

    public Vec4 project(Vec4 point, Matrix modelview, Matrix projection, java.awt.Rectangle viewport)
    {
        if (point == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (modelview == null || projection == null)
        {
            String message = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewport == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // GLU expects matrices as column-major arrays.
        double[] modelviewArray = new double[16];
        double[] projectionArray = new double[16];
        modelview.toArray(modelviewArray, 0, false);
        projection.toArray(projectionArray, 0, false);
        // GLU expects the viewport as a four-component array.
        int[] viewportArray = new int[] {viewport.x, viewport.y, viewport.width, viewport.height};

        double[] result = new double[3];
        if (!this.glu.gluProject(
            point.x, point.y, point.z,
            modelviewArray, 0,
            projectionArray, 0,
            viewportArray, 0,
            result, 0))
        {
            return null;
        }

        return Vec4.fromArray3(result, 0);
    }

    public Vec4 unProject(Vec4 windowPoint, Matrix modelview, Matrix projection, java.awt.Rectangle viewport)
    {
        if (windowPoint == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (modelview == null || projection == null)
        {
            String message = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewport == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // GLU expects matrices as column-major arrays.
        double[] modelviewArray = new double[16];
        double[] projectionArray = new double[16];
        modelview.toArray(modelviewArray, 0, false);
        projection.toArray(projectionArray, 0, false);
        // GLU expects the viewport as a four-component array.
        int[] viewportArray = new int[] {viewport.x, viewport.y, viewport.width, viewport.height};

        double[] result = new double[3];
        if (!this.glu.gluUnProject(
            windowPoint.x, windowPoint.y, windowPoint.z,
            modelviewArray, 0,
            projectionArray, 0,
            viewportArray, 0,
            result, 0))
        {
            return null;
        }

        return Vec4.fromArray3(result, 0);
    }

    public Line computeRayFromScreenPoint(double x, double y,
                                          Matrix modelview, Matrix projection, java.awt.Rectangle viewport)
    {
        if (modelview == null || projection == null)
        {
            String message = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewport == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Compute a ray originating from the view, and passing through the screen point (x, y).
        // 
        // Taken from the "OpenGL Technical FAQ & Troubleshooting Guide",
        // section 20.010 "How can I know which primitive a user has selected with the mouse?"
        //
        // http://www.opengl.org/resources/faq/technical/selection.htm#sele0010

        Matrix modelViewInv = modelview.getInverse();
        if (modelViewInv == null)
            return null;

        Vec4 eye = Vec4.UNIT_W.transformBy4(modelViewInv);
        if (eye == null)
            return null;

        double yInv = viewport.height - y - 1;
        Vec4 a = this.unProject(new Vec4(x, yInv, 0, 0), modelview, projection, viewport);
        Vec4 b = this.unProject(new Vec4(x, yInv, 1, 0), modelview, projection, viewport);
        if (a == null || b == null)
            return null;

        return new Line(eye, b.subtract3(a).normalize3());
    }

    public double computePixelSizeAtDistance(double distance, Angle fieldOfView, java.awt.Rectangle viewport)
    {
        if (fieldOfView == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewport == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Compute the coefficient for computing the size of a pixel.
        double pixelSizeScale;
        if (viewport.getWidth() > 0)
            pixelSizeScale = 2 * fieldOfView.tanHalfAngle() / viewport.getWidth();
        else
            pixelSizeScale = 0;

        return Math.abs(distance) * pixelSizeScale;
    }

    public double computeHorizonDistance(Globe globe, double elevation)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (elevation <= 0)
            return 0;

        double radius = globe.getMaximumRadius();
        return Math.sqrt(elevation * (2 * radius + elevation));
    }

    public double computeElevationAboveSurface(DrawContext dc, Position position)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        Globe globe = dc.getGlobe();
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position surfacePosition = null;
        // Look for the surface geometry point at 'position'.
        Vec4 pointOnGlobe = dc.getPointOnGlobe(position.getLatitude(), position.getLongitude());
        if (pointOnGlobe != null)
            surfacePosition = globe.computePositionFromPoint(pointOnGlobe);
        // Fallback to using globe elevation values.
        if (surfacePosition == null)
            surfacePosition = new Position(
                    position.getLatLon(),
                    globe.getElevation(position.getLatitude(), position.getLongitude()) * dc.getVerticalExaggeration());

        return position.getElevation() - surfacePosition.getElevation();
    }
}
