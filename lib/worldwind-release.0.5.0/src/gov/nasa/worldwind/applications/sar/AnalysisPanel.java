/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.CrosshairLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.view.FlyToOrbitViewStateIterator;
import gov.nasa.worldwind.view.OrbitView;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author tag
 * @version $Id: AnalysisPanel.java 5066 2008-04-16 20:54:27Z dcollins $
 */
public class AnalysisPanel extends JPanel
{
    private WorldWindow wwd;
    private SARTrack currentTrack;
    private TrackViewPanel trackViewPanel;
    private TerrainProfilePanel terrainProfilePanel;

    private CrosshairLayer crosshairLayer;
    private RenderableLayer planeModelLayer;
    private PlaneModel planeModel;

    private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener()
    {
        @SuppressWarnings({"StringEquality"})
        public void propertyChange(PropertyChangeEvent propertyChangeEvent)
        {
            if (propertyChangeEvent.getPropertyName() == TrackViewPanel.VIEW_CHANGE)
            {
                // When the track mode has changed, update the view parameters gradually.
                updateView(true);
            }
            else if (propertyChangeEvent.getPropertyName() == TrackViewPanel.POSITION_CHANGE)
            {
                // When the track position has changed, update the view parameters immediately.
                updateView(false);
            }
            else if (propertyChangeEvent.getPropertyName() == AVKey.ELEVATION_MODEL
                && trackViewPanel.isExamineViewMode() && !wwd.getView().hasStateIterator())
            {
                // When the elevation model changes, and the view is examining the terrain beneath the track
                // (but has not active state iterators), update the view parameters immediately.
                updateView(false);
            }
            else if (propertyChangeEvent.getPropertyName() == SAR2.ELEVATION_UNIT)
            {
                updateElevationUnit(propertyChangeEvent.getNewValue());
            }

            if ((propertyChangeEvent.getPropertyName() == AVKey.VIEW
                || propertyChangeEvent.getPropertyName() == AVKey.VIEW_QUIET)
                && trackViewPanel.isFollowViewMode())
            {
                updateCrosshair();
            }
        }
    };

    public AnalysisPanel()
    {
        initComponents();

        // Init plane model layer
        this.planeModel = new PlaneModel(100d, 100d, Color.YELLOW);
        this.planeModel.setShadowScale(0.1);
        this.planeModel.setShadowColor(new Color(255, 255, 0, 192));
        this.planeModelLayer = new RenderableLayer();
        this.planeModelLayer.setName("Plane Model");
        this.planeModelLayer.addRenderable(this.planeModel);
        //Init crosshair layer
        this.crosshairLayer = new CrosshairLayer("images/64x64-crosshair.png");
        this.crosshairLayer.setOpacity(0.4);
        this.crosshairLayer.setEnabled(false);

        this.trackViewPanel.addPropertyChangeListener(this.propertyChangeListener);
    }

    public void setWwd(WorldWindow wwd)
    {
        if (this.wwd != null)
        {
            this.wwd.removePropertyChangeListener(this.propertyChangeListener);
            this.wwd.getModel().getGlobe().getElevationModel().removePropertyChangeListener(this.propertyChangeListener);
            this.wwd.getView().removePropertyChangeListener(this.propertyChangeListener);
        }
        this.wwd = wwd;
        this.terrainProfilePanel.setWwd(wwd);
        if (this.wwd != null)
        {
            this.wwd.addPropertyChangeListener(this.propertyChangeListener);
            this.wwd.getModel().getGlobe().getElevationModel().addPropertyChangeListener(this.propertyChangeListener);
            this.wwd.getView().addPropertyChangeListener(this.propertyChangeListener);
            ApplicationTemplate.insertBeforeCompass(wwd, this.planeModelLayer);
            ApplicationTemplate.insertBeforeCompass(wwd, this.crosshairLayer);
        }
    }

