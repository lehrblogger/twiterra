/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;

/**
 * @author tag
 * @version $Id: Level.java 5055 2008-04-14 05:19:11Z tgaskins $
 */
public class Level implements Comparable<Level>
{
    private final AVList params;
    private final int levelNumber;
    private final String levelName; // null or empty level name signifies no data resources associated with this level
    private final LatLon tileDelta;
    private final int tileWidth;
    private final int tileHeight;
    private final String cacheName;
    private final String service;
    private final String dataset;
    private final String formatSuffix;
    private final double texelSize;
    private final String path;
    private final TileUrlBuilder urlBuilder;
    private long expiryTime = 0;
    private boolean active = true;

    // Absent tiles: A tile is deemed absent if a specified maximum number of attempts have been made to retrieve it.
    // Retrieval attempts are governed by a minimum time interval between successive attempts. If an attempt is made
    // within this interval, the tile is still deemed to be absent until the interval expires.
    private final AbsentResourceList absentTiles;
    int DEFAULT_MAX_ABSENT_TILE_ATTEMPTS = 2;
    int DEFAULT_MIN_ABSENT_TILE_CHECK_INTERVAL = 10000; // milliseconds

    public Level(AVList params)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.LayerParams");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.params = params.copy(); // Private copy to insulate from subsequent changes by the app
        String message = this.validate(params);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String ln = this.params.getStringValue(AVKey.LEVEL_NAME);
        this.levelName = ln != null ? ln : "";

        this.levelNumber = (Integer) this.params.getValue(AVKey.LEVEL_NUMBER);
        this.tileDelta = (LatLon) this.params.getValue(AVKey.TILE_DELTA);
        this.tileWidth = (Integer) this.params.getValue(AVKey.TILE_WIDTH);
        this.tileHeight = (Integer) this.params.getValue(AVKey.TILE_HEIGHT);
        this.cacheName = this.params.getStringValue(AVKey.DATA_CACHE_NAME);
        this.service = this.params.getStringValue(AVKey.SERVICE);
        this.dataset = this.params.getStringValue(AVKey.DATASET_NAME);
        this.formatSuffix = this.params.getStringValue(AVKey.FORMAT_SUFFIX);
        this.urlBuilder = (TileUrlBuilder) this.params.getValue(AVKey.TILE_URL_BUILDER);
        this.expiryTime = AVListImpl.getLongValue(params, AVKey.EXPIRY_TIME, 0L);

//        double averageTileSize = 0.5 * (this.tileWidth + this.tileHeight);
//        double averageTileDelta =
//            0.5 * (this.tileDelta.getLatitude().getRadians() + this.tileDelta.getLongitude().getRadians());
        this.texelSize = this.tileDelta.getLatitude().getRadians() / this.tileHeight;

        this.path = this.cacheName + "/" + this.levelName;

        Integer maxAbsentTileAttempts = (Integer) this.params.getValue(AVKey.MAX_ABSENT_TILE_ATTEMPTS);
        if (maxAbsentTileAttempts == null)
            maxAbsentTileAttempts = DEFAULT_MAX_ABSENT_TILE_ATTEMPTS;

        Integer minAbsentTileCheckInterval = (Integer) this.params.getValue(AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL);
        if (minAbsentTileCheckInterval == null)
            minAbsentTileCheckInterval = DEFAULT_MIN_ABSENT_TILE_CHECK_INTERVAL;

