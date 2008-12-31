/*
Copyright (C) 2001, 2006, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;

import javax.media.opengl.GL;
import java.awt.*;

/**
 * Represent a text label attached to a Point on the viewport and its rendering attributes.
 * @author Patrick Murris
 * @version $Id: ScreenAnnotation.java 5178 2008-04-25 21:51:20Z patrickmurris $
 * @see AbstractAnnotation
 * @see AnnotationAttributes
 */
public class ScreenAnnotation extends AbstractAnnotation
{
    private Point screenPoint;

    /**
     * Creates a <code>ScreenAnnotation</code> with the given text, at the given viewport position.
     * @param text the annotation text.
     * @param position the annotation viewport position.
     */
    public ScreenAnnotation(String text, Point position)
    {
        this.init(text, position, null, null);
    }

    /**
     * Creates a <code>ScreenAnnotation</code> with the given text, at the given viewport position.
     * Specifiy the <code>Font</code> to be used.
     * @param text the annotation text.
     * @param position the annotation viewport position.
     * @param font the <code>Font</code> to use.
     */
    public ScreenAnnotation(String text, Point position, Font font)
    {
        this.init(text, position, font, null);
    }

    /**
     * Creates a <code>ScreenAnnotation</code> with the given text, at the given viewport position.
     * Specifiy the <code>Font</code> and text <code>Color</code> to be used.
     * @param text the annotation text.
     * @param position the annotation viewport position.
     * @param font the <code>Font</code> to use.
     * @param textColor the text <code>Color</code>.
     */
    public ScreenAnnotation(String text, Point position, Font font, Color textColor)
    {
        this.init(text, position, font, textColor);
    }

    /**
     * Creates a <code>ScreenAnnotation</code> with the given text, at the given viewport position.
     * Specify the default {@link AnnotationAttributes} set.
     * @param text the annotation text.
     * @param position the annotation viewport position.
     * @param defaults the default {@link AnnotationAttributes} set.
     */
    public ScreenAnnotation(String text, Point position, AnnotationAttributes defaults)
    {
        if (text == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (defaults == null)
        {
            String message = Logging.getMessage("nullValue.AnnotationAttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.setText(text);
        this.screenPoint = position;
        this.getAttributes().setDefaults(defaults);
        this.getAttributes().setLeader(FrameFactory.LEADER_NONE);
        this.getAttributes().setDrawOffset(new Point(0, 0));
    }

    private void init(String text, Point position, Font font, Color textColor)
    {
        if (text == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.setText(text);
        this.screenPoint = position;
        this.getAttributes().setFont(font);
        this.getAttributes().setTextColor(textColor);
        this.getAttributes().setLeader(FrameFactory.LEADER_NONE);
        this.getAttributes().setDrawOffset(new Point(0, 0));
    }


    //-- Properties ---------------------------------------------------------------

    /**
     * Get the <code>Point</code> where the annotation is drawn in the viewport.
     * @return the <code>Point</code> where the annotation is drawn in the viewport.
     */
    public Point getScreenPoint()
    {
        return this.screenPoint;
    }

    /**
     * Set the <code>Point</code> where the annotation will be drawn in the viewport.
     * @param position the <code>Point</code> where the annotation will be drawn in the viewport.
     */
    public void setScreenPoint(Point position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.screenPoint = position;
    }

    //-- Rendering ----------------------------------------------------------------

    protected void doDraw(DrawContext dc)
    {
        if (dc.isPickingMode() && this.getPickSupport() == null)
            return;

        // Prepare to draw
        GL gl = dc.getGL();
        gl.glDepthFunc(GL.GL_ALWAYS);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        // Translate to screenpoint
        gl.glTranslated(screenPoint.x, screenPoint.y, 0d);

        // Draw
        drawAnnotation(dc, screenPoint, 1, 1, null);
    }

    /**
     * Returns an XML state document String describing the public attributes of this ScreenAnnotation.
     *
     * @return XML state document string describing this ScreenAnnotation.
     */
    public String getRestorableState()
    {
        RestorableSupport restorableSupport = null;

        // Try to parse the superclass' xml state document, if it defined one.
        String superStateInXml = super.getRestorableState();
        if (superStateInXml != null)
        {
            try
            {
                restorableSupport = RestorableSupport.parse(superStateInXml);
            }
            catch (Exception e)
            {
                // Parsing the document specified by the superclass failed.
                String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", superStateInXml);
                Logging.logger().severe(message);
            }
        }

        // Create our own state document from scratch.
        if (restorableSupport == null)
            restorableSupport = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (restorableSupport == null)
            return null;

        if (this.screenPoint != null)
        {
            RestorableSupport.StateObject screenPointStateObj = restorableSupport.addStateObject("screenPoint");
            if (screenPointStateObj != null)
            {
                restorableSupport.addStateValueAsDouble(screenPointStateObj, "x", this.screenPoint.getX());
                restorableSupport.addStateValueAsDouble(screenPointStateObj, "y", this.screenPoint.getY());
            }
        }

        return restorableSupport.getStateAsXml();
    }

    /**
     * Restores publicly settable attribute values found in the specified XML state document String. The
     * document specified by <code>stateInXml</code> must be a well formed XML document String, or this will throw an
     * IllegalArgumentException. Unknown structures in <code>stateInXml</code> are benign, because they will
     * simply be ignored.
     *
     * @param stateInXml an XML document String describing a ScreenAnnotation.
     * @throws IllegalArgumentException If <code>stateInXml</code> is null, or if <code>stateInXml</code> is not
     *                                  a well formed XML document String.
     */
    public void restoreState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Allow the superclass to restore it's state.
        try
        {
            super.restoreState(stateInXml);
        }
        catch (Exception e)
        {
            // Superclass will log the exception.
        }

        RestorableSupport restorableSupport;
        try
        {
            restorableSupport = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        // Restore the screenPoint property only if all parts are available.
        // We will not restore a partial screenPoint (for example, just the x value).
        RestorableSupport.StateObject screenPointStateObj = restorableSupport.getStateObject("screenPoint");
        if (screenPointStateObj != null)
        {
            Double xState = restorableSupport.getStateValueAsDouble(screenPointStateObj, "x");
            Double yState = restorableSupport.getStateValueAsDouble(screenPointStateObj, "y");
            if (xState != null && yState != null)
                setScreenPoint(new Point(xState.intValue(), yState.intValue()));
        }
    }
}
