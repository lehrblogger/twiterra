/*
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.placename;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GeographicText;
import gov.nasa.worldwind.render.GeographicTextRenderer;
import gov.nasa.worldwind.render.UserFacingText;
import gov.nasa.worldwind.retrieve.HTTPRetriever;
import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;

/**
 * @author Paul Collins
 * @version $Id: PlaceNameLayer.java 5125 2008-04-22 18:45:34Z jparsons $
 */
public class PlaceNameLayer extends AbstractLayer
{
    private final PlaceNameServiceSet placeNameServiceSet;
    private final List<Tile[]> tiles = new ArrayList<Tile[]>();
    private PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<Runnable>(64);
    private Vec4 referencePoint;
    private final Object fileLock = new Object();

    /**
     * @param placeNameServiceSet the set of PlaceNameService objects that PlaceNameLayer will render.
     * @throws IllegalArgumentException if <code>placeNameServiceSet</code> is null
     */
    public PlaceNameLayer(PlaceNameServiceSet placeNameServiceSet)
    {
        if (placeNameServiceSet == null)
        {
            String message = Logging.getMessage("nullValue.PlaceNameServiceSetIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.placeNameServiceSet = placeNameServiceSet.deepCopy();
        for (int i = 0; i < this.placeNameServiceSet.getServiceCount(); i++)
        {
            tiles.add(i, buildTiles(this.placeNameServiceSet.getService(i)));
        }

        if (!WorldWind.getMemoryCacheSet().containsCache(Tile.class.getName()))
        {
            long size = Configuration.getLongValue(AVKey.PLACENAME_LAYER_CACHE_SIZE, 2000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName("Placename Tiles");
            WorldWind.getMemoryCacheSet().addCache(Tile.class.getName(), cache);
        }
    }

    public final PlaceNameServiceSet getPlaceNameServiceSet()
    {
        return this.placeNameServiceSet;
    }

    private PriorityBlockingQueue<Runnable> getRequestQ()
    {
        return this.requestQ;
    }

    // ============== Tile Assembly ======================= //
    // ============== Tile Assembly ======================= //
    // ============== Tile Assembly ======================= //

    private static class Tile
    {
        final PlaceNameService placeNameService;
        final Sector sector;
        final int row;
        final int column;
        final int hash;
        // Computed data.
        String fileCachePath = null;
        Extent extent = null;
        double extentVerticalExaggeration = Double.MIN_VALUE;
        private Vec4 centroid; // Cartesian coordinate of lat/lon center
        private double priority = Double.MAX_VALUE; // Default is minimum priority

        static int computeRow(Angle delta, Angle latitude)
        {
            if (delta == null || latitude == null)
            {
                String msg = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            return (int) ((latitude.getDegrees() + 90d) / delta.getDegrees());
        }

        static int computeColumn(Angle delta, Angle longitude)
        {
            if (delta == null || longitude == null)
            {
                String msg = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            return (int) ((longitude.getDegrees() + 180d) / delta.getDegrees());
        }

        static Angle computeRowLatitude(int row, Angle delta)
        {
            if (delta == null)
            {
                String msg = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            return Angle.fromDegrees(-90d + delta.getDegrees() * row);
        }

        static Angle computeColumnLongitude(int column, Angle delta)
        {
            if (delta == null)
            {
                String msg = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            return Angle.fromDegrees(-180 + delta.getDegrees() * column);
        }

        Tile(PlaceNameService placeNameService, Sector sector, int row, int column)
        {
            this.placeNameService = placeNameService;
            this.sector = sector;
            this.row = row;
            this.column = column;
            this.hash = this.computeHash();
        }

        int computeHash()
        {
            return this.getFileCachePath() != null ? this.getFileCachePath().hashCode() : 0;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final Tile tile = (Tile) o;

            return !(this.getFileCachePath() != null ? !this.getFileCachePath().equals(tile.getFileCachePath()) : tile.getFileCachePath() != null);
        }

        Extent getExtent(DrawContext dc)
        {
            if (dc == null)
            {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().fine(message);
                throw new IllegalArgumentException(message);
            }

            if (this.extent == null || this.extentVerticalExaggeration != dc.getVerticalExaggeration())
            {
                this.extentVerticalExaggeration = dc.getVerticalExaggeration();
                this.extent = dc.getGlobe().computeBoundingCylinder(this.extentVerticalExaggeration,
                    this.sector);
            }

            return extent;
        }

        String getFileCachePath()
        {
            if (this.fileCachePath == null)
                this.fileCachePath = this.placeNameService.createFileCachePathFromTile(this.row, this.column);

            return this.fileCachePath;
        }

        PlaceNameService getPlaceNameService()
        {
            return placeNameService;
        }

        java.net.URL getRequestURL() throws java.net.MalformedURLException
        {
            return this.placeNameService.createServiceURLFromSector(this.sector);
        }

        Sector getSector()
        {
            return sector;
        }

        public int hashCode()
        {
            return this.hash;
        }

        boolean isTileInMemory()
        {
            return WorldWind.getMemoryCache(Tile.class.getName()).getObject(this) != null;
        }

        PlaceNameChunk getData()
        {
            return (PlaceNameChunk) WorldWind.getMemoryCache(Tile.class.getName()).getObject(this);
        }

        public Vec4 getCentroidPoint(Globe globe)
        {
            if (globe == null)
            {
                String msg = Logging.getMessage("nullValue.GlobeIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            if (this.centroid == null)
            {
                LatLon c = this.getSector().getCentroid();
                this.centroid = globe.computePointFromPosition(c.getLatitude(), c.getLongitude(), 0);
            }

            return this.centroid;
        }

        public double getPriority()
        {
            return priority;
        }

        public void setPriority(double priority)
        {
            this.priority = priority;
        }
    }

    private Tile[] buildTiles(PlaceNameService placeNameService)
    {
        final Sector sector = placeNameService.getSector();
        final Angle dLat = placeNameService.getTileDelta().getLatitude();
        final Angle dLon = placeNameService.getTileDelta().getLongitude();

        // Determine the row and column offset from the global tiling origin for the southwest tile corner
        int firstRow = Tile.computeRow(dLat, sector.getMinLatitude());
        int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude());
        int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude().subtract(dLat));
        int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude().subtract(dLon));

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        Tile[] tiles = new Tile[nLatTiles * nLonTiles];

        Angle p1 = Tile.computeRowLatitude(firstRow, dLat);
        for (int row = 0; row <= lastRow-firstRow; row++)
        {
            Angle p2;
            p2 = p1.add(dLat);

            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon);
            for (int col = 0; col <= lastCol-firstCol; col++)
            {
                Angle t2;
                t2 = t1.add(dLon);

                tiles[col + row * nLonTiles] = new Tile(placeNameService, new Sector(p1, p2, t1, t2), row, col);
                t1 = t2;
            }
            p1 = p2;
        }

        return tiles;
    }

    // ============== Place Name Data Structures ======================= //
    // ============== Place Name Data Structures ======================= //
    // ============== Place Name Data Structures ======================= //

    private static class PlaceNameChunk implements Cacheable
    {
        final PlaceNameService placeNameService;
        final CharBuffer textArray;
        final int[] textIndexArray;
        final double[] latlonArray;
        final int numEntries;
        final long estimatedMemorySize;

        PlaceNameChunk(PlaceNameService service, CharBuffer text, int[] textIndices,
            double[] positions, int numEntries)
        {
            this.placeNameService = service;
            this.textArray = text;
            this.textIndexArray = textIndices;
            this.latlonArray = positions;
            this.numEntries = numEntries;
            this.estimatedMemorySize = this.computeEstimatedMemorySize();
        }

        long computeEstimatedMemorySize()
        {
            long result = 0;
            if (!textArray.isDirect())
                result += (Character.SIZE / 8) * textArray.capacity();
            result += (Integer.SIZE / 8) * textIndexArray.length;
            result += (Double.SIZE / 8) * latlonArray.length;
            return result;
        }

        Position getPosition(int index)
        {
            int latlonIndex = 2 * index;
            return Position.fromDegrees(latlonArray[latlonIndex], latlonArray[latlonIndex + 1], 0);
        }

        PlaceNameService getPlaceNameService()
        {
            return this.placeNameService;
        }

        CharSequence getText(int index)
        {
            int beginIndex = textIndexArray[index];
            int endIndex = (index + 1 < numEntries) ? textIndexArray[index + 1] : textArray.length();
            return this.textArray.subSequence(beginIndex, endIndex);
        }

        public long getSizeInBytes()
        {
            return this.estimatedMemorySize;
        }

        private Iterable<GeographicText> makeIterable(DrawContext dc)
        {
            ArrayList<GeographicText> list = new ArrayList<GeographicText>();
            for (int i = 0; i < this.numEntries; i++)
            {
                CharSequence str = getText(i);
                Position pos = getPosition(i);
                GeographicText text = new UserFacingText(str, pos);
                text.setFont(this.placeNameService.getFont());
                text.setColor(this.placeNameService.getColor());
                text.setBackgroundColor(this.placeNameService.getBackgroundColor());
                text.setVisible(isNameVisible(dc, this.placeNameService, pos));
                list.add(text);
            }
            return list;
        }
    }

    // ============== Rendering ======================= //
    // ============== Rendering ======================= //
    // ============== Rendering ======================= //

    private final GeographicTextRenderer placeNameRenderer = new GeographicTextRenderer();

    @Override
    public void dispose() // override if disposal is a supported operation
    {
        super.dispose();

        if (this.placeNameRenderer != null)
            this.placeNameRenderer.dispose();
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        this.referencePoint = this.computeReferencePoint(dc);

        int serviceCount = this.placeNameServiceSet.getServiceCount();
        for (int i = 0; i < serviceCount; i++)
        {
            PlaceNameService placeNameService = this.placeNameServiceSet.getService(i);
            if (!isServiceVisible(dc, placeNameService))
                continue;

            double minDist = placeNameService.getMinDisplayDistance();
            double maxDist = placeNameService.getMaxDisplayDistance();
            double minDistSquared = minDist * minDist;
            double maxDistSquared = maxDist * maxDist;

            if (isSectorVisible(dc, placeNameService.getSector(), minDistSquared, maxDistSquared))
            {
                    Tile[] tiles = this.tiles.get(i);
                    for (Tile tile : tiles)
                    {
                        try
                        {
                            drawOrRequestTile(dc, tile, minDistSquared, maxDistSquared);
                        }
                        catch (Exception e)
                        {
                            Logging.logger().log(Level.FINE, Logging.getMessage("layers.PlaceNameLayer.ExceptionRenderingTile"),
                                e);
                        }
                    }
            }
        }

        this.sendRequests();
        this.requestQ.clear();
    }

    private Vec4 computeReferencePoint(DrawContext dc)
    {
        if (dc.getViewportCenterPosition() != null)
            return dc.getGlobe().computePointFromPosition(dc.getViewportCenterPosition());

        java.awt.geom.Rectangle2D viewport = dc.getView().getViewport();
        int x = (int) viewport.getWidth() / 2;
        for (int y = (int) (0.5 * viewport.getHeight()); y >= 0; y--)
        {
            Position pos = dc.getView().computePositionFromScreenPoint(x, y);
            if (pos == null)
                continue;

            return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(), 0d);
        }

        return null;
    }

    protected Vec4 getReferencePoint()
    {
        return this.referencePoint;
    }

    private void drawOrRequestTile(DrawContext dc, Tile tile, double minDisplayDistanceSquared,
        double maxDisplayDistanceSquared)
    {
        if (!isTileVisible(dc, tile, minDisplayDistanceSquared, maxDisplayDistanceSquared))
            return;

        if (tile.isTileInMemory())
        {
            PlaceNameChunk placeNameChunk = tile.getData();
            Iterable<GeographicText> renderIter = placeNameChunk.makeIterable(dc);
            this.placeNameRenderer.render(dc, renderIter);
            return;
        }

        // Tile's data isn't available, so request it
        if (!tile.getPlaceNameService().isResourceAbsent(tile.getPlaceNameService().getTileNumber(
                tile.row, tile.column)))
        {
            this.requestTile(dc, tile);
        }
    }

    private static boolean isServiceVisible(DrawContext dc, PlaceNameService placeNameService)
    {
        if (!placeNameService.isEnabled())
            return false;
        //noinspection SimplifiableIfStatement
        if (dc.getVisibleSector() != null && !placeNameService.getSector().intersects(dc.getVisibleSector()))
            return false;

        return placeNameService.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates());
    }


    private static boolean isSectorVisible(DrawContext dc, Sector sector, double minDistanceSquared,
        double maxDistanceSquared)
    {

        View view = dc.getView();
        Position eyePos = view.getEyePosition();
        if (eyePos == null)
            return false;
        //todo use bounds of layer?


        Angle lat = clampAngle(eyePos.getLatitude(), sector.getMinLatitude(), sector.getMaxLatitude());
        Angle lon = clampAngle(eyePos.getLongitude(), sector.getMinLongitude(), sector.getMaxLongitude());
        Vec4 p = dc.getGlobe().computePointFromPosition(lat, lon, 0d);
        double distSquared = dc.getView().getEyePoint().distanceToSquared3(p);
        //noinspection RedundantIfStatement
        if (minDistanceSquared > distSquared || maxDistanceSquared < distSquared)
            return false;

        return true;
    }

    private static boolean isTileVisible(DrawContext dc, Tile tile, double minDistanceSquared,
        double maxDistanceSquared)
    {
        if (!tile.getSector().intersects(dc.getVisibleSector()))
            return false;

        View view = dc.getView();
        Position eyePos = view.getEyePosition();
        if (eyePos == null)
            return false;

        Angle lat = clampAngle(eyePos.getLatitude(), tile.getSector().getMinLatitude(),
            tile.getSector().getMaxLatitude());
        Angle lon = clampAngle(eyePos.getLongitude(), tile.getSector().getMinLongitude(),
            tile.getSector().getMaxLongitude());
        Vec4 p = dc.getGlobe().computePointFromPosition(lat, lon, 0d);
        double distSquared = dc.getView().getEyePoint().distanceToSquared3(p);
        //noinspection RedundantIfStatement
        if (minDistanceSquared > distSquared || maxDistanceSquared < distSquared)
            return false;

        return true;
    }

    private static boolean isNameVisible(DrawContext dc, PlaceNameService service, Position namePosition)
    {
        double elevation = dc.getVerticalExaggeration() * namePosition.getElevation();
        Vec4 namePoint = dc.getGlobe().computePointFromPosition(namePosition.getLatitude(),
            namePosition.getLongitude(), elevation);
        Vec4 eyeVec = dc.getView().getEyePoint();

        double dist = eyeVec.distanceTo3(namePoint);
        return dist >= service.getMinDisplayDistance() && dist <= service.getMaxDisplayDistance();
    }

    private static Angle clampAngle(Angle a, Angle min, Angle max)
    {
        double degrees = a.degrees;
        double minDegrees = min.degrees;
        double maxDegrees = max.degrees;
        return Angle.fromDegrees(degrees < minDegrees ? minDegrees : (degrees > maxDegrees ? maxDegrees : degrees));
    }

    // ============== Image Reading and Downloading ======================= //
    // ============== Image Reading and Downloading ======================= //
    // ============== Image Reading and Downloading ======================= //

    private void requestTile(DrawContext dc, Tile tile)
    {
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        if (this.getReferencePoint() != null)
            tile.setPriority(centroid.distanceTo3(this.getReferencePoint()));

        RequestTask task = new RequestTask(tile, this);
        this.getRequestQ().add(task);
    }

    private void sendRequests()
    {
        Runnable task = this.requestQ.poll();
        while (task != null)
        {
            if (!WorldWind.getTaskService().isFull())
            {
                WorldWind.getTaskService().addTask(task);
            }
            task = this.requestQ.poll();
        }
    }

    private static class RequestTask implements Runnable, Comparable<RequestTask>
    {
        private final PlaceNameLayer layer;
        private final Tile tile;

        RequestTask(Tile tile, PlaceNameLayer layer)
        {
            this.layer = layer;
            this.tile = tile;
        }

        public void run()
        {
            if (this.tile.isTileInMemory())
                return;

            final java.net.URL tileURL = WorldWind.getDataFileCache().findFile(tile.getFileCachePath(), false);
            if (tileURL != null)
            {
                if (this.layer.loadTile(this.tile, tileURL))
                {
                    tile.getPlaceNameService().unmarkResourceAbsent(tile.getPlaceNameService().getTileNumber(
                        tile.row,
                        tile.column));
                    this.layer.firePropertyChange(AVKey.LAYER, null, this);
                    return;
                }
                else
                {
                    // Assume that something's wrong with the file and delete it.
                    WorldWind.getDataFileCache().removeFile(tileURL);
                    tile.getPlaceNameService().markResourceAbsent(tile.getPlaceNameService().getTileNumber(tile.row,
                        tile.column));
                    String message = Logging.getMessage("generic.DeletedCorruptDataFile", tileURL);
                    Logging.logger().info(message);
                }
            }

            this.layer.downloadTile(this.tile);
        }

        /**
         * @param that the task to compare
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(RequestTask that)
        {
            if (that == null)
            {
                String msg = Logging.getMessage("nullValue.RequestTaskIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            return this.tile.getPriority() == that.tile.getPriority() ? 0 :
                this.tile.getPriority() < that.tile.getPriority() ? -1 : 1;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RequestTask that = (RequestTask) o;

            // Don't include layer in comparison so that requests are shared among layers
            return !(tile != null ? !tile.equals(that.tile) : that.tile != null);
        }

        public int hashCode()
        {
            return (tile != null ? tile.hashCode() : 0);
        }

        public String toString()
        {
            return this.tile.toString();
        }
    }

    private boolean loadTile(Tile tile, java.net.URL url)
    {
        if (WWIO.isFileOutOfDate(url, this.placeNameServiceSet.getExpiryTime()))
        {
            // The file has expired. Delete it then request download of newer.
            WorldWind.getDataFileCache().removeFile(url);
            String message = Logging.getMessage("generic.DataFileExpired", url);
            Logging.logger().fine(message);
            return false;
        }

        PlaceNameChunk tileData;
        synchronized (this.fileLock)
        {
            tileData = readTileData(tile, url);
        }

        if (tileData == null)
            return false;

        addTileToCache(tile, tileData);
        return true;
    }

    private static PlaceNameChunk readTileData(Tile tile, java.net.URL url)
    {
        java.io.InputStream is = null;

        try
        {
            String path = url.getFile();
            path = path.replaceAll("%20", " "); // TODO: find a better way to get a path usable by FileInputStream

            java.io.FileInputStream fis = new java.io.FileInputStream(path);
            java.io.BufferedInputStream buf = new java.io.BufferedInputStream(fis);
            is = new java.util.zip.GZIPInputStream(buf);

            GMLPlaceNameSAXHandler handler = new GMLPlaceNameSAXHandler();
            javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser().parse(is, handler);
            return handler.createPlaceNameChunk(tile.getPlaceNameService());
        }
        catch (Exception e)
        {
            //todo log actual error JAY
            Logging.logger().log(Level.FINE,
                    Logging.getMessage("layers.PlaceNameLayer.ExceptionAttemptingToReadFile", url.toString()), e);
        }
        finally
        {
            try
            {
                if (is != null)
                    is.close();
            }
            catch (java.io.IOException e)
            {
                Logging.logger().log(Level.FINE,
                        Logging.getMessage("layers.PlaceNameLayer.ExceptionAttemptingToReadFile", url.toString()), e);
            }
        }

        return null;
    }

    private static CharBuffer newCharBuffer(int numElements)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect((Character.SIZE / 8) * numElements);
        bb.order(ByteOrder.nativeOrder());
        return bb.asCharBuffer();
    }

    private static class GMLPlaceNameSAXHandler extends org.xml.sax.helpers.DefaultHandler
    {
        static final String GML_FEATURE_MEMBER = "gml:featureMember";
        static final String TOPP_FULL_NAME_ND = "topp:full_name_nd";
        static final String TOPP_LATITUDE = "topp:latitude";
        static final String TOPP_LONGITUDE = "topp:longitude";
        final LinkedList<String> internedQNameStack = new LinkedList<String>();
        boolean inBeginEndPair = false;
        StringBuilder latBuffer = new StringBuilder();
        StringBuilder lonBuffer = new StringBuilder();

        StringBuilder textArray = new StringBuilder();
        int[] textIndexArray = new int[16];
        double[] latlonArray = new double[16];
        int numEntries = 0;

        GMLPlaceNameSAXHandler()
        {
        }

        PlaceNameChunk createPlaceNameChunk(PlaceNameService service)
        {
            int numChars = this.textArray.length();
            CharBuffer textBuffer = newCharBuffer(numChars);
            textBuffer.put(this.textArray.toString());
            textBuffer.rewind();
            return new PlaceNameChunk(service, textBuffer, this.textIndexArray, this.latlonArray, this.numEntries);
        }

        void beginEntry()
        {
            int textIndex = this.textArray.length();
            this.textIndexArray = append(this.textIndexArray, this.numEntries, textIndex);
            this.inBeginEndPair = true;
        }

        void endEntry()
        {
            double lat = this.parseDouble(this.latBuffer);
            double lon = this.parseDouble(this.lonBuffer);
            int numLatLon = 2 * this.numEntries;
            this.latlonArray = this.append(this.latlonArray, numLatLon, lat);
            numLatLon++;
            this.latlonArray = this.append(this.latlonArray, numLatLon, lon);

            this.latBuffer.delete(0, this.latBuffer.length());
            this.lonBuffer.delete(0, this.lonBuffer.length());
            this.inBeginEndPair = false;
            this.numEntries++;
        }

        double parseDouble(StringBuilder sb)
        {
            double value = 0;
            try
            {
                value = Double.parseDouble(sb.toString());
            }
            catch (NumberFormatException e)
            {
                Logging.logger().log(Level.FINE,
                        Logging.getMessage("layers.PlaceNameLayer.ExceptionAttemptingToReadFile", ""), e);
            }
            return value;
        }

        int[] append(int[] array, int index, int value)
        {
            if (index >= array.length)
                array = this.resizeArray(array);
            array[index] = value;
            return array;
        }

        int[] resizeArray(int[] oldArray)
        {
            int newSize = 2 * oldArray.length;
            int[] newArray = new int[newSize];
            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
            return newArray;
        }

        double[] append(double[] array, int index, double value)
        {
            if (index >= array.length)
                array = this.resizeArray(array);
            array[index] = value;
            return array;
        }

        double[] resizeArray(double[] oldArray)
        {
            int newSize = 2 * oldArray.length;
            double[] newArray = new double[newSize];
            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
            return newArray;
        }

        @SuppressWarnings({"StringEquality"})
        public void characters(char ch[], int start, int length)
        {
            if (!this.inBeginEndPair)
                return;

            // Top of QName stack is an interned string,
            // so we can use pointer comparison.
            String internedTopQName = this.internedQNameStack.getFirst();

            StringBuilder sb = null;
            if (TOPP_LATITUDE == internedTopQName)
                sb = this.latBuffer;
            else if (TOPP_LONGITUDE == internedTopQName)
                sb = this.lonBuffer;
            else if (TOPP_FULL_NAME_ND == internedTopQName)
                sb = this.textArray;

            if (sb != null)
                sb.append(ch, start, length);
        }

        public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes)
        {
            // Don't validate uri, localName or attributes because they aren't used.
            // Intern the qName string so we can use pointer comparison.
            String internedQName = qName.intern();
            //noinspection StringEquality
            if (GML_FEATURE_MEMBER == internedQName)
                this.beginEntry();
            this.internedQNameStack.addFirst(internedQName);
        }

        public void endElement(String uri, String localName, String qName)
        {
            // Don't validate uri or localName because they aren't used.
            // Intern the qName string so we can use pointer comparison.
            String internedQName = qName.intern();
            //noinspection StringEquality
            if (GML_FEATURE_MEMBER == internedQName)
                this.endEntry();
            this.internedQNameStack.removeFirst();
        }
    }

    private void addTileToCache(Tile tile, PlaceNameChunk tileData)
    {
        WorldWind.getMemoryCache(Tile.class.getName()).add(tile, tileData);
    }

    private void downloadTile(final Tile tile)
    {
        if (!WorldWind.getRetrievalService().isAvailable())
            return;

        java.net.URL url;
        try
        {
            url = tile.getRequestURL();
            if (WorldWind.getNetworkStatus().isHostUnavailable(url))
                return;
        }
        catch (java.net.MalformedURLException e)
        {
            Logging.logger().log(java.util.logging.Level.SEVERE,
                    Logging.getMessage("layers.TextureLayer.ExceptionCreatingTextureUrl", tile), e);
            return;
        }

        Retriever retriever;

        if ("http".equalsIgnoreCase(url.getProtocol()))
        {
            retriever = new HTTPRetriever(url, new DownloadPostProcessor(this, tile));
        }
        else
        {
            Logging.logger().severe(
                    Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", url.toString()));
            return;
        }

        // Apply any overridden timeouts.
        Integer cto = AVListImpl.getIntegerValue(this, AVKey.URL_CONNECT_TIMEOUT);
        if (cto != null && cto > 0)
            retriever.setConnectTimeout(cto);
        Integer cro = AVListImpl.getIntegerValue(this, AVKey.URL_READ_TIMEOUT);
        if (cro != null && cro > 0)
            retriever.setReadTimeout(cro);
        Integer srl = AVListImpl.getIntegerValue(this, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
        if (srl != null && srl > 0)
            retriever.setStaleRequestLimit(srl);

        WorldWind.getRetrievalService().runRetriever(retriever, tile.getPriority());
    }

    private void saveBuffer(java.nio.ByteBuffer buffer, java.io.File outFile) throws java.io.IOException
    {
        synchronized (this.fileLock) // sychronized with read of file in RequestTask.run()
        {
            WWIO.saveBuffer(buffer, outFile);
        }
    }

    private static class DownloadPostProcessor implements RetrievalPostProcessor
    {
        final PlaceNameLayer layer;
        final Tile tile;

        private DownloadPostProcessor(PlaceNameLayer layer, Tile tile)
        {
            this.layer = layer;
            this.tile = tile;
        }

        public java.nio.ByteBuffer run(Retriever retriever)
        {
            if (retriever == null)
            {
                String msg = Logging.getMessage("nullValue.RetrieverIsNull");
                Logging.logger().fine(msg);
                throw new IllegalArgumentException(msg);
            }

            try
            {
                if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
                    return null;

                URLRetriever r = (URLRetriever) retriever;
                ByteBuffer buffer = r.getBuffer();

                if (retriever instanceof HTTPRetriever)
                {
                    HTTPRetriever htr = (HTTPRetriever) retriever;
                    if (htr.getResponseCode() == java.net.HttpURLConnection.HTTP_NO_CONTENT)
                    {
                        // Mark tile as missing to avoid further attempts
                        tile.getPlaceNameService().markResourceAbsent(tile.getPlaceNameService().getTileNumber(tile.row,
                            tile.column));
                        return null;
                    }
                    else if (htr.getResponseCode() != java.net.HttpURLConnection.HTTP_OK)
                    {
                        // Also mark tile as missing, but for an unknown reason.
                        tile.getPlaceNameService().markResourceAbsent(tile.getPlaceNameService().getTileNumber(tile.row,
                            tile.column));
                        return null;
                    }
                }

                final java.io.File outFile = WorldWind.getDataFileCache().newFile(this.tile.getFileCachePath());
                if (outFile == null)
                    return null;

                if (outFile.exists())
                    return buffer; // info is already here; don't need to do anything

                if (buffer != null)
                {
                    String contentType = retriever.getContentType();
                    //System.out.println("placenamelayer content type: "+contentType);
                    if (contentType == null)
                    {
                        // TODO: logger message
                        return null;
                    }

                    this.layer.saveBuffer(buffer, outFile);
                    this.layer.firePropertyChange(AVKey.LAYER, null, this);
                    return buffer;
                }
            }
            catch (java.io.IOException e)
            {
                tile.getPlaceNameService().markResourceAbsent(tile.getPlaceNameService().getTileNumber(tile.row,
                    tile.column));
                Logging.logger().log(Level.FINE, Logging.getMessage(
                    "layers.PlaceNameLayer.ExceptionSavingRetrievedFile", this.tile.getFileCachePath()), e);
            }
            return null;
        }
    }
}
