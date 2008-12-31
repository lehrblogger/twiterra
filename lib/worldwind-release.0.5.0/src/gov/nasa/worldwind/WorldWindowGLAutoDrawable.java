/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.beans.*;
import java.util.logging.Level;

/**
 * A non-platform specific {@link WorldWindow} class. This class can be aggregated into platform-specific classes to
 * provide the core functionality of World Wind.
 *
 * @author Tom Gaskins
 * @version $Id: WorldWindowGLAutoDrawable.java 5121 2008-04-22 17:54:54Z tgaskins $
 */
public class WorldWindowGLAutoDrawable extends WorldWindowImpl implements WorldWindowGLDrawable, GLEventListener
{
    private GLAutoDrawable drawable;
    private boolean shuttingDown = false;

    /**
     * Construct a new <code>WorldWindowGLCanvase</code> for a specified {@link GLDrawable}.
     */
    public WorldWindowGLAutoDrawable()
    {
        SceneController sc = this.getSceneController();
        if (sc != null)
        {
            sc.addPropertyChangeListener(this);
        }
    }

    public void initDrawable(GLAutoDrawable glAutoDrawable)
    {
        if (glAutoDrawable == null)
        {
            String msg = Logging.getMessage("nullValue.DrawableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.drawable = glAutoDrawable;
        this.drawable.setAutoSwapBufferMode(false);
        this.drawable.addGLEventListener(this);
    }

    public void initTextureCache(TextureCache textureCache)
    {
        if (textureCache == null)
        {
            String msg = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.setTextureCache(textureCache);
    }

    @Override
    /**
     * Causes resources used by this World Window to be freed. The resources are not freed immediately. Instead, in
     * order to have the OpenGL context current during clean up, a redraw of the World Window is requested and the
     * resources are freed when the window system requests the repaint. No actual redraw is performed during that call.
     */
    public void shutdown()
    {
        this.shuttingDown = true;
        this.drawable.repaint(); // Queue a JOGL repaint request. Cleanup is done in display() while GL context current.
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent)
    {
        if (propertyChangeEvent == null)
        {
            String msg = Logging.getMessage("nullValue.PropertyChangeEventIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.drawable != null)
            this.drawable.repaint(); // Queue a JOGL repaint request.
    }

    /**
     * See {@link GLEventListener#init(GLAutoDrawable)}.
     *
     * @param glAutoDrawable the drawable
     */
    public void init(GLAutoDrawable glAutoDrawable)
    {
        if (!glAutoDrawable.getGL().isFunctionAvailable("glActiveTexture"))
        {
            this.firePropertyChange("gov.nasa.worldwind.UnsupportedMethod", "glActiveTexture", null);
        }

        // Clear the texture cache if the window is reinitializing, most likely with a new gl hardware context.
        if (this.getTextureCache() != null)
            this.getTextureCache().clear();

        this.getSceneController().reinitialize();

        if (this.getModel() != null && this.getModel().getLayers() != null)
        {
            for (Layer layer : this.getModel().getLayers())
            {
                layer.dispose();
            }
        }

//        this.drawable.setGL(new DebugGL(this.drawable.getGL()));
    }

    /**
     * See {@link GLEventListener#display(GLAutoDrawable)}.
     *
     * @param glAutoDrawable the drawable
     * @throws IllegalStateException if no {@link SceneController} exists for this canvas
     */
    public void display(GLAutoDrawable glAutoDrawable)
    {
        if (this.shuttingDown)
        {
            try
            {
                super.shutdown();
            }
            catch (Exception e)
            {
                Logging.logger().log(Level.SEVERE, Logging.getMessage(
                    "WorldWindowGLCanvas.ExceptionWhileShuttingDownWorldWindow"), e);
            }
            this.shuttingDown = false;
            return;
        }

        try
        {
            SceneController sc = this.getSceneController();
            if (sc == null)
            {
                Logging.logger().severe("WorldWindowGLCanvas.ScnCntrllerNullOnRepaint");
                throw new IllegalStateException(Logging.getMessage("WorldWindowGLCanvas.ScnCntrllerNullOnRepaint"));
            }

            Position positionAtStart = this.getCurrentPosition();
            PickedObject selectionAtStart = this.getCurrentSelection();

            try
            {
                this.callRenderingListeners(new RenderingEvent(this.drawable, RenderingEvent.BEFORE_RENDERING));
            }
            catch (Exception e)
            {
                Logging.logger().log(Level.SEVERE,
                    Logging.getMessage("WorldWindowGLAutoDrawable.ExceptionDuringGLEventListenerDisplay"), e);
            }

            this.doDisplay();

            try
            {
                this.callRenderingListeners(new RenderingEvent(this.drawable, RenderingEvent.BEFORE_BUFFER_SWAP));
            }
            catch (Exception e)
            {
                Logging.logger().log(Level.SEVERE,
                    Logging.getMessage("WorldWindowGLAutoDrawable.ExceptionDuringGLEventListenerDisplay"), e);
            }

            this.doSwapBuffers(this.drawable);

            Double frameTime = sc.getFrameTime();
            if (frameTime != null)
                this.setValue(PerformanceStatistic.FRAME_TIME, frameTime);

            Double frameRate = sc.getFramesPerSecond();
            if (frameTime != null)
                this.setValue(PerformanceStatistic.FRAME_RATE, frameRate);

            this.callRenderingListeners(new RenderingEvent(this.drawable, RenderingEvent.AFTER_BUFFER_SWAP));

            // Position and selection notification occurs only on triggering conditions, not same-state conditions:
            // start == null, end == null: nothing selected -- don't notify
            // start == null, end != null: something now selected -- notify
            // start != null, end == null: something was selected but no longer is -- notify
            // start != null, end != null, start != end: something new was selected -- notify
            // start != null, end != null, start == end: same thing is selected -- don't notify

            Position positionAtEnd = this.getCurrentPosition();
            if (positionAtStart != null || positionAtEnd != null)
            {
                if (positionAtStart != positionAtEnd)
                    this.callPositionListeners(new PositionEvent(this.drawable, sc.getPickPoint(),
                        positionAtStart, positionAtEnd));
            }

            PickedObject selectionAtEnd = this.getCurrentSelection();
            if (selectionAtStart != null || selectionAtEnd != null)
            {
                if (selectionAtStart != selectionAtEnd)
                    this.callSelectListeners(new SelectEvent(this.drawable, SelectEvent.ROLLOVER,
                        sc.getPickPoint(), sc.getPickedObjectList()));
            }
        }
        catch (Exception e)
        {
            Logging.logger().log(Level.SEVERE, Logging.getMessage(
                "WorldWindowGLCanvas.ExceptionAttemptingRepaintWorldWindow"), e);
        }
    }

    protected void doDisplay()
    {
        this.getSceneController().repaint();
    }

    protected void doSwapBuffers(GLAutoDrawable drawable)
    {
        drawable.swapBuffers();
    }

    /**
     * See {@link GLEventListener#reshape(GLAutoDrawable,int,int,int,int)}.
     *
     * @param glAutoDrawable the drawable
     */
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h)
    {
    }

    /**
     * See {@link GLEventListener#displayChanged(GLAutoDrawable,boolean,boolean)}.
     *
     * @param glAutoDrawable the drawable
     */
    public void displayChanged(GLAutoDrawable glAutoDrawable, boolean b, boolean b1)
    {
        Logging.logger().finest("WorldWindowGLCanvas.DisplayEventListenersDisplayChangedMethodCalled");
    }

    public void redrawNow()
    {
        if (this.drawable != null)
            this.drawable.display();
    }
}
