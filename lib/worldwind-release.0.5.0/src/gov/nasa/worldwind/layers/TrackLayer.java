/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.tracks.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.globes.SectorGeometryList;

/**
 * The <code>TrackLayer</code> class manages a collection of {@link gov.nasa.worldwind.tracks.Track} objects
 * for rendering and picking.
 *
 * @author tag
 * @version $Id: TrackLayer.java 4051 2007-12-22 19:57:09Z dcollins $
 * @see gov.nasa.worldwind.tracks.Track
 */
public abstract class TrackLayer extends AbstractLayer
{
    private java.util.Collection<Track> tracks = new java.util.ArrayList<Track>();
    private Iterable<Track> tracksOverride;
    private Sector boundingSector;
    private int lowerLimit;
    private int upperLimit;
    private boolean overrideElevation = false;
    private double elevation = 10d;

    /**
     * Creates a new <code>TrackLayer</code> with the specified <code>tracks</code> in its internal collection.
     *
     * @param tracks Collection of <code>Tracks</code> to add to this layer's internal collection.
     */
    public TrackLayer(java.util.Collection<Track> tracks)
    {
        if (tracks == null)
        {
            String msg = Logging.getMessage("nullValue.TracksIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.tracks.addAll(tracks);
        this.boundingSector = Sector.boundingSector(this.iterator());
    }

    public TrackPointIterator iterator()
    {
        return new TrackPointIteratorImpl(getActiveTracks());
    }

    public int getNumPoints()
    {
        return ((TrackPointIteratorImpl)this.iterator()).getNumPoints();
    }

    /**
     * Returns the Iterable of Tracks currently in use by this layer.
     * If the caller has specified a custom Iterable via {@link #setTrackIterable}, this will returns a reference
     * to that Iterable. If the caller passed <code>setTrackIterable</code> a null parameter,
     * or if <code>setTrackIterable</code> has not been called, this returns a view of this layer's internal
     * collection of Tracks.
     *
     * @return Iterable of currently active Tracks.
     */
    public Iterable<Track> getTracks()
    {
        return getActiveTracks();
    }

    /**
     * Returns the Iterable of currently active Tracks.
     * If the caller has specified a custom Iterable via {@link #setTrackIterable}, this will returns a reference
     * to that Iterable. If the caller passed <code>setTrackIterable</code> a null parameter,
     * or if <code>setTrackIterable</code> has not been called, this returns a view of this layer's internal
     * collection of Tracks.
     *
     * @return Iterable of currently active Tracks.
     */
    private Iterable<Track> getActiveTracks()
    {
        if (this.tracksOverride != null)
        {
            return this.tracksOverride;
        }
        else
        {
            // Return an unmodifiable reference to the internal list of tracks.
            // This prevents callers from changing this list and invalidating any invariants we have established.
            return java.util.Collections.unmodifiableCollection(this.tracks);
        }
    }

    /**
     * Sets the specified <code>tracks</code> as this layer's internal collection.
     * If this layer's internal collection has been overriden with a call to {@link #setTrackIterable},
     * this will throw an exception.
     *
     * @param tracks List of Tracks to use.
     * @throws IllegalArgumentException If <code>tracks</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setTrackIterable</code>.
     */
    public void setTracks(java.util.Collection<Track> tracks)
    {
        if (tracks == null)
        {
            String msg = Logging.getMessage("nullValue.TracksIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.tracksOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        clearTracks();
        this.tracks.addAll(tracks);
    }

    /**
     * Overrides the collection of currently active Tracks with the specified <code>trackIterable</code>.
     * This layer will maintain a reference to <code>trackIterable</code> strictly for picking and rendering.
     * This layer will not modify the Iterable reference. However, this will clear
     * the internal collection of Tracks, and will prevent any modification to its contents via
     * <code>setTracks</code>.
     *
     * If the specified <code>trackIterable</code> is null, this layer will revert to maintaining its internal
     * collection.
     *
     * @param trackIterable Iterable to use instead of this layer's internal collection, or null to use this
     *                      layer's internal collection.
     */
    public void setTrackIterable(Iterable<Track> trackIterable)
    {
        this.tracksOverride = trackIterable;
        // Clear the internal collection of Tracks.
        clearTracks();
    }

    private void clearTracks()
    {
        if (this.tracks != null && this.tracks.size() > 0)
            this.tracks.clear();
    }

    public Sector getBoundingSector()
    {
        return boundingSector;
    }

    public void setBoundingSector(Sector boundingSector)
    {
        this.boundingSector = boundingSector;
    }

    public int getLowerLimit()
    {
        return lowerLimit;
    }

    public void setLowerLimit(int lowerLimit)
    {
        this.lowerLimit = lowerLimit;
    }

    public int getUpperLimit()
    {
        return upperLimit;
    }

    public void setUpperLimit(int upperLimit)
    {
        this.upperLimit = upperLimit;
    }

    public double getElevation()
    {
        return this.elevation;
    }

    public void setElevation(double markerElevation)
    {
        this.elevation = markerElevation;
    }

    public boolean isOverrideElevation()
    {
        return this.overrideElevation;
    }

    public void setOverrideElevation(boolean overrideElevation)
    {
        this.overrideElevation = overrideElevation;
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.TrackLayer.Name");
    }

    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        this.draw(dc, pickPoint);
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        this.draw(dc, null);
    }

    private void draw(DrawContext dc, java.awt.Point pickPoint)
    {
        TrackPointIterator trackPoints = this.iterator();
        if (!trackPoints.hasNext())
            return;

        if (dc.getVisibleSector() == null)
            return;

        SectorGeometryList geos = dc.getSurfaceGeometry();
        if (geos == null)
            return;

        if (!dc.getVisibleSector().intersects(this.getBoundingSector()))
            return;

        this.doDraw(dc, trackPoints, pickPoint);
    }

    protected abstract void doDraw(DrawContext dc, TrackPointIterator trackPoints, java.awt.Point pickPoint);
}
