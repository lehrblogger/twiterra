/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: EyePositionIterator.java 4852 2008-03-28 19:14:52Z dcollins $
 */
public class EyePositionIterator extends AbstractViewStateIterator
{
    private final Position begin;
    private final Position end;

    public EyePositionIterator(long lengthMillis, Position beginEyePosition, Position endEyePosition)
    {
        super(lengthMillis);
        if (beginEyePosition == null || endEyePosition == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.begin = beginEyePosition;
        this.end = endEyePosition;
    }

    public EyePositionIterator(long startTimeMillis, long lengthMillis, Position beginEyePosition, Position endEyePosition)
    {
        super(startTimeMillis, lengthMillis);
        if (beginEyePosition == null || endEyePosition == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.begin = beginEyePosition;
        this.end = endEyePosition;
    }

    public final Position getBeginEyePosition()
    {
        return this.begin;
    }

    public final Position getEndEyePosition()
    {
        return this.end;
    }

    protected void doNextState(double interpolant, View view)
    {
        if (interpolant < 0 || interpolant > 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", interpolant);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);    
        }
        if (view  == null)
        {
            String message = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position newPosition = Position.interpolate(interpolant, this.begin, this.end);
        view.setEyePosition(newPosition);
    }
}
