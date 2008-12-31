/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.DragSelectEvent;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author tag
 * @version $Id: BasicDragger2.java 4909 2008-04-03 22:40:05Z patrickmurris $
 */
public class BasicDragger2 implements SelectListener
{
    private final WorldWindow wwd;
    private boolean dragging = false;

    private Point dragRefCursorPoint;
    private Vec4 dragRefObjectPoint;
    private double dragRefAltitude;

    public BasicDragger2(WorldWindow wwd)
    {
        if (wwd == null)
        {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.wwd = wwd;
    }

    public boolean isDragging()
    {
        return this.dragging;
    }

    public void selected(SelectEvent event)
    {
        if (event == null)
        {
            String msg = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (event.getEventAction().equals(SelectEvent.DRAG_END))
        {
            this.dragging = false;
        }
        else if (event.getEventAction().equals(SelectEvent.DRAG))
        {
            DragSelectEvent dragEvent = (DragSelectEvent) event;
            Object topObject = dragEvent.getTopObject();
            if (topObject == null)
                return;

            if (!(topObject instanceof Movable))
                return;

            Movable dragObject = (Movable) topObject;

            View view = wwd.getView();
            Globe globe = wwd.getModel().getGlobe();

            // Compute dragged object ref-point in model coordinates.
            // Use the Icon and Annotation logic of elevation as offset above ground when below max elevation.
            Position refPos = dragObject.getReferencePosition();
            Vec4 refPoint = null;
            if (refPos.getElevation() < globe.getMaxElevation())
                refPoint = wwd.getSceneController().getTerrain().getSurfacePoint(refPos);
            if (refPoint == null)
                refPoint = globe.computePointFromPosition(refPos);

            if (!this.isDragging())   // Dragging started
            {
                // Save initial reference points for object and cursor in screen coordinates
                // Note: y is inverted for the object point.
                this.dragRefObjectPoint = view.project(refPoint);
                // Save cursor position
                this.dragRefCursorPoint = dragEvent.getPreviousPickPoint();
                // Save start altitude
                this.dragRefAltitude = globe.computePositionFromPoint(refPoint).getElevation();
            }

            // Compute screen-coord delta since drag started.
            int dx = dragEvent.getPickPoint().x - this.dragRefCursorPoint.x;
            int dy = dragEvent.getPickPoint().y - this.dragRefCursorPoint.y;

            // Find intersection of screen coord (refObjectPoint + delta) with globe.
            double x = this.dragRefObjectPoint.x + dx;
            double y = event.getMouseEvent().getComponent().getSize().height - this.dragRefObjectPoint.y + dy - 1;
            Line ray = view.computeRayFromScreenPoint(x, y);
            Position pickPos = null;
            if (view.getEyePosition().getElevation() < globe.getMaxElevation() * 10)
            {
                // Use ray casting below some altitude
                pickPos = RayCastingSupport.intersectRayWithTerrain(globe,
                        ray.getOrigin(), ray.getDirection(), 200, 20);
            }
            else
            {
                // Use intersection with sphere at reference altitude.
                Intersection inters[] = globe.intersect(ray, this.dragRefAltitude);
                if (inters != null)
                    pickPos = globe.computePositionFromPoint(inters[0].getIntersectionPoint());
            }
            if (pickPos != null)
            {
                // Intersection with globe. Move reference point to the intersection point,
                // but maintain current altitude.
                Position p = new Position( pickPos.getLatLon(), dragObject.getReferencePosition().getElevation());
                dragObject.moveTo(p);
            }
            this.dragging = true;
        }
    }
}