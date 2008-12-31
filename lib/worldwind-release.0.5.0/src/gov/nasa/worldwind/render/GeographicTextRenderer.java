/* 
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.SectorGeometryList;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * @author dcollins
 * @version $Id: GeographicTextRenderer.java 5028 2008-04-11 19:50:38Z tgaskins $
 */
public class GeographicTextRenderer implements Disposable
{
    private Map<Font, TextRenderer> textRenderers = new HashMap<Font, TextRenderer>();
    private TextRenderer lastTextRenderer = null;
    private final GLU glu = new GLU();

    private static final Font DEFAULT_FONT = Font.decode("Arial-PLAIN-12");
    private static final Color DEFAULT_COLOR = Color.white;

    public GeographicTextRenderer()
    {
    }

    public void dispose()
    {
        for (TextRenderer textRenderer : textRenderers.values())
        {
            if (textRenderer != null)
                textRenderer.dispose();
        }

        this.textRenderers.clear();
    }

    public void render(DrawContext dc, Iterable<GeographicText> text)
    {
        this.drawMany(dc, text);
    }

    public void render(DrawContext dc, GeographicText text, Vec4 textPoint)
    {
        if (!isTextValid(text, false))
            return;

        this.drawOne(dc, text, textPoint);
    }

    private void drawMany(DrawContext dc, Iterable<GeographicText> textIterable)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().fine(msg);
            throw new IllegalArgumentException(msg);
        }
        if (textIterable == null)
        {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().fine(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getVisibleSector() == null)
            return;

        SectorGeometryList geos = dc.getSurfaceGeometry();
        if (geos == null)
            return;

        Iterator<GeographicText> iterator = textIterable.iterator();
        if (!iterator.hasNext())
            return;

        Frustum frustumInModelCoords = dc.getView().getFrustumInModelCoordinates();
        double horizon = dc.getView().computeHorizonDistance();

        while (iterator.hasNext())
        {
            GeographicText text = iterator.next();
            if (!isTextValid(text, true))
                continue;

            if (!text.isVisible())
                continue;

            Angle lat = text.getPosition().getLatitude();
            Angle lon = text.getPosition().getLongitude();

            if (!dc.getVisibleSector().contains(lat, lon))
                continue;

            Vec4 textPoint = geos.getSurfacePoint(lat, lon, text.getPosition().getElevation());
            if (textPoint == null)
                continue;

            double eyeDistance = dc.getView().getEyePoint().distanceTo3(textPoint);
            if (eyeDistance > horizon)
                continue;

            if (!frustumInModelCoords.contains(textPoint))
                continue;

            dc.addOrderedRenderable(new OrderedText(text, textPoint, eyeDistance));
        }
    }

    private void drawOne(DrawContext dc, GeographicText text, Vec4 textPoint)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().fine(msg);
            throw new IllegalArgumentException(msg);
        }
        if (dc.getView() == null)
        {
            String msg = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().fine(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getVisibleSector() == null)
            return;

        SectorGeometryList geos = dc.getSurfaceGeometry();
        if (geos == null)
            return;

        if (!text.isVisible())
            return;

        if (textPoint == null)
        {
            if (text.getPosition() == null)
                return;

            Angle lat = text.getPosition().getLatitude();
            Angle lon = text.getPosition().getLongitude();

            if (!dc.getVisibleSector().contains(lat, lon))
                return;

            textPoint = geos.getSurfacePoint(lat, lon, text.getPosition().getElevation());
            if (textPoint == null)
                return;
        }

        double horizon = dc.getView().computeHorizonDistance();
        double eyeDistance = dc.getView().getEyePoint().distanceTo3(textPoint);
        if (eyeDistance > horizon)
            return;

        if (!dc.getView().getFrustumInModelCoordinates().contains(textPoint))
            return;

        dc.addOrderedRenderable(new OrderedText(text, textPoint, eyeDistance));
    }

    private static boolean isTextValid(GeographicText text, boolean checkPosition)
    {
        if (text == null || text.getText() == null)
            return false;

        //noinspection RedundantIfStatement
        if (checkPosition && text.getPosition() == null)
            return false;

        return true;
    }

    private class OrderedText implements OrderedRenderable
    {
        GeographicText text;
        Vec4 point;
        double eyeDistance;

        OrderedText(GeographicText text, Vec4 point, double eyeDistance)
        {
            this.text = text;
            this.point = point;
            this.eyeDistance = eyeDistance;
        }

        public double getDistanceFromEye()
        {
            return this.eyeDistance;
        }

        public void render(DrawContext dc)
        {
            GeographicTextRenderer.this.beginRendering(dc);

            try
            {
                GeographicTextRenderer.this.drawText(dc, this);
                // Draw as many as we can in a batch to save ogl state switching.
                while (dc.getOrderedRenderables().peek() instanceof OrderedText)
                {
                    OrderedText ot = (OrderedText) dc.getOrderedRenderables().poll();
                    GeographicTextRenderer.this.drawText(dc, ot);
                }
            }
            catch (WWRuntimeException e)
            {
                Logging.logger().log(java.util.logging.Level.SEVERE, "generic.ExceptionWhileRenderingText", e);
            }
            catch (Exception e)
            {
                Logging.logger().log(java.util.logging.Level.SEVERE, "generic.ExceptionWhileRenderingText", e);
            }
            finally
            {
                GeographicTextRenderer.this.endRendering(dc);
            }
        }

        public void pick(DrawContext dc, java.awt.Point pickPoint)
        {
        }
    }

    private final int[] viewportArray = new int[4];

    private void beginRendering(DrawContext dc)
    {
        GL gl = dc.getGL();
        int attribBits =
            GL.GL_ENABLE_BIT // for enable/disable changes
                | GL.GL_COLOR_BUFFER_BIT // for alpha test func and ref, and blend
                | GL.GL_CURRENT_BIT      // for current color
                | GL.GL_DEPTH_BUFFER_BIT // for depth test, depth func, and depth mask
                | GL.GL_TRANSFORM_BIT    // for modelview and perspective
                | GL.GL_VIEWPORT_BIT;    // for depth range
        gl.glPushAttrib(attribBits);

        gl.glGetIntegerv(GL.GL_VIEWPORT, viewportArray, 0);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, viewportArray[2], 0, viewportArray[3]);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        // Enable the depth test but don't write to the depth buffer.
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthMask(false);

        // Suppress polygon culling.
        gl.glDisable(GL.GL_CULL_FACE);

        // Suppress any fully transparent image pixels
        gl.glEnable(GL.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL.GL_GREATER, 0.001f);
    }

    private void endRendering(DrawContext dc)
    {
        if (this.lastTextRenderer != null)
        {
            this.lastTextRenderer.end3DRendering();
            this.lastTextRenderer = null;
        }

        GL gl = dc.getGL();

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();

        gl.glPopAttrib();
    }

    private Vec4 drawText(DrawContext dc, OrderedText uText)
    {
        if (uText.point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().fine(msg);
            return null;
        }

        GeographicText geographicText = uText.text;

        final CharSequence charSequence = geographicText.getText();
        if (charSequence == null)
            return null;

        final Vec4 screenPoint = dc.getView().project(uText.point);
        if (screenPoint == null)
            return null;

        Font font = geographicText.getFont();
        if (font == null)
            font = DEFAULT_FONT;

        TextRenderer textRenderer = this.textRenderers.get(font);
        if (textRenderer == null)
            textRenderer = this.initializeTextRenderer(font);
        if (textRenderer != this.lastTextRenderer)
        {
            if (this.lastTextRenderer != null)
                this.lastTextRenderer.end3DRendering();
            textRenderer.begin3DRendering();
            this.lastTextRenderer = textRenderer;
        }

        this.setDepthFunc(dc, uText, screenPoint);

        Rectangle2D textBound = textRenderer.getBounds(charSequence);
        float x = (float) (screenPoint.x - textBound.getWidth() / 2d);
        float y = (float) (screenPoint.y);

        Color color = geographicText.getColor();
        if (color == null)
            color = DEFAULT_COLOR;

        Color background = geographicText.getBackgroundColor();
        if (background != null)
        {
            textRenderer.setColor(background);
            textRenderer.draw3D(charSequence, x + 1, y - 1, 0, 1);
        }

        textRenderer.setColor(color);
        textRenderer.draw3D(charSequence, x, y, 0, 1);
        textRenderer.flush();

        return screenPoint;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void setDepthFunc(DrawContext dc, OrderedText uText, Vec4 screenPoint)
    {
        GL gl = dc.getGL();

        //if (uText.text.isAlwaysOnTop())
        //{
        //    gl.glDepthFunc(GL.GL_ALWAYS);
        //    return;
        //}

        Position eyePos = dc.getView().getEyePosition();
        if (eyePos == null)
        {
            gl.glDepthFunc(GL.GL_ALWAYS);
            return;
        }

        double altitude = eyePos.getElevation();
        if (altitude < (dc.getGlobe().getMaxElevation() * dc.getVerticalExaggeration()))
        {
            double depth = screenPoint.z - (8d * 0.00048875809d);
            depth = depth < 0d ? 0d : (depth > 1d ? 1d : depth);
            gl.glDepthFunc(GL.GL_LESS);
            gl.glDepthRange(depth, depth);
        }
        //else if (screenPoint.z >= 1d)
        //{
        //    gl.glDepthFunc(GL.GL_EQUAL);
        //    gl.glDepthRange(1d, 1d);
        //}
        else
        {
            gl.glDepthFunc(GL.GL_ALWAYS);
        }
    }

    private TextRenderer initializeTextRenderer(Font font)
    {
        TextRenderer textRenderer = new TextRenderer(font, true, true);
        textRenderer.setUseVertexArrays(false);
        TextRenderer oldTextRenderer;
        oldTextRenderer = this.textRenderers.put(font, textRenderer);
        if (oldTextRenderer != null)
            oldTextRenderer.dispose();
        return textRenderer;
    }
}
