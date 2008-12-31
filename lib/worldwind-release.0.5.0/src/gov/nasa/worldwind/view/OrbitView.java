/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;

/**
 * @author dcollins
 * @version $Id: OrbitView.java 4994 2008-04-09 18:15:52Z dcollins $
 */
public interface OrbitView extends View
{
    Position getCenterPosition();

    void setCenterPosition(Position center);

    Angle getHeading();

    void setHeading(Angle heading);

    Angle getPitch();

    void setPitch(Angle pitch);

    double getZoom();

    void setZoom(double zoom);

    OrbitViewModel getOrbitViewModel();

    boolean canFocusOnViewportCenter();

    void focusOnViewportCenter();

    public static final String CENTER_STOPPED = "gov.nasa.worldwind.view.OrbitView.CenterStopped";

    void stopMovementOnCenter();
}
