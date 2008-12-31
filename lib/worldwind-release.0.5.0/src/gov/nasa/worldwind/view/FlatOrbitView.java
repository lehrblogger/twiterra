/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;

/**
 * @author Patrick Muris
 * @version $Id: BasicOrbitView.java 4111 2008-01-09 15:06:41Z dcollins $
 */
public class FlatOrbitView extends BasicOrbitView
{
    // Properties updated in doApply().
    private Globe globe;

    // TODO: make configurable
    private static final double MINIMUM_FAR_DISTANCE = 100;
    
    public FlatOrbitView()
    {
        super(new FlatOrbitViewModel());
    }

    public double getAutoFarClipDistance()
    {
        // Use the current eye point to auto-configure the far clipping plane distance.
        Vec4 eyePoint = this.getCurrentEyePoint();
        return computeFarDistance(eyePoint);
    }

    private double computeFarDistance(Vec4 eyePoint)
    {
        double far = this.computeHorizonDistance(eyePoint);
        return far < MINIMUM_FAR_DISTANCE ? MINIMUM_FAR_DISTANCE : far;
    }

    protected void doApply(DrawContext dc)
    {
        // Keep the last Globe used for horizon distance computation.
        this.globe = dc.getGlobe();
        // Invoke superclass functionality.
        super.doApply(dc);
    }

    public double computeHorizonDistance()
    {
        // Use the eye point from the last call to apply() to compute horizon distance.
        Vec4 eyePoint = this.getEyePoint();
        return this.computeHorizonDistance(eyePoint);
    }

    private double computeHorizonDistance(Vec4 eyePoint)
    {
        double horizon = 0;
        // Compute largest distance to flat globe 'corners'.
        if (this.globe != null && eyePoint != null)
        {
            double dist = 0;
            Vec4 p;
            // Use max distance to six points around the map
            p = this.globe.computePointFromPosition(Angle.POS90, Angle.NEG180, 0); // NW
            dist = Math.max(dist, eyePoint.distanceTo3(p));
            p = this.globe.computePointFromPosition(Angle.POS90, Angle.POS180, 0); // NE
            dist = Math.max(dist, eyePoint.distanceTo3(p));
            p = this.globe.computePointFromPosition(Angle.NEG90, Angle.NEG180, 0); // SW
            dist = Math.max(dist, eyePoint.distanceTo3(p));
            p = this.globe.computePointFromPosition(Angle.NEG90, Angle.POS180, 0); // SE
            dist = Math.max(dist, eyePoint.distanceTo3(p));
            p = this.globe.computePointFromPosition(Angle.ZERO, Angle.POS180, 0); // E
            dist = Math.max(dist, eyePoint.distanceTo3(p));
            p = this.globe.computePointFromPosition(Angle.ZERO, Angle.NEG180, 0); // W
            dist = Math.max(dist, eyePoint.distanceTo3(p));
            horizon = dist;
        }
        return horizon;
    }
}
