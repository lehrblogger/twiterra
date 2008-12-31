/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

/**
 * A collection of useful math methods, all static.
 *
 * @author tag
 * @version $Id: WWMath.java 5103 2008-04-21 05:41:15Z tgaskins $
 */
public class WWMath
{
    /**
     * Convenience method for testing is a value is a power of two.
     *
     * @param value the value to test for power of 2
     * @return true if power of 2, else false
     */
    public static boolean isPowerOfTwo(int value)
    {
        return (value == nearestPowerOfTwo(value));
    }

    /**
     * Returns a resolution value that is the nearest power of 2 greater than or equal to the given
     * value.
     *
     * @param reference the reference value. The power of 2 returned is greater than or equal to this value.
     * @return power of two resolution
     */
    public static int nearestPowerOfTwo(int reference)
    {
        int power = (int) Math.ceil(Math.log(reference) / Math.log(2d));
        return (int) Math.pow(2d, power);
    }

    /**
     * Convenience method to compute the log-2 of a value.
     *
     * @param value the value to take the log of.
     * @return the log base 2 of the specified value.
     */
    public static double logBase2(double value)
    {
        return Math.log(value) / Math.log(2d);
    }
}
