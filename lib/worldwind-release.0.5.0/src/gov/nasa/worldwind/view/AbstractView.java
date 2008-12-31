/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.ViewStateIterator;
import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;

/**
 * @author dcollins
 * @version $Id: AbstractView.java 4994 2008-04-09 18:15:52Z dcollins $
 */
public abstract class AbstractView extends WWObjectImpl implements View
{
    private boolean detectCollisions = true;
    private boolean hadCollisions;
    private ViewStateIterator viewStateIterator;

    public boolean isDetectCollisions()
    {
        return this.detectCollisions;
    }

    public void setDetectCollisions(boolean detectCollisions)
    {
        this.detectCollisions = detectCollisions;
    }

    public boolean hadCollisions()
    {
        boolean result = this.hadCollisions;
        this.hadCollisions = false;
        return result;
    }

    // TODO
    // Separate control should be provided over whether the View will detect collisions
    // which reports through hadCollisions() and flagHadCollisions(),
    // and whether the View will resolve collisions itself,
    // something along the lines of isResolveCollisions.
    // At the same time, flagHadCollisions() should be made part of the public View interface.
    protected void flagHadCollisions()
    {
        this.hadCollisions = true;     
    }

    public void stopMovement()
    {
        forceStopStateIterators();
        firePropertyChange(VIEW_STOPPED, null, this);
    }

    public void apply(DrawContext dc)
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
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoGlobeSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        updateStateIterator();
        doApply(dc);
    }

    protected abstract void doApply(DrawContext dc);

    public Matrix pushReferenceCenter(DrawContext dc, Vec4 referenceCenter)
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
        if (referenceCenter == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Matrix modelview = getModelviewMatrix();

        // Compute a new model-view matrix with origin at referenceCenter.
        Matrix matrix = null;
        if (modelview != null)
            matrix = modelview.multiply(Matrix.fromTranslation(referenceCenter));

        GL gl = dc.getGL();
        // Store the current matrix-mode state.
        int[] matrixMode = new int[1];
        gl.glGetIntegerv(GL.GL_MATRIX_MODE, matrixMode, 0);

        if (matrixMode[0] != GL.GL_MODELVIEW)
            gl.glMatrixMode(GL.GL_MODELVIEW);

        // Push and load a new model-view matrix to the current OpenGL context held by 'dc'.
        gl.glPushMatrix();
        if (matrix != null)
        {
            double[] matrixArray = new double[16];
            matrix.toArray(matrixArray, 0, false);
            gl.glLoadMatrixd(matrixArray, 0);
        }

        // Restore matrix-mode state.
        if (matrixMode[0] != GL.GL_MODELVIEW)
            gl.glMatrixMode(matrixMode[0]);

        return matrix;
    }

    public void popReferenceCenter(DrawContext dc)
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

        GL gl = dc.getGL();
        // Store the current matrix-mode state.
        int[] matrixMode = new int[1];
        gl.glGetIntegerv(GL.GL_MATRIX_MODE, matrixMode, 0);

        // Pop a model-view matrix off the current OpenGL context held by 'dc'.
        if (matrixMode[0] != GL.GL_MODELVIEW)
            gl.glMatrixMode(GL.GL_MODELVIEW);

        // Pop the top model-view matrix.
        gl.glPopMatrix();

        // Restore matrix-mode state.
        if (matrixMode[0] != GL.GL_MODELVIEW)
            gl.glMatrixMode(matrixMode[0]);
    }

    public void applyStateIterator(ViewStateIterator viewStateIterator)
    {
        ViewStateIterator oldIterator = this.viewStateIterator;
        this.viewStateIterator = viewStateIterator;

        if (this.viewStateIterator != null)
        {
            this.viewStateIterator = this.viewStateIterator.coalesceWith(this, oldIterator);
            firePropertyChange(AVKey.VIEW, null, this);
        }
    }

    public boolean hasStateIterator()
    {
        return this.viewStateIterator != null;
    }

    public void stopStateIterators()
    {
        forceStopStateIterators();
    }

    private void updateStateIterator()
    {
        if (this.viewStateIterator != null)
        {
            if (this.viewStateIterator.hasNextState(this))
            {
                this.viewStateIterator.nextState(this);
                firePropertyChange(AVKey.VIEW, null, this);
            }
            else
            {
                forceStopStateIterators();
                firePropertyChange(AVKey.VIEW_QUIET, null, this);
            }
        }
    }

    private void forceStopStateIterators()
    {
        this.viewStateIterator = null;
    }
}
