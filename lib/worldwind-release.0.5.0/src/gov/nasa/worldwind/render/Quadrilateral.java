/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;
import java.awt.*;
import java.nio.DoubleBuffer;

/**
 * @author tag
 * @version $Id: Quadrilateral.java 2471 2007-07-31 21:50:57Z tgaskins $
 */
public class Quadrilateral implements Renderable, Movable
{
    private LatLon southwestCorner;
    private LatLon northeastCorner;
    private double elevation;
    private Vec4 referenceCenter;
    private DoubleBuffer vertices;
    private int antiAliasHint = GL.GL_FASTEST;
    private Color color = Color.WHITE;

    public Quadrilateral(LatLon southwestCorner, LatLon northeastCorner, double elevation)
    {
        if (southwestCorner == null || northeastCorner == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.southwestCorner = southwestCorner;
        this.northeastCorner = northeastCorner;
        this.elevation = elevation;
    }

    public Quadrilateral(Sector sector, double elevation)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.southwestCorner = new LatLon(sector.getMinLatitude(), sector.getMinLongitude());
        this.northeastCorner = new LatLon(sector.getMaxLatitude(), sector.getMaxLongitude());
        this.elevation = elevation;
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.color = color;
    }

    public int getAntiAliasHint()
    {
        return antiAliasHint;
    }

    public void setAntiAliasHint(int hint)
    {
        if (!(hint == GL.GL_DONT_CARE || hint == GL.GL_FASTEST || hint == GL.GL_NICEST))
        {
            String msg = Logging.getMessage("generic.InvalidHint");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.antiAliasHint = hint;
    }

    public void setCorners(LatLon southWest, LatLon northEast)
    {
        this.southwestCorner = southWest;
        this.northeastCorner = northEast;
        this.vertices = null;
    }

    public LatLon[] getCorners()
    {
        LatLon[] retVal = new LatLon[2];

        retVal[0] = this.southwestCorner;
        retVal[1] = this.northeastCorner;

        return retVal;
    }

    public double getElevation()
    {
        return elevation;
    }

    public void setElevation(double elevation)
    {
        this.elevation = elevation;
        this.vertices = null;
    }

    private void intializeGeometry(DrawContext dc)
    {
        DoubleBuffer verts = BufferUtil.newDoubleBuffer(12);

        Vec4[] p = new Vec4[4];

        p[0] = dc.getGlobe().computePointFromPosition(this.southwestCorner.getLatitude(),
            this.southwestCorner.getLongitude(), this.elevation);
        p[1] = dc.getGlobe().computePointFromPosition(this.southwestCorner.getLatitude(),
            this.northeastCorner.getLongitude(), this.elevation);
        p[2] = dc.getGlobe().computePointFromPosition(this.northeastCorner.getLatitude(),
            this.northeastCorner.getLongitude(), this.elevation);
        p[3] = dc.getGlobe().computePointFromPosition(this.northeastCorner.getLatitude(),
            this.southwestCorner.getLongitude(), this.elevation);

        Vec4 refcenter = new Vec4(
            (p[0].x + p[2].x) / 2.0,
            (p[0].y + p[2].y) / 2.0,
            (p[0].z + p[2].z) / 2.0);

        for (int i = 0; i < 4; i++)
        {
            verts.put(p[i].x - refcenter.x);
            verts.put(p[i].y - refcenter.y);
            verts.put(p[i].z - refcenter.z);
        }

        this.referenceCenter = refcenter;
        this.vertices = verts;
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (this.vertices == null)
        {
            this.intializeGeometry(dc);

            if (this.vertices == null)
                return; // TODO: logger a warning
        }

        GL gl = dc.getGL();

        int attrBits = GL.GL_HINT_BIT | GL.GL_CURRENT_BIT;
        if (!dc.isPickingMode())
        {
            if (this.color.getAlpha() != 255)
                attrBits |= GL.GL_COLOR_BUFFER_BIT;
        }

        gl.glPushAttrib(attrBits);
        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT);
        dc.getView().pushReferenceCenter(dc, this.referenceCenter);

        try
        {
            if (!dc.isPickingMode())
            {
                if (this.color.getAlpha() != 255)
                {
                    gl.glEnable(GL.GL_BLEND);
                    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                }
                dc.getGL().glColor4ub((byte) this.color.getRed(), (byte) this.color.getGreen(),
                    (byte) this.color.getBlue(), (byte) this.color.getAlpha());
            }

            gl.glHint(GL.GL_POLYGON_SMOOTH_HINT, this.antiAliasHint);
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL.GL_DOUBLE, 0, this.vertices.rewind());
            gl.glDrawArrays(GL.GL_QUADS, 0, 4);
        }
        finally
        {
            gl.glPopClientAttrib();
            gl.glPopAttrib();
            dc.getView().popReferenceCenter(dc);
        }
    }

    public Position getReferencePosition()
    {
        return new Position(this.southwestCorner, this.elevation);
    }

    public void move(Position delta)
    {
        if (delta == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.northeastCorner = this.northeastCorner.add(delta);
        this.southwestCorner = this.southwestCorner.add(delta);
        this.elevation = this.elevation + delta.getElevation();
        this.vertices = null;
    }

    public void moveTo(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Position delta = position.subtract(this.getReferencePosition());
        this.move(delta);
    }
}
