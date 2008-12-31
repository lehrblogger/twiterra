/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.util.*;

import java.net.*;

/**
 * @author tag
 * @version $Id: NAIPCalifornia.java 5215 2008-04-30 04:37:46Z tgaskins $
 */
public class NAIPCalifornia extends BasicTiledImageLayer
{
    public NAIPCalifornia()
    {
        super(makeLevels(new URLBuilder()));
        this.setUseTransparentTextures(true);
        this.setValue(AVKey.URL_CONNECT_TIMEOUT, 20000);
        this.setValue(AVKey.URL_READ_TIMEOUT, 20000);
        this.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 30000);
        this.setAvailableImageFormats(new String[] {"image/png"});
    }

    private static LevelSet makeLevels(URLBuilder urlBuilder)
    {
        AVList params = new AVListImpl();

        params.setValue(AVKey.TILE_WIDTH, 512);
        params.setValue(AVKey.TILE_HEIGHT, 512);
        params.setValue(AVKey.DATA_CACHE_NAME, "Earth/NAIP/California");
        params.setValue(AVKey.SERVICE, "http://giifmap.cnr.berkeley.edu/cgi-bin/naip.wms");
        params.setValue(AVKey.DATASET_NAME, "naip2005C");
        params.setValue(AVKey.FORMAT_SUFFIX, ".dds");
        params.setValue(AVKey.NUM_LEVELS, 14);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d)));
        params.setValue(AVKey.SECTOR, Sector.fromDegrees(32.2006, 42.0421, -124.45, -113.222));
        params.setValue(AVKey.TILE_URL_BUILDER, urlBuilder);

        return new LevelSet(params);
    }

    private static class URLBuilder implements TileUrlBuilder
    {
        public URL getURL(Tile tile, String imageFormat) throws MalformedURLException
        {
            StringBuffer sb = new StringBuffer(tile.getLevel().getService());
            if (sb.lastIndexOf("?") != sb.length() - 1)
                sb.append("?");
            sb.append("service=WMS");
            sb.append("&request=GetMap");
            sb.append("&version=1.1.1");
            sb.append("&srs=EPSG:4326");
            sb.append("&layers=");
            sb.append(tile.getLevel().getDataset());
            sb.append("&styles=default");
            sb.append("&width=");
            sb.append(tile.getLevel().getTileWidth());
            sb.append("&height=");
            sb.append(tile.getLevel().getTileHeight());
            sb.append("&format=image/png");
            sb.append("&transparent=true");
            sb.append("&bgcolor=0x000000");

            Sector s = tile.getSector();
            sb.append("&bbox=");
            sb.append(s.getMinLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMinLatitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLatitude().getDegrees());


            return new java.net.URL(sb.toString());
        }
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Earth.NAIP.California.Name");
    }
}
