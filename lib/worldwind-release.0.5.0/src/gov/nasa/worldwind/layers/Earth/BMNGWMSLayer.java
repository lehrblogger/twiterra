/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.wms.*;

import java.util.*;

/**
 * Displays one of the twelve month of Blue Marble Next generation 2004 + Bathymetry via WMS.
 *
 * @author tag
 * @version $Id: BMNGWMSLayer.java 5283 2008-05-02 22:02:56Z dcollins $
 */
public class BMNGWMSLayer extends WMSTiledImageLayer
{
    private static final int DEFAULT_MONTH = 5;

    public BMNGWMSLayer()
    {
        this(DEFAULT_MONTH);
    }

    public BMNGWMSLayer(int month)
    {
        super(makeXmlState(month));

        // TODO: incorporate these into state string
        this.setValue(AVKey.URL_READ_TIMEOUT, 30000);
        this.setValue(AVKey.DISPLAY_NAME, String.format("BlueMarble (WMS) %02d/2004", month));
        this.setForceLevelZeroLoads(true);
        this.setRetainLevelZeroTiles(true);
        this.setUseMipMaps(true);
        this.setAvailableImageFormats(new String[] {"image/png", "image/dds"});
    }

    private static String makeXmlState(int month)
    {
        long expiryTime = new GregorianCalendar(2008, 3, 9).getTimeInMillis();
        String m = month + "";
        String mm = String.format("%02d", month);

        String xmlState = new String(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<restorableState>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.NumEmptyLevels\">0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.ImageFormat\">image/dds</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.DataCacheNameKey\">Earth/BMNGWMS/BMNG(Shaded + Bathymetry) Tiled - Version 1.1 - " + m + ".2004</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.ServiceURLKey\">http://www.nasa.network.com/wms?SERVICE=WMS&amp;</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.Title\">Blue Marble " + mm + "/2004</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.NumLevels\">5</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.FormatSuffixKey\">.dds</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LevelZeroTileDelta.Latitude\">36.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LevelZeroTileDelta.Longitude\">36.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.DatasetNameKey\">bmng2004" + mm + "</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.TileHeightKey\">512</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.TileWidthKey\">512</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LayerNames\">|bmng2004" + mm + "</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MinLatitude\">-90.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MaxLatitude\">90.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MinLongitude\">-180.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MaxLongitude\">180.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.ExpiryTime\">" + expiryTime + "</stateObject>"
                + "<stateObject name=\"wms.UseTransparentTextures\">false</stateObject>"
                + "<stateObject name=\"wms.LayerName\">BlueMarbleNG " + mm + "/2004</stateObject>"
                + "<stateObject name=\"wms.LayerEnabled\">true</stateObject>"
                + "</restorableState>"
        );
        return xmlState;
    }
}
