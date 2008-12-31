/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.tracks.*;
import gov.nasa.worldwind.util.Logging;

/**
 * @author tag
 * @version $Id: TrackPipesLayer.java 4051 2007-12-22 19:57:09Z dcollins $
 */
public class TrackPipesLayer extends TrackLayer
{
    private PipeRenderer pipeRenderer = new PipeRenderer();

    public TrackPipesLayer(java.util.Collection<Track> tracks)
    {
        super(tracks);
    }

    @Override
    public void setLowerLimit(int lowerLimit)
    {
        super.setLowerLimit(lowerLimit);
        this.pipeRenderer.setLowerLimit(this.getLowerLimit());
    }

    @Override
    public void setUpperLimit(int upperLimit)
    {
        super.setUpperLimit(upperLimit);
        this.pipeRenderer.setUpperLimit(this.getUpperLimit());
    }

    public Material getPipeMaterial()
    {
        return this.pipeRenderer.getPipeMaterial();
    }

    public void setPipeMaterial(Material material)
    {
        this.pipeRenderer.setPipeMaterial(material);
    }

    public Material getJunctionMaterial()
    {
        return this.pipeRenderer.getJunctionMaterial();
    }

    public void setJunctionMaterial(Material material)
    {
        this.pipeRenderer.setJunctionMaterial(material);
    }

    public void setJunctionShape(String shapeName)
    {
        this.pipeRenderer.setJunctionShape(shapeName);
    }

    public String getJunctionShape()
    {
        return this.pipeRenderer.getJunctionShape();
    }

    @Override
    public void setElevation(double elevation)
    {
        super.setElevation(elevation);
        this.pipeRenderer.setElevation(elevation);
    }

    @Override
    public void setOverrideElevation(boolean overrideElevation)
    {
        super.setOverrideElevation(overrideElevation);
        this.pipeRenderer.setOverrideElevation(overrideElevation);
    }

    protected void doDraw(DrawContext dc, TrackPointIterator trackPoints, java.awt.Point pickPoint)
    {
        this.pipeRenderer.render(dc, trackPoints);
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.TrackPipesLayer.Name");
    }
}
