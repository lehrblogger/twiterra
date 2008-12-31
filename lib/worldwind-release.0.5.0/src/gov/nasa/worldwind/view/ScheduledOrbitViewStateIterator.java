/* 
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: ScheduledOrbitViewStateIterator.java 4100 2008-01-08 02:49:54Z dcollins $
 */
public class ScheduledOrbitViewStateIterator extends BasicOrbitViewStateIterator
{
    private final int maxSmoothing;

    protected ScheduledOrbitViewStateIterator(long lengthMillis, OrbitViewAnimator animator, boolean doSmoothing)
    {
        this(new ScheduledOrbitViewInterpolator(lengthMillis), animator, doSmoothing);
    }

    protected ScheduledOrbitViewStateIterator(ScheduledOrbitViewInterpolator interpolator, OrbitViewAnimator animator,
        boolean doSmoothing)
    {
        super(false, interpolator, animator);

        if (interpolator == null)
        {
            String message = Logging.getMessage("nullValue.OrbitViewInterpolatorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (animator == null)
        {
            String message = Logging.getMessage("nullValue.OrbitViewAnimatorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.maxSmoothing = maxSmoothingFromFlag(doSmoothing);
    }

    public final boolean isSmoothing()
    {
        return this.maxSmoothing != 0;
    }

    public void doNextState(double interpolant, OrbitView orbitView)
    {
        if (orbitView == null)
        {
            String message = Logging.getMessage("nullValue.OrbitViewIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double smoothedInterpolant = interpolantSmoothed(interpolant, this.maxSmoothing);
        super.doNextState(smoothedInterpolant, orbitView);
    }

    private static double interpolantSmoothed(double interpolant, int smoothingIterations)
    {
        // Apply iterative hermite smoothing.
        double smoothed = interpolant;
        for (int i = 0; i < smoothingIterations; i++)
        {
            smoothed = smoothed * smoothed * (3.0 - 2.0 * smoothed);
        }
        return smoothed;
    }

    private static int maxSmoothingFromFlag(boolean doSmoothing)
    {
        if (doSmoothing)
            return 1;
        else
            return 0;
    }    

    // ============== Factory Functions ======================= //
    // ============== Factory Functions ======================= //
    // ============== Factory Functions ======================= //

    public static ScheduledOrbitViewStateIterator createCenterIterator(
        Position begin, Position end)
    {
        if (begin == null || end == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        // TODO: length-scaling factory function
        final long DEFAULT_LENGTH_MILLIS = 4000;
        boolean smoothed = true;
        return createCenterIterator(
            begin, end,
            DEFAULT_LENGTH_MILLIS, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createCenterIterator(
        Position begin, Position end,
        long lengthMillis, boolean smoothed)
    {
        if (begin == null || end == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.PositionAccessor propertyAccessor
            = OrbitViewPropertyAccessor.createCenterPositionAccessor();
        OrbitViewAnimator animator = new BasicOrbitViewAnimator.PositionAnimator(begin, end, propertyAccessor);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createHeadingIterator(
        Angle begin, Angle end)
    {
        if (begin == null || end == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long lengthMillis = getScaledLengthMillis(
            begin, end, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);
        boolean smoothed = true;
        return createHeadingIterator(
            begin,
            end,
            lengthMillis, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createHeadingIterator(
        Angle begin, Angle end,
        long lengthMillis, boolean smoothed)
    {
        if (begin == null || end == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.AngleAccessor propertyAccessor = OrbitViewPropertyAccessor.createHeadingAccessor();
        OrbitViewAnimator animator = new BasicOrbitViewAnimator.AngleAnimator(begin, end, propertyAccessor);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createPitchIterator(
        Angle begin, Angle end)
    {
        if (begin == null || end == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 1500;
        long lengthMillis = getScaledLengthMillis(
            begin, end, Angle.POS90,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);
        boolean smoothed = true;
        return createPitchIterator(
            begin,
            end,
            lengthMillis, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createPitchIterator(
        Angle begin, Angle end,
        long lengthMillis, boolean smoothed)
    {
        if (begin == null || end == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.AngleAccessor propertyAccessor = OrbitViewPropertyAccessor.createPitchAccessor();
        OrbitViewAnimator animator = new BasicOrbitViewAnimator.AngleAnimator(begin, end, propertyAccessor);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createZoomIterator(
        double begin, double end)
    {
        // TODO: length-scaling factory function
        final long DEFAULT_LENGTH_MILLIS = 4000;
        boolean smoothed = true;
        return createZoomIterator(
            begin, end,
            DEFAULT_LENGTH_MILLIS, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createZoomIterator(
        double begin, double end,
        long lengthMillis, boolean smoothed)
    {
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.DoubleAccessor propertyAccessor = OrbitViewPropertyAccessor.createZoomAccessor();
        OrbitViewAnimator animator = new BasicOrbitViewAnimator.DoubleAnimator(begin, end, propertyAccessor);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createCenterHeadingPitchIterator(
        Position beginCenter, Position endCenter,
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch,
        long lengthMillis, boolean smoothed)
    {
        if (beginCenter == null || endCenter == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (beginHeading == null || endHeading == null || beginPitch == null || endPitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.PositionAccessor cPropAccessor =
                OrbitViewPropertyAccessor.createCenterPositionAccessor();
        OrbitViewPropertyAccessor.AngleAccessor hPropAccessor = OrbitViewPropertyAccessor.createHeadingAccessor();
        OrbitViewPropertyAccessor.AngleAccessor pPropAccessor = OrbitViewPropertyAccessor.createPitchAccessor();

        OrbitViewAnimator cAnimator = new BasicOrbitViewAnimator.PositionAnimator(beginCenter, endCenter, cPropAccessor);
        OrbitViewAnimator hAnimator = new BasicOrbitViewAnimator.AngleAnimator(beginHeading, endHeading, hPropAccessor);
        OrbitViewAnimator pAnimator = new BasicOrbitViewAnimator.AngleAnimator(beginPitch, endPitch, pPropAccessor);

        OrbitViewAnimator animator = new BasicOrbitViewAnimator.CompoundAnimator(cAnimator, hAnimator, pAnimator);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createCenterZoomIterator(
        Position beginCenter, Position endCenter,
        double beginZoom, double endZoom,
        long lengthMillis, boolean smoothed)
    {
        if (beginCenter == null || endCenter == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.PositionAccessor cPropAccessor =
                OrbitViewPropertyAccessor.createCenterPositionAccessor();
        OrbitViewPropertyAccessor.DoubleAccessor zPropAccessor = OrbitViewPropertyAccessor.createZoomAccessor();

        OrbitViewAnimator cAnimator = new BasicOrbitViewAnimator.PositionAnimator(beginCenter, endCenter, cPropAccessor);
        OrbitViewAnimator zAnimator = new BasicOrbitViewAnimator.DoubleAnimator(beginZoom, endZoom, zPropAccessor);

        OrbitViewAnimator animator = new BasicOrbitViewAnimator.CompoundAnimator(cAnimator, zAnimator);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createCenterHeadingPitchZoomIterator(
        Position beginCenter, Position endCenter,
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch,
        double beginZoom, double endZoom,
        long lengthMillis, boolean smoothed)
    {
        if (beginCenter == null || endCenter == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (beginHeading == null || endHeading == null || beginPitch == null || endPitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.PositionAccessor cPropAccessor =
                OrbitViewPropertyAccessor.createCenterPositionAccessor();
        OrbitViewPropertyAccessor.AngleAccessor hPropAccessor = OrbitViewPropertyAccessor.createHeadingAccessor();
        OrbitViewPropertyAccessor.AngleAccessor pPropAccessor = OrbitViewPropertyAccessor.createPitchAccessor();
        OrbitViewPropertyAccessor.DoubleAccessor zPropAccessor = OrbitViewPropertyAccessor.createZoomAccessor();

        OrbitViewAnimator cAnimator = new BasicOrbitViewAnimator.PositionAnimator(beginCenter, endCenter, cPropAccessor);
        OrbitViewAnimator hAnimator = new BasicOrbitViewAnimator.AngleAnimator(beginHeading, endHeading, hPropAccessor);
        OrbitViewAnimator pAnimator = new BasicOrbitViewAnimator.AngleAnimator(beginPitch, endPitch, pPropAccessor);
        OrbitViewAnimator zAnimator = new BasicOrbitViewAnimator.DoubleAnimator(beginZoom, endZoom, zPropAccessor);

        OrbitViewAnimator animator = new BasicOrbitViewAnimator.CompoundAnimator(
            cAnimator, hAnimator, pAnimator, zAnimator);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createHeadingPitchIterator(
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch)
    {
        if (beginHeading == null || endHeading == null || beginPitch == null || endPitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long headingLengthMillis = getScaledLengthMillis(
            beginHeading, endHeading, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);
        long pitchLengthMillis = getScaledLengthMillis(
            beginPitch, endPitch, Angle.POS90,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS / 2L);
        long lengthMillis = headingLengthMillis + pitchLengthMillis;
        boolean smoothed = true;
        return createHeadingPitchIterator(
            beginHeading, endHeading,
            beginPitch, endPitch,
            lengthMillis, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createHeadingPitchIterator(
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch,
        long lengthMillis, boolean smoothed)
    {
        if (beginHeading == null || endHeading == null || beginPitch == null || endPitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.AngleAccessor hPropAccessor = OrbitViewPropertyAccessor.createHeadingAccessor();
        OrbitViewPropertyAccessor.AngleAccessor pPropAccessor = OrbitViewPropertyAccessor.createPitchAccessor();

        OrbitViewAnimator hAnimator = new BasicOrbitViewAnimator.AngleAnimator(beginHeading, endHeading, hPropAccessor);
        OrbitViewAnimator pAnimator = new BasicOrbitViewAnimator.AngleAnimator(beginPitch, endPitch, pPropAccessor);

        OrbitViewAnimator animator = new BasicOrbitViewAnimator.CompoundAnimator(hAnimator, pAnimator);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    public static ScheduledOrbitViewStateIterator createHeadingPitchZoomIterator(
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch,
        double beginZoom, double endZoom,
        long lengthMillis, boolean smoothed)
    {
        if (beginHeading == null || endHeading == null || beginPitch == null || endPitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewPropertyAccessor.AngleAccessor hPropAccessor = OrbitViewPropertyAccessor.createHeadingAccessor();
        OrbitViewPropertyAccessor.AngleAccessor pPropAccessor = OrbitViewPropertyAccessor.createPitchAccessor();
        OrbitViewPropertyAccessor.DoubleAccessor zPropAccessor = OrbitViewPropertyAccessor.createZoomAccessor();

        OrbitViewAnimator hAnimator = new BasicOrbitViewAnimator.AngleAnimator(beginHeading, endHeading, hPropAccessor);
        OrbitViewAnimator pAnimator = new BasicOrbitViewAnimator.AngleAnimator(beginPitch, endPitch, pPropAccessor);
        OrbitViewAnimator zAnimator = new BasicOrbitViewAnimator.DoubleAnimator(beginZoom, endZoom, zPropAccessor);

        OrbitViewAnimator animator = new BasicOrbitViewAnimator.CompoundAnimator(hAnimator, pAnimator, zAnimator);
        return new ScheduledOrbitViewStateIterator(lengthMillis, animator, smoothed);
    }

    private static long getScaledLengthMillis(
        Angle begin, Angle end, Angle max,
        long minLengthMillis, long maxLengthMillis)
    {
        Angle angularDistance = begin.angularDistanceTo(end);
        double scaleFactor = angularRatio(angularDistance, max);
        return (long) mixDouble(scaleFactor, minLengthMillis, maxLengthMillis);
    }

    private static double angularRatio(Angle x, Angle y)
    {
        if (x == null || y == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double unclampedRatio = x.divide(y);
        return clampDouble(unclampedRatio, 0, 1);
    }

    private static double clampDouble(double value, double min, double max)
    {
        return value < min ? min : (value > max ? max : value);
    }

    private static double mixDouble(double amount, double value1, double value2)
    {
        if (amount < 0)
            return value1;
        else if (amount > 1)
            return value2;
        return value1 * (1.0 - amount) + value2 * amount;
    }
}
