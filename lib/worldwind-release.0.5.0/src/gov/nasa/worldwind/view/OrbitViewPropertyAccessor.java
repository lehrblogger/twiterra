/* 
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.geom.*;

/**
 * @author dcollins
 * @version $Id: OrbitViewPropertyAccessor.java 4100 2008-01-08 02:49:54Z dcollins $
 */
public class OrbitViewPropertyAccessor
{
    private OrbitViewPropertyAccessor()
    {
    }

    public static interface AngleAccessor
    {
        Angle getAngle(OrbitView orbitView);

        boolean setAngle(OrbitView orbitView, Angle value);
    }

    public static interface DoubleAccessor
    {
        Double getDouble(OrbitView orbitView);

        boolean setDouble(OrbitView orbitView, Double value);
    }
    
    public static interface PositionAccessor
    {
        Position getPosition(OrbitView orbitView);

        boolean setPosition(OrbitView orbitView, Position value);
    }

    //public static interface QuaternionAccessor
    //{
    //    Quaternion getQuaternion(OrbitView orbitView);
    //
    //    boolean setQuaternion(OrbitView orbitView, Quaternion value);
    //}

    public static PositionAccessor createCenterPositionAccessor()
    {
        return new CenterPositionAccessor();
    }

    public static AngleAccessor createHeadingAccessor()
    {
        return new HeadingAccessor();
    }

    public static AngleAccessor createPitchAccessor()
    {
        return new PitchAccessor();
    }

    public static DoubleAccessor createZoomAccessor()
    {
        return new ZoomAccessor();
    }

    //public static RotationAccessor createRotationAccessor()
    //{
    //    return new RotationAccessor();
    //}

    // ============== Implementation ======================= //
    // ============== Implementation ======================= //
    // ============== Implementation ======================= //

    private static class CenterPositionAccessor implements PositionAccessor
    {
        public Position getPosition(OrbitView orbitView)
        {
            if (orbitView == null)
                return null;

            return orbitView.getCenterPosition();
        }

        public boolean setPosition(OrbitView orbitView, Position value)
        {
             //noinspection SimplifiableIfStatement
            if (orbitView == null || value == null)
                return false;

            try
            {
                orbitView.setCenterPosition(value);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    private static class HeadingAccessor implements AngleAccessor
    {
        public final Angle getAngle(OrbitView orbitView)
        {
            if (orbitView == null)
                return null;

            return orbitView.getHeading();
        }

        public final boolean setAngle(OrbitView orbitView, Angle value)
        {
            //noinspection SimplifiableIfStatement
            if (orbitView == null || value == null)
                return false;

            try
            {
                orbitView.setHeading(value);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    private static class PitchAccessor implements AngleAccessor
    {
        public final Angle getAngle(OrbitView orbitView)
        {
            if (orbitView == null)
                return null;

            return orbitView.getPitch();
        }

        public final boolean setAngle(OrbitView orbitView, Angle value)
        {
            //noinspection SimplifiableIfStatement
            if (orbitView == null || value == null)
                return false;

            try
            {
                orbitView.setPitch(value);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    private static class ZoomAccessor implements DoubleAccessor
    {
        public final Double getDouble(OrbitView orbitView)
        {
            if (orbitView == null)
                return null;

            return orbitView.getZoom();
        }

        public final boolean setDouble(OrbitView orbitView, Double value)
        {
            //noinspection SimplifiableIfStatement
            if (orbitView == null || value == null)
                return false;

            try
            {
                orbitView.setZoom(value);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    //private static class RotationAccessor implements QuaternionAccessor
    //{
    //    public final Quaternion getQuaternion(OrbitView orbitView)
    //    {
    //        if (orbitView == null)
    //            return null;
    //
    //        return orbitView.getRotation();
    //    }
    //
    //    public final boolean setQuaternion(OrbitView orbitView, Quaternion value)
    //    {
    //        if (orbitView == null || value == null)
    //            return false;
    //
    //        orbitView.setRotation(value);
    //        return true;
    //    }
    //}
}
