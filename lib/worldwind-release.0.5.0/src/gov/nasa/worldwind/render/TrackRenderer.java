/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.tracks.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: TrackRenderer.java 4521 2008-02-20 00:45:39Z tgaskins $
 */
public class TrackRenderer extends LocationRenderer
{
    private double markerPixels = 8d; // TODO: these should all be configurable
    private double minMarkerSize = 3d;
    private Material material = Material.WHITE;
    private String iconFilePath;
    private LocationRenderer.Shape shape = SPHERE;

    public TrackRenderer()
    {
    }

    public double getMarkerPixels()
    {
        return markerPixels;
    }

    public void setMarkerPixels(double markerPixels)
    {
        this.markerPixels = markerPixels;
    }

    public double getMinMarkerSize()
    {
        return minMarkerSize;
    }

    public void setMinMarkerSize(double minMarkerSize)
    {
        this.minMarkerSize = minMarkerSize;
    }

    public Material getMaterial()
    {
        return material;
    }

    public void setMaterial(Material material)
    {
        if (material == null)
        {
            String msg = Logging.getMessage("nullValue.MaterialIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // don't validate material's colors - material does that.

        this.material = material;
    }

    public String getIconFilePath()
    {
        return iconFilePath;
    }

    public void setIconFilePath(String iconFilePath)
    {
        //don't validate - a null iconFilePath cancels icon drawing
        this.iconFilePath = iconFilePath;
    }

    public void setShapeType(String shapeName)
    {
        if (shapeName.equalsIgnoreCase("Cone"))
            this.shape = CONE;
        else if (shapeName.equalsIgnoreCase("Cylinder"))
            this.shape = CYLINDER;
        else
            this.shape = SPHERE;
    }

    protected Vec4 draw(DrawContext dc, Iterator<TrackPoint> trackPositions)
    {
        if (dc.getVisibleSector() == null)
            return null;

        SectorGeometryList geos = dc.getSurfaceGeometry();
        if (geos == null)
            return null;

        if (!this.shape.isInitialized)
            this.shape.initialize(dc);

        Vec4 lastPointDrawn = null;

        this.begin(dc);
        {
            if (!dc.isPickingMode())
                this.material.apply(dc.getGL(), GL.GL_FRONT);

            Vec4 previousDrawnPoint = null;
            double radius;
            for (int index = 0; trackPositions.hasNext(); index++)
            {
                TrackPoint tp = trackPositions.next();

                if (index < this.lowerLimit)
                    continue;

                if (index > this.upperLimit)
                    break;

                Vec4 point = this.computeSurfacePoint(dc, tp);
                if (point == null)
                    continue;

                if (dc.isPickingMode())
                {
                    java.awt.Color color = dc.getUniquePickColor();
                    int colorCode = color.getRGB();
                    PickedObject po = new PickedObject(colorCode,
                        this.getClient() != null ? this.getClient() : tp.getPosition(), tp.getPosition(), false);
                    po.setValue(AVKey.PICKED_OBJECT_ID, index);
                    this.pickSupport.addPickableObject(po);
                    dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                }

                radius = this.computeMarkerRadius(dc, point);

                if (previousDrawnPoint == null)
                {
                    // It's the first point drawn
                    previousDrawnPoint = point;
                    this.shape.render(dc, point, radius);
                    lastPointDrawn = point;
                    continue;
                }

                double separation = point.distanceTo3(previousDrawnPoint);
                double minSeparation = 4d * radius;
                if (separation > minSeparation)
                {
                    previousDrawnPoint = point;
                    this.shape.render(dc, point, radius);
                    lastPointDrawn = point;
                }
            }
        }
        this.end(dc);

        return lastPointDrawn;
    }
//
//    protected Vec4 doDrawPoints(DrawContext dc, List<Vec4> points)
//    {
//        Vec4 lastPointDrawn = null;
//
//        double radius = this.minMarkerSize;
//        for (Vec4 point : points)
//        {
//            this.shape.render(dc, point, radius);
//            lastPointDrawn = point;
//        }
//
//        return lastPointDrawn;
//    }

    private double computeMarkerRadius(DrawContext dc, Vec4 point)
    {
        double d = point.distanceTo3(dc.getView().getEyePoint());
        double radius = this.markerPixels * dc.getView().computePixelSizeAtDistance(d);
        if (radius < this.minMarkerSize)
            radius = this.minMarkerSize;

        return radius;
    }
}
