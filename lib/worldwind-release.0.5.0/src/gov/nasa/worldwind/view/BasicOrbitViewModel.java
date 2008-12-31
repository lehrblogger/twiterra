/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: BasicOrbitViewModel.java 4810 2008-03-26 00:50:55Z dcollins $
 */
public class BasicOrbitViewModel implements OrbitViewModel
{
    private static class BasicModelCoordinates implements OrbitViewModel.ModelCoordinates
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

    public BasicOrbitViewModel()
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

    private static Matrix computeCenterTransform(Globe globe, Position center)
    {
        Matrix transform = Matrix.IDENTITY;
        if (globe != null && center != null)
        {
            // The view eye position will be the same as the center position.
            // This is only the case without any zoom, heading, and pitch.
            Vec4 eyePoint = globe.computePointFromPosition(center);
            // The view forward direction will be colinear with the
            // geoid surface normal at the center position.
            Vec4 normal = globe.computeSurfaceNormalAtPoint(eyePoint);
            Vec4 lookAtPoint = eyePoint.subtract3(normal);
            // The up vector computed here can be approximate, because
            // fromLookAt() will create a correct up from a general direction.
            // The up vector, however cannot be zero and cannot be colinear with the
            // forward vector (forward=lookAtPoint-eyePoint).
            Vec4 up = approximateUpVector(globe, center);
            // Creates a viewing matrix looking from eyePoint towards lookAtPoint,
            // with the given up direction. The forward, right, and up vectors
            // contained in the matrix are guaranteed to be orthogonal. This means
            // that the Matrix's up may not be equivalent to the specified up vector
            // here (though it will point in the same general direction).
            // In this case, the forward direction would not be affected.
            transform = Matrix.fromLookAt(eyePoint, lookAtPoint, up);
        }
        return transform;
    }

    private static Vec4 approximateUpVector(Globe globe, Position center)
    {
        Matrix transform = Matrix.IDENTITY;
        if (globe != null && center != null)
        {
            Angle lat = center.getLatitude();
            Angle lon = center.getLongitude();
            // Center lat/lon is expressed as 3D rotation, which uses "Geocentric" latitude. In order for the
            // center to appear at the specified latitude, we must convert the incoming coordinates from
            // "Geodetic" coordinates to "Geocentric" coordinates.
            Angle latGeodetic = geodeticToGeocentric(globe, lat, center.getElevation());

            transform = transform.multiply(Matrix.fromRotationX(latGeodetic));
            transform = transform.multiply(Matrix.fromRotationY(lon.multiply(-1)));
        }
        return Vec4.UNIT_Y.transformBy4(transform.getInverse());
    }

    private static Angle geodeticToGeocentric(Globe globe, Angle latitude, double elevation)
    {
        if (globe != null && latitude != null)
        {
            Vec4 point = globe.computePointFromPosition(latitude, Angle.ZERO, elevation);
            PolarPoint polarPoint = PolarPoint.fromCartesian(point);
            return polarPoint.getLatitude();
        }
        else
        {
            return latitude;
        }
    }
}
