package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.geom.*;

/**
 * @author tag
 * @version $Id: USGSTopoLowRes.java 5102 2008-04-21 05:41:05Z tgaskins $
 */
public class USGSTopoLowRes extends BasicTiledImageLayer
{
    public USGSTopoLowRes()
    {
        super(makeLevels());
        this.setMaxActiveAltitude(1e6d);
        this.setSplitScale(1.9);
        this.setValue(AVKey.MAP_SCALE, new Double(250e3));
        this.setAvailableImageFormats(new String[] {"image/jpg"});
    }

    private static LevelSet makeLevels()
    {
        AVList params = new AVListImpl();

        params.setValue(AVKey.TILE_WIDTH, 512);
        params.setValue(AVKey.TILE_HEIGHT, 512);
        params.setValue(AVKey.DATA_CACHE_NAME, "Earth/USGS Topographic Maps");
        params.setValue(AVKey.SERVICE, "http://worldwind25.arc.nasa.gov/tile/tile.aspx");
        params.setValue(AVKey.DATASET_NAME, "102dds");
        params.setValue(AVKey.FORMAT_SUFFIX, ".dds");
        params.setValue(AVKey.NUM_LEVELS, 5);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 2);
        params.setValue(AVKey.INACTIVE_LEVELS, "0,1,2,3");

        Angle levelZeroDelta = Angle.fromDegrees(3.2);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(levelZeroDelta, levelZeroDelta));

        params.setValue(AVKey.SECTOR, new Sector(Angle.fromDegrees(17.84), Angle.fromDegrees(71.55),
            Angle.fromDegrees(-168.67), Angle.fromDegrees(-65.15)));

        return new LevelSet(params);
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Earth.USGSTopographicMaps.Name");
    }
}
