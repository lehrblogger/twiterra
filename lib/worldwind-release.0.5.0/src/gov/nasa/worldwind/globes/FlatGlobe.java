/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.globes;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

/**
 * Flat globe.
 * See Flat World Notes inline comments for difference from EllipsoidalGlobe.
 *
 * @author Patrick Murris - based on EllipsoidalGlobe
 * @version $Id: FlatGlobe.java 5229 2008-04-30 18:16:29Z patrickmurris $
 */
public class FlatGlobe extends WWObjectImpl implements Globe
{
    public final static String PROJECTION_LAT_LON = "gov.nasa.worldwind.globes.projectionLatLon";
    public final static String PROJECTION_MERCATOR = "gov.nasa.worldwind.globes.projectionMercator";
    public final static String PROJECTION_SINUSOIDAL = "gov.nasa.worldwind.globes.projectionSinusoidal";
    public final static String PROJECTION_MODIFIED_SINUSOIDAL =
        "gov.nasa.worldwind.globes.projectionModifiedSinusoidal";

    private final double equatorialRadius;
    private final double polarRadius;
    private final double es;
    private final Vec4 center;
    private Tessellator tessellator;

    private final ElevationModel elevationModel;
    private String projection = PROJECTION_MERCATOR;

    public FlatGlobe(double equatorialRadius, double polarRadius, double es, ElevationModel em)
    {
        if (em == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationModelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;
        this.es = es; // assume it's consistent with the two radii
        this.center = Vec4.ZERO;
        this.elevationModel = em;
        this.tessellator = (Tessellator) WorldWind.createConfigurationComponent(AVKey.TESSELLATOR_CLASS_NAME);
    }

    private static class StateKey
    {
        private final Tessellator tessellator;
        private final String projection;

        public StateKey(FlatGlobe globe)
        {
            this.tessellator = globe.tessellator;
            this.projection = globe.projection;
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StateKey stateKey = (StateKey) o;

            if (projection != null ? !projection.equals(stateKey.projection) : stateKey.projection != null)
                return false;
            //noinspection RedundantIfStatement
            if (tessellator != null ? !tessellator.equals(stateKey.tessellator) : stateKey.tessellator != null)
                return false;

            return true;
        }

        public int hashCode()
        {
            int result;
            result = (tessellator != null ? tessellator.hashCode() : 0);
            result = 31 * result + (projection != null ? projection.hashCode() : 0);
            return result;
        }
    }

    public Object getStateKey()
    {
        return new StateKey(this);
    }

    public Tessellator getTessellator()
    {
        return tessellator;
    }

    public void setTessellator(Tessellator tessellator)
    {
        this.tessellator = tessellator;
    }

    // TODO: return the flat globe plane extent radius here?
    public final double getRadius()
    {
        return this.equatorialRadius;
    }

    public final double getEquatorialRadius()
    {
        return this.equatorialRadius;
    }

    public final double getPolarRadius()
    {
        return this.polarRadius;
    }

    public double getMaximumRadius()
    {
        return this.equatorialRadius;
    }

    // TODO: Find a more accurate workaround then getMaximumRadius()
    public double getRadiusAt(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        return getMaximumRadius();
        //return this.computePointFromPosition(latitude, longitude, 0d).getLength3();
    }

    // TODO: Find a more accurate workaround then getMaximumRadius()
    public double getRadiusAt(LatLon latLon)
    {
        if (latLon == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        return getMaximumRadius();
        //return this.computePointFromPosition(latLon.getLatitude(), latLon.getLongitude(), 0d).getLength3();
    }

    public double getEccentricitySquared()
    {
        return this.es;
    }

    public final double getDiameter()
    {
        return this.equatorialRadius * 2;
    }

    public final Vec4 getCenter()
    {
        return this.center;
    }

    public double getMaxElevation()
    {
        return this.elevationModel.getMaxElevation();
    }

    public double getMinElevation()
    {
        return this.elevationModel.getMinElevation();
    }

    public double[] getMinAndMaxElevations(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.elevationModel.getMinAndMaxElevations(sector);
    }

    public final Extent getExtent()
    {
        return this;
    }

    public void setProjection(String projection)
    {
        if (projection == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.projection.equals(projection))
            return;

        this.projection = projection;
        this.tessellator = null;
    }

    public String getProjection()
    {
        return this.projection;
    }

    public boolean intersects(Frustum frustum)
    {
        if (frustum == null)
        {
            String message = Logging.getMessage("nullValue.FrustumIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return frustum.intersects(this);
    }

    public Intersection[] intersect(Line line)
    {
        return this.internalIntersect(line, this.equatorialRadius);
    }

    public Intersection[] intersect(Line line, double altitude)
    {
        return this.internalIntersect(line, this.equatorialRadius + altitude);
    }

    // Flat World Note: plane/line intersection point (OK)
    // Flat World Note: extract altitude from equRadius by subtracting this.equatorialRadius (OK)
    private Intersection[] internalIntersect(Line line, double equRadius)
    {
        if (line == null)
        {
            String message = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        // Intersection with world plane
        Plane plane = new Plane(0, 0, 1, -(equRadius - this.equatorialRadius));   // Flat globe plane
        Vec4 p = plane.intersect(line);
        if (p == null)
            return null;
        // Check if we are in the world boundaries
        Position pos = this.computePositionFromPoint(p);
        if (pos == null)
            return null;
        if (pos.getLatitude().degrees < -90 || pos.getLatitude().degrees > 90 ||
            pos.getLongitude().degrees < -180 || pos.getLongitude().degrees > 180)
            return null;

        return new Intersection[] {new Intersection(p, false)};
    }

    // Flat World Note: plane/line intersection test (OK)
    public boolean intersects(Line line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.intersect(line) != null;
    }

    // Flat World Note: plane/plane intersection test (OK)
    public boolean intersects(Plane plane)
    {
        if (plane == null)
        {
            String msg = Logging.getMessage("nullValue.PlaneIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return !plane.getNormal().equals(Vec4.UNIT_Z);

    }

    // Flat World Note: return constant (OK)
    public Vec4 computeSurfaceNormalAtPoint(Vec4 p)
    {
        if (p == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return Vec4.UNIT_Z;
    }

    public final ElevationModel getElevationModel()
    {
        return this.elevationModel;
    }

    public Double getBestElevation(Angle latitude, Angle longitude)
    {
        return this.elevationModel.getBestElevation(latitude, longitude);
    }

    // Flat World Note: return zero if outside the lat/lon normal boundaries (OK)
    public final Double getElevationAtResolution(Angle latitude, Angle longitude, double resolution)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (latitude.degrees < -90 || latitude.degrees > 90 || longitude.degrees < -180 || longitude.degrees > 180)
            return 0d;

        int target = this.elevationModel.getTargetResolution(this, resolution);
        return this.elevationModel.getElevationAtResolution(latitude, longitude, target);
    }

    // Flat World Note: return zero if outside the lat/lon normal boundaries (OK)
    public final double getElevation(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (latitude.degrees < -90 || latitude.degrees > 90 || longitude.degrees < -180 || longitude.degrees > 180)
            return 0d;

        return this.elevationModel != null ? this.elevationModel.getElevation(latitude, longitude) : 0;
    }

    public final Vec4 computePointFromPosition(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.geodeticToCartesian(position.getLatitude(), position.getLongitude(), position.getElevation());
    }

    public final Vec4 computePointFromPosition(Angle latitude, Angle longitude, double metersElevation)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.geodeticToCartesian(latitude, longitude, metersElevation);
    }

    public final Position computePositionFromPoint(Vec4 point)
    {
        if (point == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.cartesianToGeodetic(point);
    }

    public final Position getIntersectionPosition(Line line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Intersection[] intersections = this.intersect(line);
        if (intersections == null)
            return null;

        return this.computePositionFromPoint(intersections[0].getIntersectionPoint());
    }

    // The code below maps latitude / longitude position to a flat world Cartesian coordinates.
    // The world plane is located at the origin and has UNIT-Z as normal.
    // The Y axis points to the north pole. The Z axis points up. The X axis completes a right-handed
    // coordinate system, and points east. Latitude and longitude zero are at the origine on y and x respectively.
    // Sea level is at z = zero.
    // Flat World Note: Implement flat projections (OK)
    private Vec4 geodeticToCartesian(Angle latitude, Angle longitude, double metersElevation)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Vec4 cart = null;
        if (this.projection.equals(PROJECTION_LAT_LON))
        {
            // Lat/Lon projection - plate carree
            cart = new Vec4(this.equatorialRadius * longitude.radians,
                this.equatorialRadius * latitude.radians,
                metersElevation);
        }
        else if (this.projection.equals(PROJECTION_MERCATOR))
        {
            // Mercator projection
            if (latitude.degrees > 75) latitude = Angle.fromDegrees(75);
            if (latitude.degrees < -75) latitude = Angle.fromDegrees(-75);
            cart = new Vec4(this.equatorialRadius * longitude.radians,
                this.equatorialRadius * Math.log(Math.tan(Math.PI / 4 + latitude.radians / 2)),
                metersElevation);
        }
        else if (this.projection.equals(PROJECTION_SINUSOIDAL))
        {
            // Sinusoidal projection
            cart = new Vec4(this.equatorialRadius * longitude.radians * latitude.cos(),
                this.equatorialRadius * latitude.radians,
                metersElevation);
        }
        else if (this.projection.equals(PROJECTION_MODIFIED_SINUSOIDAL))
        {
            // Modified Sinusoidal projection
            cart = new Vec4(this.equatorialRadius * longitude.radians * Math.pow(latitude.cos(), .3),
                this.equatorialRadius * latitude.radians,
                metersElevation);
        }
        return cart;
    }

    // Flat World Note: Implement flat projections (OK)
    private Position cartesianToGeodetic(Vec4 cart)
    {
        if (cart == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position pos = null;
        if (this.projection.equals(PROJECTION_LAT_LON))
        {
            // Lat/Lon projection - plate carree
            pos = Position.fromRadians(
                cart.y / this.equatorialRadius,
                cart.x / this.equatorialRadius,
                cart.z);
        }
        else if (this.projection.equals(PROJECTION_MERCATOR))
        {
            // Mercator projection
            pos = Position.fromRadians(
                Math.atan(Math.sinh(cart.y / this.equatorialRadius)),
                cart.x / this.equatorialRadius,
                cart.z);
        }
        else if (this.projection.equals(PROJECTION_SINUSOIDAL))
        {
            // Sinusoidal projection
            pos = Position.fromRadians(
                cart.y / this.equatorialRadius,
                cart.x / this.equatorialRadius / Angle.fromRadians(cart.y / this.equatorialRadius).cos(),
                cart.z);
        }
        else if (this.projection.equals(PROJECTION_MODIFIED_SINUSOIDAL))
        {
            // Modified Sinusoidal projection
            pos = Position.fromRadians(
                cart.y / this.equatorialRadius,
                cart.x / this.equatorialRadius / Math.pow(Angle.fromRadians(cart.y / this.equatorialRadius).cos(), .3),
                cart.z);
        }
        return pos;
    }


    /**
     * Returns a cylinder that minimally surrounds the sector at a specified vertical exaggeration.
     *
     * @param verticalExaggeration the vertical exaggeration to apply to the globe's elevations when computing the
     *                             cylinder.
     * @param sector               the sector to return the bounding cylinder for.
     * @return The minimal bounding cylinder in Cartesian coordinates.
     * @throws IllegalArgumentException if <code>globe</code> or <code>sector</code> is null
     */
    // Flat World Note: Adapt to flat world tiles (OK)
    public Cylinder computeBoundingCylinder(double verticalExaggeration, Sector sector)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Compute the center points of the bounding cylinder's top and bottom planes.
        LatLon center = sector.getCentroid();
        double[] minAndMaxElevations = this.getMinAndMaxElevations(sector);
        double minHeight = minAndMaxElevations[0] * verticalExaggeration;
        double maxHeight = minAndMaxElevations[1] * verticalExaggeration;
        Vec4 centroidTop = this.computePointFromPosition(center.getLatitude(), center.getLongitude(), maxHeight);
        Vec4 centroidBot = this.computePointFromPosition(center.getLatitude(), center.getLongitude(), minHeight);

        /* // Compute radius of circumscribing circle around general quadrilateral.
        Vec4 northwest = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMinLongitude(), maxHeight);
        Vec4 southeast = this.computePointFromPosition(sector.getMinLatitude(), sector.getMaxLongitude(), maxHeight);
        Vec4 southwest = this.computePointFromPosition(sector.getMinLatitude(), sector.getMinLongitude(), maxHeight);
        Vec4 northeast = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMaxLongitude(), maxHeight);
        double a = southwest.distanceTo3(southeast);
        double b = southeast.distanceTo3(northeast);
        double c = northeast.distanceTo3(northwest);
        double d = northwest.distanceTo3(southwest);
        double s = 0.5 * (a + b + c + d);
        double area = Math.sqrt((s - a) * (s - b) * (s - c) * (s - d));
        double radius = Math.sqrt((a * b + c * d) * (a * d + b * c) * (a * c + b * d)) / (4d * area);
        */

        // Compute radius of circumscribing circle using largest distance from center to corners.
        Vec4 northwest = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMinLongitude(), maxHeight);
        Vec4 southeast = this.computePointFromPosition(sector.getMinLatitude(), sector.getMaxLongitude(), maxHeight);
        Vec4 southwest = this.computePointFromPosition(sector.getMinLatitude(), sector.getMinLongitude(), maxHeight);
        Vec4 northeast = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMaxLongitude(), maxHeight);
        double a = southwest.distanceTo3(centroidBot);
        double b = southeast.distanceTo3(centroidBot);
        double c = northeast.distanceTo3(centroidBot);
        double d = northwest.distanceTo3(centroidBot);
        double radius = Math.max(Math.max(a, b), Math.max(c, d));

        return new Cylinder(centroidBot, centroidTop, radius);
    }

    public SectorGeometryList tessellate(DrawContext dc)
    {
        if (this.tessellator == null)
        {
            this.tessellator = (Tessellator) WorldWind.createConfigurationComponent(AVKey.TESSELLATOR_CLASS_NAME);
        }

        return this.tessellator.tessellate(dc);
    }
}