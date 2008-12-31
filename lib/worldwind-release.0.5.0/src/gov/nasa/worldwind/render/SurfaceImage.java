/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.logging.Level;

/**
 * Renders a single image tile. The image source can be a local file, a <code>BufferedImage</code>
 * or a network http source.
 * <p>
 * Images from remote sources are downloaded in background and will be saved in the cache directory
 * </p>
 *
 * @author Patrick Murris, Tom Gaskins, Antonio Santiago
 * @version $Id: SurfaceImage.java 5244 2008-05-01 01:15:59Z patrickmurris $
 */
public class SurfaceImage implements SurfaceTile, Renderable, Movable
{
    private static final String DEFAULT_CACHE_DIRECTORY = "SurfaceImages";

    private Object imageSource;
    private Sector sector;
    private Position referencePosition;
    private Extent extent;
    private double extentVerticalExaggertion = Double.MIN_VALUE; // VE used to calculate the extent
    private double opacity = 1.0;
    private TextureData textureData = null;
    private boolean reload = false;     // Force texture data to be reloaded
    private boolean useCache = true;
    private boolean loading = false;    // True when image is loading or downloading
    private boolean hasProblem = false; // True when download failed
    private Layer layer;
    private String cacheDirectory = DEFAULT_CACHE_DIRECTORY;

    /**
     * Renders a single image tile from a local or remote network source.
     *
     * @param imageSource can be a local image path, a <code>BufferedImage</code> or a url string pointing to
     *                    an http server.
     * @param sector      the sector covered by the image.
     */
    public SurfaceImage(Object imageSource, Sector sector)
    {
        initialize(imageSource, sector, null, this.cacheDirectory);
    }

    /**
     * Renders a single image tile from a local or remote network source.
     *
     * @param imageSource can be a local image path, a <code>BufferedImage</code> or a url string pointing to
     *                    an http server.
     * @param sector      the sector covered by the image.
     * @param layer       a reference to the layer handling this image. This layer will fire an event when the image has
     *                    finished downloading.
     */
    public SurfaceImage(Object imageSource, Sector sector, Layer layer)
    {
        initialize(imageSource, sector, layer, this.cacheDirectory);
    }

    /**
     * Renders a single image tile from a local or remote network source.
     *
     * @param imageSource    can be a local image path, a <code>BufferedImage</code> or a url string pointing to
     *                       an http server.
     * @param sector         the sector covered by the image.
     * @param layer          a reference to the layer handling this image. This layer will fire an event when the image has
     *                       finished downloading.
     * @param cacheDirectory the cache directory where the downloaded image should be saved and retrieved.
     */
    public SurfaceImage(Object imageSource, Sector sector, Layer layer, String cacheDirectory)
    {
        initialize(imageSource, sector, layer, cacheDirectory);
    }

    private void initialize(Object imageSource, Sector sector, Layer layer, String cacheDirectory)
    {
        if (imageSource == null)
        {
            String message = Logging.getMessage("nullValue.ImageSourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (cacheDirectory == null)
        {
            String message = Logging.getMessage("nullValue.DirectionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.imageSource = imageSource;
        this.sector = sector;
        this.referencePosition = new Position(sector.getCentroid(), 0);
        this.layer = layer;
        this.cacheDirectory = cacheDirectory;
    }

    /**
     * Get the image source object. It can be a <code>String</code> containing a path to either a
     * local file or a networked file. It can also be a <code>BufferedImage</code>.
     *
     * @return the image source object.
     */
    public Object getImageSource()
    {
        return imageSource;
    }

    public Sector getSector()
    {
        return this.sector;
    }

    /**
     * Sets the sector for the image allowing to change its size or position.
     *
     * @param sector the new sector.
     */
    public void setSector(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.sector = sector;
        this.extent = null;
    }

    /**
     * Returns if the image is loading texture data.
     *
     * @return true if the image data is being loaded.
     */
    public boolean isLoading()
    {
        return this.loading;
    }

    /**
     * Returns whether there was any problem loading texture data.
     *
     * @return true if image data failed to download - or other problems.
     */
    public boolean hasProblem()
    {
        return this.hasProblem;
    }

    /**
     * Force texture data to be reloaded.
     *
     * @param useCache true if data should be reloaded from the cache.
     * @return true if reloading has been succesfully scheduled.
     */
    public boolean reload(boolean useCache)
    {
        if (this.loading)
            return false;

        this.reload = true;
        this.useCache = useCache;
        this.loading = false;
        this.hasProblem = false;

        return true;
    }

    public Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.extent == null || this.extentVerticalExaggertion != dc.getVerticalExaggeration())
        {
            this.extent = dc.getGlobe().computeBoundingCylinder(dc.getVerticalExaggeration(), this.getSector());
            this.extentVerticalExaggertion = dc.getVerticalExaggeration();
        }

        return this.extent;
    }

    public double getOpacity()
    {
        return opacity;
    }

    public void setOpacity(double opacity)
    {
        this.opacity = opacity;
    }

    /**
     * Get the layer reference to which this <code>SurfaceImage</code> belongs. May be <code>null</code>
     *
     * @return the layer reference.
     */
    public Layer getLayer()
    {
        return this.layer;
    }

    private void setTexture(TextureCache tc, Texture texture)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        tc.put(this.imageSource, texture);
    }

    private Texture getTexture(TextureCache tc)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return tc.get(this.imageSource);
    }