    public void setCurrentTrack(SARTrack currentTrack)
    {
        this.currentTrack = currentTrack;
        this.trackViewPanel.setCurrentTrack(currentTrack);
    }

    private void updateElevationUnit(Object newValue)
    {
        if (newValue != null)
        {
            this.trackViewPanel.setElevationUnit(newValue.toString());
            this.trackViewPanel.updateReadout(this.getPositionAlongSegment());
        }
    }

    private Angle getControlHeading()
    {
        return Angle.ZERO;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private Angle getControlPitch()
    {
        return Angle.fromDegrees(80);
    }

    private Angle getControlFOV()
    {
        return Angle.fromDegrees(45);
    }

    private void updateView(boolean goSmoothly)
    {
        OrbitView view = (OrbitView) this.wwd.getView();
        view.setFieldOfView(this.getControlFOV());

        if (this.trackViewPanel.isOverrideClipDistance())
        {
            view.setNearClipDistance(this.trackViewPanel.getClipDistance());
            view.setDetectCollisions(false);
        }
        else
        {
            view.setNearClipDistance(-1); // Tells View to auto-compute the near clip distance.
            view.setDetectCollisions(!trackViewPanel.isSubsurfaceOkay());
        }

        Position pos = this.getPositionAlongSegment();
        if (pos != null)
        {
            Angle heading = this.getHeading().add(this.getControlHeading());

            this.terrainProfilePanel.updatePosition(pos, heading);
            this.planeModel.setPosition(pos);
            this.planeModel.setHeading(heading);
            this.crosshairLayer.setEnabled(false);  // Turn off crosshair by default

            if (this.trackViewPanel.isExamineViewMode())
            {
                this.terrainProfilePanel.setFollowObject();
                // Set the view center point to the current track position on the ground - spheroid.
                // This gets the eye looking at the cross section.
                Position groundPos = getSmoothedGroundPositionAlongSegment();
                if (groundPos == null)
                    groundPos = getGroundPositionAlongSegment();

                if (goSmoothly)
                {
                    Angle initialPitch = Angle.fromDegrees(Math.min(60, view.getPitch().degrees));
                    double initialZoom = 10000;
                    // If the player is active, set initial parameters immediately.
                    // Otherwise, set initial parameters gradually.
                    if (this.trackViewPanel.isPlayerActive())
                    {
                        view.setCenterPosition(groundPos);
                        view.setZoom(initialZoom);
                        view.setPitch(initialPitch);
                    }
                    else
                    {
                        view.applyStateIterator(FlyToOrbitViewStateIterator.createPanToIterator(
                            view, this.wwd.getModel().getGlobe(),
                            groundPos, view.getHeading(), initialPitch, initialZoom, true));
                    }
                }
                else
                {
                    // Stop any state iterators, and center movement only.
                    view.stopStateIterators();
                    view.stopMovementOnCenter();
                    // Set the view to center on the track position,
                    // while keeping the eye altitude constant.
                    try
                    {
                        Position eyePos = view.getCurrentEyePosition();
                        // New eye lat/lon will follow the ground position.
                        LatLon newEyeLatLon = eyePos.add(groundPos.subtract(view.getCenterPosition())).getLatLon();
                        // Eye elevation will not change unless it is below the ground position elevation.
                        double newEyeElev = eyePos.getElevation() < groundPos.getElevation() ?
                                groundPos.getElevation() : eyePos.getElevation();

                        Position newEyePos = new Position(newEyeLatLon, newEyeElev);
                        view.setOrientation(newEyePos, groundPos);
                    }
                    // Fallback to setting center position.
                    catch (Exception e)
                    {
                        view.setCenterPosition(groundPos);
                        // View/OrbitView will have logged the exception, no need to log it here.
                    }
                }
            }
            else if (this.trackViewPanel.isFollowViewMode())
            {
                Angle pitch = Angle.POS90;
                double zoom = 0;

                this.updateCrosshair();
                this.terrainProfilePanel.setFollowObject();

                // Place the eye at the track current lat-lon and altitude, with the proper heading
                // and pitch from slider. Intended to simulate the view from the plane.
                if (goSmoothly)
                {
                    // If the player is active, set initial parameters immediately.
                    // Otherwise, set initial parameters gradually.
                    if (this.trackViewPanel.isPlayerActive())
                    {
                        view.setCenterPosition(pos);
                        view.setHeading(heading);
                        view.setPitch(pitch);
                        view.setZoom(zoom);
                    }
                    else
                    {
                        view.applyStateIterator(FlyToOrbitViewStateIterator.createPanToIterator(
                            view, this.wwd.getModel().getGlobe(),
                            pos, heading, pitch, zoom));
                    }
                }
                else
                {
                    // Stop any state iterators, and any view movement.
                    view.stopStateIterators();
                    view.stopMovement();
                    // Set the view values to follow the track.
                    view.setCenterPosition(pos);
                    view.setHeading(heading);
                    view.setPitch(pitch);
                    view.setZoom(zoom);
                }
            }
            else if (this.trackViewPanel.isFreeViewMode())
            {
                if (goSmoothly)
                {
                    // Stop any state iterators, and any view movement.
                    view.stopStateIterators();
                    view.stopMovement();
                    // Set the view's center position to a point on the ground (without moving the eye).
                    // This is needed to ensure normal interactions immediately.
                    try
                    {
                        view.focusOnViewportCenter();
                    }
                    catch (Exception e)
                    {
                        // View/OrbitView will have logged the exception, no need to log it here.
                    }
                }
            }
        }

        this.trackViewPanel.updateReadout(pos);
        this.wwd.redraw();
    }

    private int getCurrentPositionNumber()
    {
        return this.trackViewPanel.getCurrentPositionNumber();
    }

    private boolean isLastPosition(int n)
    {
        return n >= this.currentTrack.size() - 1;
    }

    private Position getCurrentSegmentStartPosition()
    {
        if (this.currentTrack == null || this.currentTrack.size() == 0)
            return null;

        Position pos;
        int n = this.getCurrentPositionNumber();
        if (isLastPosition(n))
            pos = this.currentTrack.get(this.currentTrack.size() - 1);
        else
            pos = this.currentTrack.get(n);

        return new Position(pos.getLatitude(), pos.getLongitude(), pos.getElevation() + this.currentTrack.getOffset());
    }

    private Position getCurrentSegmentEndPosition()
    {
        if (this.currentTrack == null || this.currentTrack.size() == 0)
            return null;

        Position pos;
        int n = this.getCurrentPositionNumber();
        if (isLastPosition(n + 1))
            pos = this.currentTrack.get(this.currentTrack.size() - 1);
        else
            pos = this.currentTrack.get(n + 1);

        return new Position(pos.getLatitude(), pos.getLongitude(), pos.getElevation() + this.currentTrack.getOffset());
    }

    private Position getPositionAlongSegment()
    {
        double t = this.trackViewPanel.getPositionDelta();
        return this.getPositionAlongSegment(t);
    }

    private Position getPositionAlongSegment(double t)
    {
        Position pa = this.getCurrentSegmentStartPosition();
        if (pa == null)
            return null;
        Position pb = this.getCurrentSegmentEndPosition();
        if (pb == null)
            return pa;

        return interpolateTrackPosition(t, pa, pb);
    }

    private Angle getHeading()
    {
        Position pA;
        Position pB;

        int cpn = this.getCurrentPositionNumber();
        if (!this.isLastPosition(cpn))
        {
            pA = this.currentTrack.get(cpn);
            pB = this.currentTrack.get(cpn + 1);
        }
        else
        {
            pA = this.currentTrack.get(cpn - 1);
            pB = this.currentTrack.get(cpn);
        }

        return LatLon.greatCircleAzimuth(pA.getLatLon(), pB.getLatLon());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private Position getGroundPositionAlongSegment()
    {
        if (this.wwd == null)
            return null;

        Position pos = getPositionAlongSegment();
        if (pos == null)
            return null;

        double elevation = this.wwd.getModel().getGlobe().getElevation(pos.getLatitude(), pos.getLongitude());
        return new Position(pos.getLatLon(), elevation);
    }

    // TODO: weighted average should be over actual polyline track points
    private Position getSmoothedGroundPositionAlongSegment()
    {
        if (this.currentTrack == null || this.currentTrack.size() == 0)
            return null;

        Position start = getCurrentSegmentStartPosition();
        Position end = getCurrentSegmentEndPosition();
        if (start == null || end == null)
            return null;

        Globe globe = this.wwd.getModel().getGlobe();
        if (globe == null)
            return null;

        int n = this.getCurrentPositionNumber();
        double t = this.trackViewPanel.getPositionDelta();
        // Limit t to 0 if this is the last position.
        if (isLastPosition(n))
            t = 0;

        double tstep = 1 / 100.0;
        int numWeights = 15; // TODO: extract to configurable property

        double elev = 0;
        double sumOfWeights = 0;
        // Compute the moving weighted average of track positions on both sides of the current track position.
        for (int i = 0; i < numWeights; i++)
        {
            double tt;
            Position pos;

            // Previous ground positions.
            tt = t - i * tstep;
            pos = null;
            if (tt >= 0) // Position is in the current track segment.
                pos = interpolateTrackPosition(tt, start, end);
            else if (tt < 0 && n > 0) // Position is in the previous track segment.
                pos = interpolateTrackPosition(tt + 1, this.currentTrack.get(n-1), start);
            if (pos != null)
            {
                double e = globe.getElevation(pos.getLatitude(), pos.getLongitude());
                elev += (numWeights - i) * e;
                sumOfWeights += (numWeights - i);
            }

            // Next ground positions.
            // We don't want to count the first position twice.
            if (i != 0)
            {
                tt = t + i * tstep;
                pos = null;
                if (tt <= 1) // Position is in the current track segment.
                    pos = interpolateTrackPosition(tt, start, end);
                else if (tt > 1 && !isLastPosition(n + 1)) // Position is in the next track segment.
                    pos = interpolateTrackPosition(tt - 1, end, this.currentTrack.get(n+2));
                if (pos != null)
                {
                    double e = globe.getElevation(pos.getLatitude(), pos.getLongitude());
                    elev += (numWeights - i) * e;
                    sumOfWeights += (numWeights - i);
                }
            }
        }
        elev /= sumOfWeights;

        Position actualPos = interpolateTrackPosition(t, start, end);
        return new Position(actualPos.getLatLon(), elev);
    }

    /**
     * SAR tracks points are connected with lines of constant heading (rhumb lines). In order to compute
     * an interpolated position between two track points, we must use rhumb computations, rather
     * than linearly interpolate the position.
     * @param t a decimal number between 0 and 1
     * @param begin first position
     * @param end second position
     * @return Position in between begin and end
     */
    private Position interpolateTrackPosition(double t, Position begin, Position end)
    {
        if (begin == null || end == null)
            return null;

        LatLon lla = begin.getLatLon();
        LatLon llb = end.getLatLon();
        // The track is drawn as a rhumb line.
        // Therefore we must use rhumb computations to interpolate lat/lon.
        Angle az = LatLon.rhumbAzimuth(lla, llb);
        Angle dist = LatLon.rhumbDistance(lla, llb);
        dist = dist.multiply(t);
        LatLon ll = LatLon.rhumbEndPosition(lla, az, dist);
        // Elevation is independent of track line type (i.e. rhumb, great-circle, linear),
        // so we interpolate elevation normally.
        double e = (1d - t) * begin.getElevation() + t * end.getElevation();
        return new Position(ll, e);
    }

//    /**
//     * Compute crosshair location in viewport for 'follow' - 'fly-it' mode.
//     * <p>
//     * It computes the intersection of the air track with the near clipping plane
//     * and determines the corresponding crosshair position in the viewport.</p>
//     * <p>
//     * This assumes the view is headed in the same direction as the air track,
//     * and the eye is set to look at the aircraft from a distance and angle.</p>
//     *
//     * @param view the current <code>View</code>
//     * @param a view pitch angle relative to the air track (0 degree = horizontal)
//     * @param distance eye distance from the aircraft
//     * @return the crosshair center position in the viewport.
//     */
//    private Vec4 computeCrosshairPosition(View view, Angle a, double distance)
//    {
//        double hfovH = view.getFieldOfView().radians / 2; // half horizontal fov in radians
//        double hw = view.getViewport().width / 2;         // half viewport width
//        double hh = view.getViewport().height / 2;        // half viewport height
//        double d = hw / Math.tan(hfovH);                  // distance to viewport plane in pixels
//        // distance to near plane in meters
//        double dNearMeter = Math.abs(view.getFrustum().getNear().getDistance());
//        // crosshair elevation above viewport center in meter
//        double dyMeter = (dNearMeter - distance) * Math.sin(a.radians);
//        // corresponding vertical fov half angle
//        double ay = Math.atan(dyMeter / dNearMeter);
//        // corresponding viewport crosshair elevation in pixels
//        double dy = Math.tan(ay) * d;
//        // final crosshair viewport position
//        return new Vec4(hw, hh + dy, 0, 0);
//    }

    // Update crosshair position to follow the air track
    private void updateCrosshair()
    {
        Vec4 crosshairPos = computeCrosshairPosition();
        if (crosshairPos != null)
        {
            this.crosshairLayer.setEnabled(true);
            this.crosshairLayer.setLocationCenter(crosshairPos);
        }
        else
            this.crosshairLayer.setEnabled(false);

    }

    // Compute cartesian intersection between the current air track segment and the near plane.
    // Follow rhumb line segments.
    private Vec4 computeCrosshairPosition()
    {
        Position posA = getCurrentSegmentStartPosition();
        Position posB = getCurrentSegmentEndPosition();
        Angle segmentAzimuth = LatLon.rhumbAzimuth(posA.getLatLon(), posB.getLatLon());
        Angle segmentDistance = LatLon.rhumbDistance(posA.getLatLon(), posB.getLatLon());
        Globe globe = this.wwd.getModel().getGlobe();
        Plane near = this.wwd.getView().getFrustumInModelCoordinates().getNear();
        int numSubsegments = 10;  // TODO: get from track polyline
        double step = 1d / numSubsegments;
        Position p1 = null, p2;
        for (double s = 0; s <= 1; s += step)
        {
            if (s == 0)
                p2 = posA;
            else if (s >= 1)
                p2 = posB;
            else
            {
                Angle distance = Angle.fromRadians(s * segmentDistance.radians);
                LatLon latLon = LatLon.rhumbEndPosition(posA.getLatLon(), segmentAzimuth, distance);
                p2 = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
            }
            if (p1 != null)
            {
                Vec4 pa = globe.computePointFromPosition(p1);
                Vec4 pb = globe.computePointFromPosition(p2);
                if(pa.distanceTo3(pb) > 0)
                {
                    Vec4 intersection = near.intersect(pa, pb);
                    if (intersection != null)
                    {
                        return this.wwd.getView().project(intersection);
                    }
                }
            }
            p1 = p2;
        }
        return null;
    }

    private void initComponents()
    {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
                
        this.trackViewPanel = new TrackViewPanel();
        this.trackViewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(this.trackViewPanel);

        this.terrainProfilePanel = new TerrainProfilePanel();
        this.terrainProfilePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(this.terrainProfilePanel);
    }
}
