/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.wms.*;
import gov.nasa.worldwind.avlist.*;

/**
 * @author tag
 * @version $Id: NAIPCaliforniaWMS.java 4600 2008-03-04 21:48:28Z tgaskins $
 */
public class NAIPCaliforniaWMS extends WMSTiledImageLayer
{
    private static final String xmlState;

    static
    {
        xmlState = new String(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<restorableState>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.NumEmptyLevels\">0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.ImageFormat\">image/png</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.DataCacheNameKey\">Earth/NAIP/California</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.ServiceURLKey\">http://giifmap.cnr.berkeley.edu/cgi-bin/naip.wms?</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.Title\">NAIP California</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.NumLevels\">14</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.FormatSuffixKey\">.dds</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LevelZeroTileDelta.Latitude\">36.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LevelZeroTileDelta.Longitude\">36.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.DatasetNameKey\">naip2005C</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.TileHeightKey\">512</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.TileWidthKey\">512</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LayerNames\">naip2005C</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MinLatitude\">32.2006</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MaxLatitude\">42.0421</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MinLongitude\">-124.45</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MaxLongitude\">-113.222</stateObject>"
                + "<stateObject name=\"wms.UseTransparentTextures\">true</stateObject>"
                + "<stateObject name=\"wms.LayerName\">NAIP California (WMS)</stateObject>"
                + "<stateObject name=\"wms.LayerEnabled\">true</stateObject>"
                + "<stateObject name=\"wms.Version\">1.1.1</stateObject>"
                + "<stateObject name=\"wms.Crs\">&amp;srs=EPSG:4326</stateObject>"
                + "</restorableState>"
        );
    }

    public NAIPCaliforniaWMS()
    {
        super(xmlState);

        this.setValue(AVKey.URL_READ_TIMEOUT, 30000);
        this.setValue(AVKey.DISPLAY_NAME, "NAIP California (WMS)");
    }
}
