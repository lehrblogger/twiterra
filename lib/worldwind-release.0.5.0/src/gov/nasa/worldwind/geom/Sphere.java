/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

/**
 * Represents a sphere in three dimensional space.
 * <p/>
 * Instances of <code>Sphere</code> are immutable. </p>
 *
 * @author Tom Gaskins
 * @version $Id: Sphere.java 2471 2007-07-31 21:50:57Z tgaskins $
 */
public final class Sphere implements Extent, Renderable
{
    public final static Sphere UNIT_SPHERE = new Sphere(Vec4.ZERO, 1);

    private final Vec4 center;
    private final double radius;

    /**
     * Creates a sphere that completely contains a set of points.
     *
     * @param points the <code>Vec4</code>s to be enclosed by the new Sphere
     * @return a <code>Sphere</code> encompassing the given array of <code>Vec4</code>s
     * @throws IllegalArgumentException if <code>points</code> is null or empty
     */
    public static Sphere createBoundingSphere(Vec4 points[])
    {
        if (points == null)
        {
            String message = Logging.getMessage("nullValue.PointsArrayIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (points.length < 1)
        {
            String message = Logging.getMessage("Geom.Sphere.NoPointsSpecified");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Creates the sphere around the axis aligned bounding box of the input points.
        Vec4[] extrema = composeExtrema(points);
        Vec4 center = new Vec4(
            (extrema[0].x + extrema[1].x) / 2.0,
            (extrema[0].y + extrema[1].y) / 2.0,
            (extrema[0].z + extrema[1].z) / 2.0);
        double radius = extrema[0].distanceTo3(extrema[1]) / 2.0;

        return new Sphere(center, radius);
    }

    /**
     * Creates a new <code>Sphere</code> from a given center and radius. <code>radius</code> must be positive (that is,
     * greater than zero), and <code>center</code> may not be null.
     *
     * @param center the center of the new sphere
     * @param radius the radius of the new sphere
     * @throws IllegalArgumentException if <code>center</code> is null or if <code>radius</code> is non-positive
     */
    public Sphere(Vec4 center, double radius)
    {
        if (center == null)
        {
            String message = Logging.getMessage("nullValue.CenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (radius <= 0)
        {
            String message = Logging.getMessage("Geom.Sphere.RadiusIsZeroOrNegative", radius);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.center = center;
        this.radius = radius;
    }

    /**
     * Calculate the extrema of a given array of <code>Vec4</code>s. The resulting array is always of length 2, with the
     * first element containing the minimum extremum, and the second containing the maximum. The minimum extremum is
     * composed by taking the smallest x, y and z values from all the <code>Vec4</code>s in the array. These values are
     * not necessarily taken from the same <code>Vec4</code>. The maximum extrema is composed in the same fashion.
     *
     * @param points any array of <code>Vec4</code>s
     * @return a array with length of 2, comprising the most extreme values in the given array
     * @throws IllegalArgumentException if <code>points</code> is null
     */
    public static Vec4[] composeExtrema(Vec4 points[])
    {
        if (points == null)
        {
            String message = Logging.getMessage("nullValue.PointsArrayIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (points.length == 0)
            return null;

        double xmin = points[0].x;
        double ymin = points[0].y;
        double zmin = points[0].z;
        double xmax = xmin;
        double ymax = ymin;
        double zmax = zmin;

        for (int i = 1; i < points.length; i++)
        {
            double x = points[i].x;
            if (x > xmax)
            {
                xmax = x;
            }
            else if (x < xmin)
            {
                xmin = x;
            }

            double y = points[i].y;
            if (y > ymax)
            {
                ymax = y;
            }
            else if (y < ymin)
            {
                ymin = y;
            }

            double z = points[i].z;
            if (z > zmax)
            {
                zmax = z;
            }
            else if (z < zmin)
            {
                zmin = z;
            }
        }

        return new Vec4[] {new Vec4(xmin, ymin, zmin), new Vec4(xmax, ymax, zmax)};
    }

    /**
     * Obtains the radius of this <code>Sphere</code>. The radus is the distance from the center to the surface. If an
     * object's distance to this sphere's center is less than or equal to the radius, then that object is at least
     * partially within this <code>Sphere</code>.
     *
     * @return the radius of this sphere
     */
    public final double getRadius()
    {
        return this.radius;
    }

    /**
     * Obtains the diameter of this <code>Sphere</code>. The diameter is twice the radius.
     *
     * @return the diameter of this <code>Sphere</code>
     */
    public final double getDiameter()
    {
        return 2 * this.radius;
    }

    /**
     * Obtains the center of this <code>Sphere</code>.
     *
     * @return the <code>Vec4</code> situated at the center of this <code>Sphere</code>
     */
    public final Vec4 getCenter()
    {
        return this.center;
    }

    /**
     * Obtains the intersections of this sphere with a line. The returned array may be either null or of zero length if
     * no intersections are discovered. It does not contain null elements and will have a size of 2 at most. Tangential
     * intersections are marked as such. <code>line</code> is considered to have infinite length in both directions.
     *
     * @param line the <code>Line</code> with which to intersect this <code>Sphere</code>
     * @return an array containing all the intersections of this <code>Sphere</code> and <code>line</code>
     * @throws IllegalArgumentException if <code>line</code> is null
     */
    public final Intersection[] intersect(Line line)
    {
        if (line == null)
        {
            String message = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double a = line.getDirection().getLengthSquared3();
        double b = 2 * line.selfDot();
        double c = line.getOrigin().getLengthSquared3() - this.radius * this.radius;

        double discriminant = Sphere.discriminant(a, b, c);
        if (discriminant < 0)
            return null;

        double discriminantRoot = Math.sqrt(discriminant);
        if (discriminant == 0)
        {
            Vec4 p = line.getPointAt((-b - discriminantRoot) / (2 * a));
            return new Intersection[] {new Intersection(p, true)};
        }
        else // (discriminant > 0)
        {
            Vec4 near = line.getPointAt((-b - discriminantRoot) / (2 * a));
            Vec4 far = line.getPointAt((-b + discriminantRoot) / (2 * a));
            return new Intersection[] {new Intersection(near, false), new Intersection(far, false)};
        }
    }

    /**
     * Calculates a discriminant. A discriminant is useful to determine the number of roots to a quadratic equation. If
     * the discriminant is less than zero, there are no roots. If it equals zero, there is one root. If it is greater
     * than zero, there are two roots.
     *
     * @param a the coefficient of the second order pronumeral
     * @param b the coefficient of the first order pronumeral
     * @param c the constant parameter in the quadratic equation
     * @return the discriminant "b squared minus 4ac"
     */
    private static double discriminant(double a, double b, double c)
    {
        return b * b - 4 * a * c;
    }

    /**
     * tests for intersetion with a <code>Frustum</code>. This operation is commutative, so
     * <code>someSphere.intersects(frustum)</code> and <code>frustum.intersects(someSphere)</code> are equivalent.
     *
     * @param frustum the <code>Frustum</code> with which to test for intersection
     * @return true if either <code>frustum</code> or this <code>Sphere</code> wholly or partially contain the other,
     *         false otherwise.
     * @throws IllegalArgumentException if <code>frustum</code> is null
     */
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

    /**
     * Tests for intersection with a <code>Line</code>.
     *
     * @param line the <code>Line</code> with which to test for intersection
     * @return true if <code>line</code> intersects or makes a tangent with the surface of this <code>Sphere</code>
     * @throws IllegalArgumentException if <code>line</code> is null
     */
    public boolean intersects(Line line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        return line.distanceTo(this.center) <= this.radius;
    }

    /**
     * Tests for intersection with a <code>Plane</code>.
     *
     * @param plane the <code>Plane</code> with which to test for intersection
     * @return true if <code>plane</code> intersects or makes a tangent with the surface of this <code>Sphere</code>
     * @throws IllegalArgumentException if <code>plane</code> is null
     */
    public boolean intersects(Plane plane)
    {
        if (plane == null)
        {
            String msg = Logging.getMessage("nullValue.PlaneIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double dq1 = plane.dot(this.center);
        return dq1 <= this.radius;
    }

    /**
     * Causes this <code>Sphere</code> to render itself using the <code>DrawContext</code> provided. <code>dc</code> may
     * not be null.
     *
     * @param dc the <code>DrawContext</code> to be used
     * @throws IllegalArgumentException if <code>dc</code> is null
     */
    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        javax.media.opengl.GL gl = dc.getGL();

        gl.glPushAttrib(javax.media.opengl.GL.GL_TEXTURE_BIT | javax.media.opengl.GL.GL_ENABLE_BIT
            | javax.media.opengl.GL.GL_CURRENT_BIT);
        gl.glDisable(javax.media.opengl.GL.GL_TEXTURE_2D);
        gl.glColor3d(1, 1, 0);

        gl.glMatrixMode(javax.media.opengl.GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glTranslated(this.center.x, this.center.y, this.center.z);
        javax.media.opengl.glu.GLUquadric quadric = dc.getGLU().gluNewQuadric();
        dc.getGLU().gluQuadricDrawStyle(quadric, javax.media.opengl.glu.GLU.GLU_LINE);
        dc.getGLU().gluSphere(quadric, this.radius, 10, 10);
        gl.glPopMatrix();
        dc.getGLU().gluDeleteQuadric(quadric);

        gl.glPopAttrib();
    }

    @Override
    public String toString()
    {
        return "Sphere: center = " + this.center.toString() + " radius = " + Double.toString(this.radius);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final gov.nasa.worldwind.geom.Sphere sphere = (gov.nasa.worldwind.geom.Sphere) o;

        if (Double.compare(sphere.radius, radius) != 0)
            return false;
        //noinspection RedundantIfStatement
        if (!center.equals(sphere.center))
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        result = center.hashCode();
        temp = radius != +0.0d ? Double.doubleToLongBits(radius) : 0L;
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

//    public final boolean intersects(Line line)
//    {
//        if (line == null)
//        {
//            String message = WorldWind.retrieveErrMsg("nullValue.LineIsNull");
//            WorldWind.logger().logger(Level.SEVERE, message);
//            throw new IllegalArgumentException(message);
//        }
//
//        double a = line.getDirection().getLengthSquared();
//        double b = 2 * line.selfDot();
//        double c = line.getOrigin().selfDot() - this.radius * this.radius;
//
//        double discriminant = Sphere.discriminant(a, b, c);
//        if (discriminant < 0)
//        {
//            return false;
//        }
//
//        return true;
//
//    }
}