    private Texture initializeTexture(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = null;

        if (this.imageSource instanceof String)
        {
            String path = (String) this.imageSource;
            if (path.toLowerCase().startsWith("http"))
            {
                // Handle remote file
                if (this.loading)
                    return null;
                if (this.textureData != null && !this.reload)
                {
                    t = TextureIO.newTexture(this.textureData);
                }
                else if (!this.hasProblem)
                {
                    sendLoadRequests(path);
                    return null;
                }
            }
            else
            {
                // Handle local file or resource
                Object streamOrException = WWIO.getFileOrResourceAsStream(path, this.getClass());
                if (streamOrException == null || streamOrException instanceof Exception)
                {
                    Logging.logger().log(Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile",
                        streamOrException != null ? streamOrException : "");
                    return null;
                }
                try
                {
                    t = TextureIO.newTexture((InputStream) streamOrException, true, null);
                }
                catch (Exception e)
                {
                    Logging.logger().log(java.util.logging.Level.SEVERE,
                        "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
                    return null;
                }
            }
        }
        else if (this.imageSource instanceof BufferedImage)
        {
            try
            {
                t = TextureIO.newTexture((BufferedImage) this.imageSource, true);
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.IOExceptionDuringTextureInitialization");
                Logging.logger().log(Level.SEVERE, msg, e);
                return null;
            }
        }
        else
        {
            Logging.logger().log(java.util.logging.Level.SEVERE, "SurfaceImage.UnknownSourceType",
                this.imageSource.getClass().getName());
            return null;
        }

        if (t == null) // In case JOGL TextureIO returned null
        {
            Logging.logger().log(java.util.logging.Level.SEVERE, "generic.TextureUnreadable",
                this.imageSource instanceof String ? this.imageSource : this.imageSource.getClass().getName());
            return null;
        }

        // Textures with the same path are assumed to be identical textures, so key the texture id off the
        // image source.
        this.setTexture(dc.getTextureCache(), t);
        t.bind();

        GL gl = dc.getGL();
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);//_MIPMAP_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

        return t;
    }

    public boolean bind(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = this.getTexture(dc.getTextureCache());
        if (t == null || this.reload)
        {
            t = this.initializeTexture(dc);
            if (t != null)
                return true; // texture was bound during initialization.
        }

        if (t != null)
            t.bind();

        return t != null;
    }

    public void applyInternalTransform(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // Use the tile's texture if available.
        Texture t = this.getTexture(dc.getTextureCache());
        if (t == null)
            t = this.initializeTexture(dc);

        if (t != null)
        {
            if (t.getMustFlipVertically())
            {
                GL gl = GLContext.getCurrent().getGL();
                gl.glMatrixMode(GL.GL_TEXTURE);
                gl.glLoadIdentity();
                gl.glScaled(1, -1, 1);
                gl.glTranslated(0, -1, 0);
            }
        }
    }

