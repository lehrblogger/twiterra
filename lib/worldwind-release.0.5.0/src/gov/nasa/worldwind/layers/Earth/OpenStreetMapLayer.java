/*
Copyright (C) 2001, 2006 United States Government
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
 * OpenStreetMap WMS layer(s).
 * @author Patrick Murris
 * @version $Id: OpenStreetMapLayer.java 5055 2008-04-14 05:19:11Z tgaskins $
 */
public class OpenStreetMapLayer extends BasicTiledImageLayer
{
    private static final String defaultDatasetName = "osm-4326-hybrid";
    private String datasetName;

    /**
     * Default OpenStreetMap hybrid layer - transparent, see-through.
     */
    public OpenStreetMapLayer()
    {
        super(makeLevels(defaultDatasetName));
        this.setUseTransparentTextures(true);
        this.datasetName = defaultDatasetName;
    }

    /**
     * Access to a specific layer from OSM WMS server - eg 'osm-4326'.
     * @param datasetName the layer dataset name.
     */
    public OpenStreetMapLayer(String datasetName)
    {
        super(makeLevels(datasetName));
        this.setUseTransparentTextures(true);
        this.datasetName = datasetName;
    }

    private static LevelSet makeLevels(String datasetName)
    {
        AVList params = new AVListImpl();

        params.setValue(AVKey.TILE_WIDTH, 256);
        params.setValue(AVKey.TILE_HEIGHT, 256);
        params.setValue(AVKey.DATA_CACHE_NAME, "Earth/OSM-WMS/" + datasetName);
        params.setValue(AVKey.SERVICE, "http://t2.hypercube.telascience.org/tiles");
        params.setValue(AVKey.DATASET_NAME, datasetName);
        params.setValue(AVKey.FORMAT_SUFFIX, ".dds");
        params.setValue(AVKey.NUM_LEVELS, 20);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(180d), Angle.fromDegrees(180d)));
        params.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);
        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());

        return new LevelSet(params);
    }

    private static class URLBuilder implements TileUrlBuilder
    {
        public URL getURL(Tile tile, String imageFormat) throws MalformedURLException
        {
            StringBuffer sb = new StringBuffer(tile.getLevel().getService());
            if (sb.lastIndexOf("?") != sb.length() - 1)
                sb.append("?");
            sb.append("request=GetMap");
            sb.append("&layers=");
            sb.append(tile.getLevel().getDataset());
            sb.append("&srs=EPSG:4326");
            sb.append("&width=");
            sb.append(tile.getLevel().getTileWidth());
            sb.append("&height=");
            sb.append(tile.getLevel().getTileHeight());

            Sector s = tile.getSector();
            sb.append("&bbox=");
            sb.append(s.getMinLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMinLatitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLatitude().getDegrees());

            sb.append("&format=image/png");
            sb.append("&service=WMS");
            sb.append("&version=1.1.1");

            return new java.net.URL(sb.toString());
        }
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Earth.OpenStreetMapLayer.Name") +
                (defaultDatasetName.equals(this.datasetName) ? "" : " " + this.datasetName);
    }
}
