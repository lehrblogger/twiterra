/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Moon;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.util.*;

import java.util.GregorianCalendar;

/**
 * Layer for Moon Clementin 30xx dataset.
 * @author Patrick Murris
 * @version $Id: Clementine30Layer.java 5183 2008-04-26 02:10:13Z patrickmurris $
 */
public class Clementine30Layer extends BasicTiledImageLayer
{
    public Clementine30Layer()
    {
        super(makeLevels());
        this.setForceLevelZeroLoads(true);
        this.setRetainLevelZeroTiles(true);
    }

    private static LevelSet makeLevels()
    {
        AVList params = new AVListImpl();

        params.setValue(AVKey.TILE_WIDTH, 512);
        params.setValue(AVKey.TILE_HEIGHT, 512);
        params.setValue(AVKey.DATA_CACHE_NAME, "Moon/Clementine30");
        params.setValue(AVKey.SERVICE, "http://worldwind28.arc.nasa.gov/TestWebApp/WebForm1.aspx");
        params.setValue(AVKey.DATASET_NAME, "clementine30xx");
        params.setValue(AVKey.FORMAT_SUFFIX, ".dds");
        params.setValue(AVKey.NUM_LEVELS, 6);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d)));
        params.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);

        return new LevelSet(params);
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Moon.Clementine30.Name");
    }
}