    // Render the surface image tile
    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!this.sector.intersects(dc.getVisibleSector()))
            return;

        GL gl = dc.getGL();
        try
        {
            if (!dc.isPickingMode())
            {
                double opacity = this.layer != null ? this.getOpacity() * this.layer.getOpacity() : this.getOpacity();

                if (opacity < 1)
                {
                    gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT | GL.GL_CURRENT_BIT);
                    gl.glColor4d(1d, 1d, 1d, opacity);
                }
                else
                {
                    gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT);
                }
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
            else
            {
                gl.glPushAttrib(GL.GL_POLYGON_BIT);
            }

            gl.glPolygonMode(GL.GL_FRONT, GL.GL_FILL);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            dc.getGeographicSurfaceTileRenderer().renderTile(dc, this);
        }
        finally
        {
            gl.glPopAttrib();
        }
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || this.getClass() != o.getClass())
            return false;

        SurfaceImage that = (SurfaceImage) o;
        return imageSource.equals(that.imageSource) && sector.equals(that.sector);
    }

    public int hashCode()
    {
        int result;
        result = imageSource.hashCode();
        result = 31 * result + sector.hashCode();
        return result;
    }

    private void sendLoadRequests(String path)
    {
        if (WorldWind.getTaskService().isFull())
            return;

        this.reload = false;
        this.loading = true;
        WorldWind.getTaskService().addTask(new RequestTask(path));
    }

    private class RequestTask implements Runnable
    {
        private final String path;

        public RequestTask(String path)
        {
            this.path = path;
        }

        public void run()
        {
            final java.net.URL textureURL = WorldWind.getDataFileCache().findFile(getCachePath(path), false);

            if (textureURL != null && SurfaceImage.this.useCache)
            {
                // Load cached texture
                loadCachedTexture(textureURL);
            }
            else
            {
                // Download texture
                downloadTexture(path);
            }
        }

        private boolean loadCachedTexture(java.net.URL textureURL)
        {
            // TODO: handle expiration date/time
/*            if (WWIO.isFileOutOfDate(textureURL, 0))
            {
                // The file has expired. Delete it then request download of newer.
                gov.nasa.worldwind.WorldWind.getDataFileCache().removeFile(textureURL);
                String message = Logging.getMessage("generic.DataFileExpired", textureURL);
                Logging.logger().fine(message);
            }
*/

            TextureData textureData = null;
            try
            {
                textureData = TextureIO.newTextureData(textureURL, true, null);
                SurfaceImage.this.textureData = textureData;
                SurfaceImage.this.hasProblem = false;
                if (SurfaceImage.this.layer != null)
                    SurfaceImage.this.layer.firePropertyChange(AVKey.LAYER, null, this);
            }
            catch (Exception e)
            {
                Logging.logger().log(java.util.logging.Level.SEVERE,
                    "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
                SurfaceImage.this.hasProblem = true;
            }

            SurfaceImage.this.loading = false;

            return textureData != null;
        }

        private void downloadTexture(final String path)
        {
            if (!WorldWind.getRetrievalService().isAvailable())
                return;

            try
            {
                URL url = new URL(path);
                if ("http".equalsIgnoreCase(url.getProtocol()))
                {
                    // Download asynchronously
                    if (WorldWind.getNetworkStatus().isHostUnavailable(url))
                        return;

                    Retriever retriever = new HTTPRetriever(url, new DownloadPostProcessor());

                    // Apply any overridden timeouts from the layer.
                    if (SurfaceImage.this.layer != null)
                    {
                        Integer cto = AVListImpl.getIntegerValue(SurfaceImage.this.layer,
                            AVKey.URL_CONNECT_TIMEOUT);
                        if (cto != null && cto > 0)
                            retriever.setConnectTimeout(cto);
                        Integer cro = AVListImpl.getIntegerValue(SurfaceImage.this.layer,
                            AVKey.URL_READ_TIMEOUT);
                        if (cro != null && cro > 0)
                            retriever.setReadTimeout(cro);
                        Integer srl = AVListImpl.getIntegerValue(SurfaceImage.this.layer,
                            AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
                        if (srl != null && srl > 0)
                            retriever.setStaleRequestLimit(srl);
                    }

                    WorldWind.getRetrievalService().runRetriever(retriever);
                }
                else
                {
                    SurfaceImage.this.loading = false;
                    SurfaceImage.this.hasProblem = true;
                    Logging.logger().severe(Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", path));
                }
            }
            catch (MalformedURLException ex)
            {
                SurfaceImage.this.loading = false;
                SurfaceImage.this.hasProblem = true;
                Logging.logger().severe(Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", path));
            }
        }
    }

    private class DownloadPostProcessor implements RetrievalPostProcessor
    {
        public ByteBuffer run(Retriever retriever)
        {
            if (retriever == null)
            {
                // Missing data.
                SurfaceImage.this.loading = false;
                SurfaceImage.this.hasProblem = true;
                String msg = Logging.getMessage("nullValue.RetrieverIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
            {
                // Missing data.
                SurfaceImage.this.loading = false;
                SurfaceImage.this.hasProblem = true;
                return null;
            }

            HTTPRetriever htr = (HTTPRetriever) retriever;
            if (htr.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT)
            {
                // Missing data.
                SurfaceImage.this.loading = false;
                SurfaceImage.this.hasProblem = true;
                return null;
            }
            else if (htr.getResponseCode() != HttpURLConnection.HTTP_OK)
            {
                // Missing data.
                SurfaceImage.this.loading = false;
                SurfaceImage.this.hasProblem = true;
                return null;
            }

            URLRetriever r = (URLRetriever) retriever;
            ByteBuffer buffer = r.getBuffer();
            if (buffer != null)
            {
                try
                {
                    // Store file in the cache
                    String name = SurfaceImage.this.getCachePath(htr.getUrl().toString());
                    final File outFile = WorldWind.getDataFileCache().newFile(name);
                    if (outFile == null)
                    {
                        String msg = Logging.getMessage("generic.CantCreateCacheFile", name);
                        Logging.logger().warning(msg);
                        return null;
                    }
                    else
                    {
                        WWIO.saveBuffer(buffer, outFile);
                    }

                    SurfaceImage.this.textureData = TextureIO.newTextureData(
                        new ByteArrayInputStream(buffer.array()), true, null);
                    SurfaceImage.this.hasProblem = false;
                    // Fire layer event
                    if (SurfaceImage.this.layer != null)
                        SurfaceImage.this.layer.firePropertyChange(AVKey.LAYER, null, this);
                }
                catch (Exception e)
                {
                    Logging.logger().log(java.util.logging.Level.SEVERE, Logging.getMessage(
                        "layers.TextureLayer.ExceptionSavingRetrievedTextureFile", htr.getUrl().toString()), e);
                    SurfaceImage.this.hasProblem = true;
                }
            }
            SurfaceImage.this.loading = false;
            return null;
        }
    }

    private String getCachePath(String path)
    {
        try
        {
            URL url = new URL(path);
            if (DEFAULT_CACHE_DIRECTORY.equals(this.cacheDirectory))
                return this.cacheDirectory + "/" + WWIO.formPath(url.getHost()) + url.getPath();
            return this.cacheDirectory + "/" + new File(url.getFile()).getName();
        }
        catch (MalformedURLException ex)
        {
            return WWIO.formPath(this.cacheDirectory, path);
        }
    }

    public void move(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Increase the current sector position.
        double minlat = this.sector.getMinLatitude().getDegrees();
        double minlon = this.sector.getMinLongitude().getDegrees();
        double maxlat = this.sector.getMaxLatitude().getDegrees();
        double maxlon = this.sector.getMaxLongitude().getDegrees();

        double poslat = position.getLatitude().getDegrees();
        double poslon = position.getLongitude().getDegrees();

        minlat += poslat;
        maxlat += poslat;
        minlon += poslon;
        maxlon += poslon;

        // Check new values don't exceed the limits.
        if (maxlat > 90 || maxlat < -90 || minlat > 90 || minlat < -90 ||
            maxlon > 180 || maxlon < -180 || minlon > 180 || minlon < -180)
        {
            return;
        }

        this.referencePosition.add(position);
        setSector(Sector.fromDegrees(minlat, maxlat, minlon, maxlon));
    }

    public void moveTo(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Calculate new position
        double poslat = position.getLatitude().getDegrees();
        double poslon = position.getLongitude().getDegrees();

        double halfDeltaLat = this.sector.getDeltaLatDegrees() / 2;
        double halfDeltaLon = this.sector.getDeltaLonDegrees() / 2;

        double minlat = poslat - halfDeltaLat;
        double maxlat = poslat + halfDeltaLat;
        double minlon = poslon - halfDeltaLon;
        double maxlon = poslon + halfDeltaLon;

        // Check new values don't exceed the limits.
        if (maxlat > 90 || maxlat < -90 || minlat > 90 || minlat < -90 ||
            maxlon > 180 || maxlon < -180 || minlon > 180 || minlon < -180)
        {
            return;
        }

        this.referencePosition = position;
        setSector(Sector.fromDegrees(minlat, maxlat, minlon, maxlon));
    }

    public Position getReferencePosition()
    {
        return this.referencePosition;
    }
}
