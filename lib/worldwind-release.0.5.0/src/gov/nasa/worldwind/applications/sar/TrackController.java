/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;

import java.beans.*;
import java.util.HashMap;

/**
 * @author tag
 * @version $Id: TrackController.java 4926 2008-04-04 21:04:54Z dcollins $
 */
public class TrackController
{
    public static final String TRACK_ADD = "TrackController.TrackAdded";
    public static final String TRACK_REMOVE = "TrackController.TrackRemoved";
    public static final String TRACK_MODIFY = "TrackController.TrackModified";
    public static final String TRACK_DISABLE = "TrackController.TrackDisabled";
    public static final String TRACK_ENABLE = "TrackController.TrackEnabled";
    public static final String TRACK_CURRENT = "TrackController.TrackCurrent";
    public static final String TRACK_NAME = "TrackController.TrackName";
    public static final String TRACK_DIRTY_BIT = "TrackController.TrackDirtyBit";

    public static final String BEGIN_TRACK_POINT_ENTRY = "TrackController.BeginTrackPointEntry";
    public static final String END_TRACK_POINT_ENTRY = "TrackController.EndTrackPointEntry";

    private WorldWindow wwd;
    private TracksPanel tracksPanel;
    private AnalysisPanel analysisPanel;
    private HashMap<SARTrack, Layer> trackLayers = new HashMap<SARTrack, Layer>();
    private SARTrackBuilder trackBuilder;

    public TrackController()
    {
        this.trackBuilder = new SARTrackBuilder();
    }

    public WorldWindow getWwd()
    {
        return wwd;
    }

    public void setWwd(WorldWindow wwd)
    {
        if (wwd == this.wwd)
            return;
        
        this.wwd = wwd;
        this.trackBuilder.setWwd(this.wwd);
    }

    public TracksPanel getTracksPanel()
    {
        return tracksPanel;
    }

    public void setTracksPanel(TracksPanel tracksPanel)
    {
        this.tracksPanel = tracksPanel;
    }

    public AnalysisPanel getAnalysisPanel()
    {
        return analysisPanel;
    }

    public void setAnalysisPanel(AnalysisPanel analysisPanel)
    {
        this.analysisPanel = analysisPanel;
    }

    public void addTrack(SARTrack track)
    {
        if (track == null)
            return;

        this.createPolylineTrackRepresentation(track);

        track.addPropertyChangeListener(new PropertyChangeListener()
        {
            @SuppressWarnings({"StringEquality"})
            public void propertyChange(PropertyChangeEvent propertyChangeEvent)
            {
                if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_REMOVE)
                    removeTrack((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_MODIFY)
                    updateTrack((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_ENABLE)
                    enableTrack((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_DISABLE)
                    disableTrack((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_CURRENT)
                    trackCurrent((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_NAME)
                    trackName((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_DIRTY_BIT)
                    trackDirtyBit((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.BEGIN_TRACK_POINT_ENTRY)
                    beginTrackPointEntry((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.END_TRACK_POINT_ENTRY)
                    endTrackPointEntry((SARTrack) propertyChangeEvent.getSource());
            }
        });

        this.tracksPanel.addTrack(track);
    }

    public SARTrack getCurrentTrack()
    {
        return this.tracksPanel.getCurrentTrack();
    }

    public void refreshCurrentTrack()
    {
        trackCurrent(getCurrentTrack());
    }

    private void createPolylineTrackRepresentation(SARTrack track)
    {
        Polyline airPath = new Polyline(track);
        airPath.setOffset(track.getOffset());
        airPath.setPathType(Polyline.RHUMB_LINE);
        airPath.setColor(track.getColor());

        Polyline groundPath = new Polyline(track);
        groundPath.setFollowTerrain(true);
        groundPath.setPathType(Polyline.RHUMB_LINE);
        groundPath.setColor(track.getColor());
        groundPath.setStippleFactor(5);
        groundPath.setStipplePattern((short) 0xAAAA);

        RenderableLayer layer = new RenderableLayer();
        layer.addRenderable(airPath);
        layer.addRenderable(groundPath);
        this.wwd.getModel().getLayers().add(layer);
        if (this.wwd != null)
            this.wwd.redraw();
        this.trackLayers.put(track, layer);
    }

    private void removeTrack(SARTrack track)
    {
        Layer layer = this.trackLayers.get(track);
        if (layer == null)
            return;

        this.trackLayers.remove(track);
        this.wwd.getModel().getLayers().remove(layer);
        if (this.wwd != null)
            this.wwd.redraw();
    }

    private void enableTrack(SARTrack track)
    {
        RenderableLayer layer = (RenderableLayer) this.trackLayers.get(track);
        if (layer == null)
            return;

        layer.setEnabled(true);
        if (this.wwd != null)
            this.wwd.redraw();
    }

    private void disableTrack(SARTrack track)
    {
        RenderableLayer layer = (RenderableLayer) this.trackLayers.get(track);
        if (layer == null)
            return;

        layer.setEnabled(false);
        if (this.wwd != null)
            this.wwd.redraw();
    }

    private void updateTrack(SARTrack track)
    {
        RenderableLayer layer = (RenderableLayer) this.trackLayers.get(track);
        if (layer == null)
            return;

        for (Renderable r : layer.getRenderables())
        {
            Polyline line = (Polyline) r;
            line.setPositions(track);
            if (!line.isFollowTerrain())
                line.setOffset(track.getOffset());
        }
        if (this.wwd != null)
            this.wwd.redraw();
    }

    private void trackCurrent(SARTrack track)
    {
        this.analysisPanel.setCurrentTrack(track);
        this.trackBuilder.setArmed(false);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void trackName(SARTrack track)
    {
        // Intentionally left blank, as a placeholder for future functionality.
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void trackDirtyBit(SARTrack track)
    {
        // Intentionally left blank, as a placeholder for future functionality.
    }

    private void beginTrackPointEntry(SARTrack track)
    {
        this.trackBuilder.setArmed(false);

        this.trackBuilder.setTrack(track);
        this.trackBuilder.setArmed(true);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void endTrackPointEntry(SARTrack track)
    {
        this.trackBuilder.setArmed(false);
    }
}
