/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.ViewStateIterator;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: AbstractViewStateIterator.java 4852 2008-03-28 19:14:52Z dcollins $
 */
public abstract class AbstractViewStateIterator implements ViewStateIterator
{
    private long startTime = -1;
    private long length;
    private boolean smoothed = true;
    private boolean hasNext = true;

    public AbstractViewStateIterator(long lengthMillis)
    {
        this(-1, lengthMillis);
    }

    public AbstractViewStateIterator(long startTimeMillis, long lengthMillis)
    {
        if (lengthMillis < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", lengthMillis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.startTime = startTimeMillis;
        this.length = lengthMillis;
    }

    public boolean isSmoothed()
    {
        return this.smoothed;
    }

    public void setSmoothed(boolean smoothed)
    {
        this.smoothed = smoothed;
    }

    public ViewStateIterator coalesceWith(View view, ViewStateIterator stateIterator)
    {
        return this;
    }

    public boolean hasNextState(View view)
    {
        return this.hasNext;
    }

    public void nextState(View view)
    {
        if (view  == null)
        {
            String message = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Get the current system time in milliseconds.
        long timeMillis = System.currentTimeMillis();
        // If no start time is specified, begin counting time on the first run.
        if (this.startTime < 0)
            this.startTime = timeMillis;

        // If the iterator has not started, exit for now.
        if (!hasStarted(timeMillis))
            return;

        // Stop iteration when we've reached the end time.
        if (!hasNextIteration(timeMillis))
            stopNextIteration();

        // Compute - and optionally smooth - the interpolant corresponding to the current time.
        double interpolant = computeInterpolant(timeMillis);
        if (this.smoothed)
            interpolant = smoothValue(interpolant);

        try
        {
            doNextState(interpolant, view);
            view.firePropertyChange(AVKey.VIEW, null, view);
        }
        catch (Exception e)
        {
            Logging.logger().log(java.util.logging.Level.SEVERE, "generic.ExceptionWhileRunningViewStateIterator", e);
            stopNextIteration();
        }
    }

    protected abstract void doNextState(double interpolant, View view);

    protected final void stopNextIteration()
    {
        this.hasNext = false;
    }

    private boolean hasStarted(long timeMillis)
    {
        return this.startTime < timeMillis;
    }

    private boolean hasNextIteration(long timeMillis)
    {
        // Iterator has more iterations when current-time < end-time.
        return timeMillis < (this.startTime + this.length);
    }

    private double computeInterpolant(long timeMillis)
    {
        // When no start time is specified, begin counting time on the first run.
        if (this.startTime < 0)
            this.startTime = timeMillis;
        // Exit when current time is before starting time.
        if (timeMillis < this.startTime)
            return 0;

        long elapsedTime = timeMillis - this.startTime;
        double unclampedInterpolant = ((double) elapsedTime) / ((double) this.length);
        return clampValue(unclampedInterpolant, 0, 1);
    }

    private static double clampValue(double value, double min, double max)
    {
        return value < min ? min : (value > max ? max : value);
    }

    private static double smoothValue(double value)
    {
        // Apply "hermite" smoothing.
        return value * value * (3.0 - 2.0 * value);
    }
}
