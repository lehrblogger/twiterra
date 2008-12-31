/*
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;

/**
 * @author Patrick Muris from dcollins BasicOrbitViewModel
 * @version $Id: FlatOrbitViewModel.java 4292 2008-01-29 21:18:10Z dcollins $
 */
class FlatOrbitViewModel implements OrbitViewModel
{
    private static class BasicModelCoordinates implements FlatOrbitViewModel.ModelCoordinates
    {
        private final Position center;
        private final Angle heading;
        private final Angle pitch;
        private final double zoom;

        private BasicModelCoordinates(Position center, Angle heading, Angle pitch, double zoom)
        {
            if (center == null)
            {
                String message = Logging.getMessage("nullValue.PositionIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (heading == null || pitch == null)
            {
                String message = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.center = center;
            this.heading = heading;
            this.pitch = pitch;
            this.zoom = zoom;
        }

        public Position getCenterPosition()
        {
            return this.center;
        }

        public Angle getHeading()
        {
            return this.heading;
        }

        public Angle getPitch()
        {
            return this.pitch;
        }

        public double getZoom()
        {
            return this.zoom;
        }
    }

    FlatOrbitViewModel()
    {
    }

    public Matrix computeTransformMatrix(Globe globe, Position center, Angle heading, Angle pitch, double zoom)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (center == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (heading == null || pitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Construct the model-view transform matrix for the specified coordinates.
        // Because this is a model-view transform, matrices are applied in reverse order.
        Matrix transform = Matrix.IDENTITY;
        // Zoom.
        transform = transform.multiply(Matrix.fromTranslation(0, 0, -zoom));
        // Heading and pitch.
        transform = transform.multiply(Matrix.fromRotationX(pitch.multiply(-1)));
        transform = transform.multiply(Matrix.fromRotationZ(heading));
        // Center position.
        transform = transform.multiply(computeCenterTransform(globe, center));

        return transform;
    }

    public ModelCoordinates computeModelCoordinates(Globe globe, Vec4 eyePoint, Vec4 centerPoint, Vec4 up)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (eyePoint == null || centerPoint == null || up == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Matrix modelview = Matrix.fromLookAt(eyePoint, centerPoint, up);
        return computeModelCoordinates(globe, modelview, centerPoint);
    }

    public ModelCoordinates computeModelCoordinates(Globe globe, Matrix modelTransform, Vec4 centerPoint)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (modelTransform == null)
        {
            String message = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (centerPoint == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Compute the center position, and center position transform.
        Position centerPos = globe.computePositionFromPoint(centerPoint);
        Matrix centerTransform = computeCenterTransform(globe, centerPos);
        Matrix centerTransformInv = centerTransform.getInverse();
        if (centerTransformInv == null)
        {
            String message = Logging.getMessage("generic.NoninvertibleMatrix");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // Compute the heading-pitch-zoom transform.
        Matrix hpzTransform = modelTransform.multiply(centerTransformInv);
        // Extract the heading, pitch, and zoom values from the transform.
        Angle heading = hpzTransform.getRotationZ();
        Angle pitch = hpzTransform.getRotationX();
        Vec4 zoomVec = hpzTransform.getTranslation();
        if (heading != null && pitch != null && zoomVec != null)
            return new BasicModelCoordinates(centerPos, heading, pitch.multiply(-1), zoomVec.getLength3());
        else
            return null;
    }

    // TODO: Adapt to flat world (OK)
    private static Matrix computeCenterTransform(Globe globe, Position center)
    {
        Matrix transform = Matrix.IDENTITY;
        if (globe != null && center != null)
        {
            // Flat sea level at zero on z, no need to move from globe center to surface.
            // No need to compute "spherical coordinates" for center, because Globe is computing the translation.
            // Center latitude and longitude. Use translation for lat/lon placement.
            Vec4 centerPoint = globe.computePointFromPosition(center);
            // Globe center point.
            Vec4 globeCenter = globe.getCenter();

            transform = transform.multiply(Matrix.fromTranslation(-centerPoint.x, -centerPoint.y, -centerPoint.z));
            transform = transform.multiply(Matrix.fromTranslation(-globeCenter.x, -globeCenter.y, -globeCenter.z));
        }
        return transform;
    }
}
