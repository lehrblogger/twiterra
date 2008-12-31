/* 
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.rpf;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: RPFFrameTransform.java 4852 2008-03-28 19:14:52Z dcollins $
 */
public abstract class RPFFrameTransform
{
    RPFFrameTransform()
    {
    }

    public static RPFFrameTransform createFrameTransform(char zoneCode, String rpfDataType, double resolution)
    {
        if (!RPFZone.isZoneCode(zoneCode))
        {
            String message = Logging.getMessage("RPFZone.UnknownZoneCode", zoneCode);
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }
        if (rpfDataType == null || !RPFDataSeries.isRPFDataType(rpfDataType))
        {
            String message = Logging.getMessage("RPFDataSeries.UnkownDataType", rpfDataType);
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }
        if (resolution < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", resolution);
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        return newFrameTransform(zoneCode, rpfDataType, resolution);
    }

    private static RPFFrameTransform newFrameTransform(char zoneCode, String rpfDataType, double resolution)
    {
        boolean isNonpolarZone = !RPFZone.isPolarZone(zoneCode);
        if (isNonpolarZone)
        {
            return RPFNonpolarFrameTransform.createNonpolarFrameTransform(zoneCode, rpfDataType, resolution);
        }
        else
        {
            String message = String.format("Zone is not supported: %c", zoneCode);
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }
    }

    public abstract int getFrameNumber(int row, int column);

    public abstract int getMaximumFrameNumber();

    public abstract int getRows();

    public abstract int getColumns();

    public abstract LatLon computeFrameOrigin(int frameNumber);

    public abstract Sector computeFrameCoverage(int frameNumber);

    /* [Section 30.6, MIL-C-89038] */
    /* [Section A.3.6, MIL-PRF-89041A] */
    static int frameNumber(int row, int column, int columnFrames)
    {
        return column + row * columnFrames;
    }

    /* [Section 30.6, MIL-C-89038] */
    /* [Section A.3.6, MIL-PRF-89041A] */
    static int maxFrameNumber(int rowFrames, int columnFrames)
    {
        return (rowFrames * columnFrames) - 1;
    }

    /* [Section 30.6, MIL-C-89038] */
    /* [Section A.3.6, MIL-PRF-89041A] */
    static int frameRow(int frameNumber, int columnFrames)
    {
        return (int) (frameNumber / (double) columnFrames);
    }

    /* [Section 30.6, MIL-C-89038] */
    /* [Section A.3.6, MIL-PRF-89041A] */
    static int frameColumn(int frameNumber, int frameRow, int columnFrames)
    {
        return frameNumber - (frameRow * columnFrames);
    }
}
