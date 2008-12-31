/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.globes;

import gov.nasa.worldwind.WWObject;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.geom.*;

/**
 * @author Tom Gaskins
 * @version $Id: Globe.java 5215 2008-04-30 04:37:46Z tgaskins $
 */
public interface Globe extends WWObject, Extent
{
    Vec4 computePointFromPosition(Angle latitude, Angle longitude, double metersElevation);

    Vec4 computeSurfaceNormalAtPoint(Vec4 p);

    ElevationModel getElevationModel();

    Extent getExtent();

    double getEquatorialRadius();

    double getPolarRadius();

    double getMaximumRadius();

    double getRadiusAt(Angle latitude, Angle longitude);

    double getElevation(Angle latitude, Angle longitude);

    double getMaxElevation();

    double getMinElevation();

    Position getIntersectionPosition(Line line);

    double getEccentricitySquared();

    Position computePositionFromPoint(Vec4 point);

    Vec4 computePointFromPosition(Position position);

    double getRadiusAt(LatLon latLon);

    double[] getMinAndMaxElevations(Sector sector);

    Intersection[] intersect(Line line, double altitude);

    Cylinder computeBoundingCylinder(double verticalExaggeration, Sector sector);

    Double getBestElevation(Angle latitude, Angle longitude);

    Double getElevationAtResolution(Angle latitude, Angle longitude, double resolution);

    Tessellator getTessellator();

    void setTessellator(Tessellator tessellator);

    SectorGeometryList tessellate(DrawContext dc);

    Object getStateKey();

}