        this.absentTiles = new AbsentResourceList(maxAbsentTileAttempts, minAbsentTileCheckInterval);
    }

    /**
     * Determines whether the constructor arguments are valid.
     *
     * @param params the list of parameters to validate.
     * @return null if valid, otherwise a <code>String</code> containing a description of why it's invalid.
     */
    protected String validate(AVList params)
    {
        StringBuffer sb = new StringBuffer();

        Object o = params.getValue(AVKey.LEVEL_NUMBER);
        if (o == null || !(o instanceof Integer) || ((Integer) o) < 0)
            sb.append(Logging.getMessage("term.levelNumber"));

        o = params.getValue(AVKey.LEVEL_NAME);
        if (o == null || !(o instanceof String))
            sb.append(Logging.getMessage("term.levelName"));

        o = params.getValue(AVKey.TILE_WIDTH);
        if (o == null || !(o instanceof Integer) || ((Integer) o) < 0)
            sb.append(Logging.getMessage("term.tileWidth"));

        o = params.getValue(AVKey.TILE_HEIGHT);
        if (o == null || !(o instanceof Integer) || ((Integer) o) < 0)
            sb.append(Logging.getMessage("term.tileHeight"));

        o = params.getValue(AVKey.TILE_DELTA);
        if (o == null || !(o instanceof LatLon))
            sb.append(Logging.getMessage("term.tileDelta"));

        o = params.getValue(AVKey.DATA_CACHE_NAME);
        if (o == null || !(o instanceof String) || ((String) o).length() < 1)
            sb.append(Logging.getMessage("term.cacheFolder"));

        o = params.getValue(AVKey.TILE_URL_BUILDER);
        if (o == null || !(o instanceof TileUrlBuilder))
            sb.append(Logging.getMessage("term.tileURLBuilder"));

        o = params.getValue(AVKey.EXPIRY_TIME);
        if (o != null && (!(o instanceof Long) || ((Long) o) < 1))
            sb.append(Logging.getMessage("term.expiryTime"));

        if (params.getStringValue(AVKey.LEVEL_NAME).length() > 0)
        {
            o = params.getValue(AVKey.DATASET_NAME);
            if (o == null || !(o instanceof String) || ((String) o).length() < 1)
                sb.append(Logging.getMessage("term.datasetName"));

            o = params.getValue(AVKey.FORMAT_SUFFIX);
            if (o == null || !(o instanceof String) || ((String) o).length() < 1)
                sb.append(Logging.getMessage("term.formatSuffix"));
        }

        if (sb.length() == 0)
            return null;

        return Logging.getMessage("layers.LevelSet.InvalidLevelDescriptorFields", sb.toString());
    }

    public final AVList getParams()
    {
        return params;
    }

    public String getPath()
    {
        return this.path;
    }

    public final int getLevelNumber()
    {
        return this.levelNumber;
    }

    public String getLevelName()
    {
        return this.levelName;
    }

    public LatLon getTileDelta()
    {
        return this.tileDelta;
    }

    public final int getTileWidth()
    {
        return this.tileWidth;
    }

    public final int getTileHeight()
    {
        return this.tileHeight;
    }

    public final String getFormatSuffix()
    {
        return this.formatSuffix;
    }

    public final String getService()
    {
        return this.service;
    }

    public final String getDataset()
    {
        return this.dataset;
    }

    public final String getCacheName()
    {
        return this.cacheName;
    }

    public final double getTexelSize(double radius)
    {
        return radius * this.texelSize;
    }

    public final boolean isEmpty()
    {
        return this.levelName == null || this.levelName.equals("") || !this.active;
    }

    public final void markResourceAbsent(long tileNumber)
    {
        this.absentTiles.markResourceAbsent(tileNumber);
    }

    public final boolean isResourceAbsent(long tileNumber)
    {
        return this.absentTiles.isResourceAbsent(tileNumber);
    }

    public final void unmarkResourceAbsent(long tileNumber)
    {
        this.absentTiles.unmarkResourceAbsent(tileNumber);
    }

    public final long getExpiryTime()
    {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) // TODO: remove
    {
        this.expiryTime = expiryTime;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

//    public interface TileURLBuilder
//    {
//        public URL getURL(Tile tile) throws java.net.MalformedURLException;
//    }

    /**
     * Returns the URL necessary to retrieve the specified tile.
     *
     * @param tile the tile who's resources will be retrieved.
     * @param imageFormat a string identifying the mime type of the desired image format
     * @return the resource URL.
     * @throws java.net.MalformedURLException if the URL cannot be formed from the tile's parameters.
     * @throws IllegalArgumentException       if <code>tile</code> is null.
     */
    public java.net.URL getTileResourceURL(Tile tile, String imageFormat) throws java.net.MalformedURLException
    {
        if (tile == null)
        {
            String msg = Logging.getMessage("nullValue.TileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.urlBuilder.getURL(tile, imageFormat);
    }

    public Sector computeSectorForPosition(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Compute the tile's SW lat/lon based on its row/col in the level's data set.
        Angle dLat = this.getTileDelta().getLatitude();
        Angle dLon = this.getTileDelta().getLongitude();

        int row = Tile.computeRow(this.getTileDelta().getLatitude(), latitude);
        int col = Tile.computeColumn(this.getTileDelta().getLongitude(), longitude);
        Angle minLatitude = Tile.computeRowLatitude(row, dLat);
        Angle minLongitude = Tile.computeColumnLongitude(col, dLon);

        return new Sector(minLatitude, minLatitude.add(dLat), minLongitude, minLongitude.add(dLon));
    }

    public int compareTo(Level that)
    {
        if (that == null)
        {
            String msg = Logging.getMessage("nullValue.LevelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        return this.levelNumber < that.levelNumber ? -1 : this.levelNumber == that.levelNumber ? 0 : 1;
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final Level level = (Level) o;

        if (levelNumber != level.levelNumber)
            return false;
        if (tileHeight != level.tileHeight)
            return false;
        if (tileWidth != level.tileWidth)
            return false;
        if (cacheName != null ? !cacheName.equals(level.cacheName) : level.cacheName != null)
            return false;
        if (dataset != null ? !dataset.equals(level.dataset) : level.dataset != null)
            return false;
        if (formatSuffix != null ? !formatSuffix.equals(level.formatSuffix) : level.formatSuffix != null)
            return false;
        if (levelName != null ? !levelName.equals(level.levelName) : level.levelName != null)
            return false;
        if (service != null ? !service.equals(level.service) : level.service != null)
            return false;
        //noinspection RedundantIfStatement
        if (tileDelta != null ? !tileDelta.equals(level.tileDelta) : level.tileDelta != null)
            return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = levelNumber;
        result = 29 * result + (levelName != null ? levelName.hashCode() : 0);
        result = 29 * result + (tileDelta != null ? tileDelta.hashCode() : 0);
        result = 29 * result + tileWidth;
        result = 29 * result + tileHeight;
        result = 29 * result + (formatSuffix != null ? formatSuffix.hashCode() : 0);
        result = 29 * result + (service != null ? service.hashCode() : 0);
        result = 29 * result + (dataset != null ? dataset.hashCode() : 0);
        result = 29 * result + (cacheName != null ? cacheName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return this.path;
    }
}
