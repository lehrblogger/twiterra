package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.wms.*;
import gov.nasa.worldwind.util.*;

import java.util.*;

/**
 * @author tag
 * @version $Id: LandsatI3WMSLayer.java 5283 2008-05-02 22:02:56Z dcollins $
 */
public class LandsatI3WMSLayer extends WMSTiledImageLayer
{
    private static final String xmlState;

    static
    {
        long expiryTime = new GregorianCalendar(2008, 3, 11).getTimeInMillis();
        xmlState = new String(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<restorableState>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.NumEmptyLevels\">4</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.ImageFormat\">image/dds</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.DataCacheNameKey\">Earth/NASA LandSat I3 WMS</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.ServiceURLKey\">http://www.nasa.network.com/wms?SERVICE=WMS&amp;</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.Title\">i-cubed Landsat</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.NumLevels\">10</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.FormatSuffixKey\">.dds</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LevelZeroTileDelta.Latitude\">36.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LevelZeroTileDelta.Longitude\">36.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.DatasetNameKey\">|esat</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.TileHeightKey\">512</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.TileWidthKey\">512</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.LayerNames\">|esat</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MinLatitude\">-90.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MaxLatitude\">90.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MinLongitude\">-180.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avKey.Sector.MaxLongitude\">180.0</stateObject>"
                + "<stateObject name=\"gov.nasa.worldwind.avkey.ExpiryTime\">" + expiryTime + "</stateObject>"
                + "<stateObject name=\"wms.UseTransparentTextures\">true</stateObject>"
                + "<stateObject name=\"wms.LayerName\">i-cubed Landsat</stateObject>"
                + "<stateObject name=\"wms.LayerEnabled\">true</stateObject>"
                + "</restorableState>"
        );
    }

    public LandsatI3WMSLayer()
    {
        super(xmlState);

        // TODO: incorporate these into state string
        this.setValue(AVKey.URL_READ_TIMEOUT, 20000);
        this.setUseMipMaps(true);
        this.setUseTransparentTextures(true);
        this.setAvailableImageFormats(new String[] {"image/png", "image/dds"});
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Earth.LandsatI3Layer.Name");
    }
}
