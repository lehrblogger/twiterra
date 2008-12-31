/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;

/**
 * @author tag
 * @version $Id: TextureTile.java 5215 2008-04-30 04:37:46Z tgaskins $
 */
public class TextureTile extends Tile implements SurfaceTile
{
    private volatile TextureData textureData;
    private TextureTile fallbackTile = null; // holds texture to use if own texture not available
    private Vec4 centroid; // Cartesian coordinate of lat/lon center
    private Extent extent = null; // bounding volume
    private double extentVerticalExaggertion = Double.MIN_VALUE; // VE used to calculate the extent
    private Globe globe;
    private Object globeStateKey;
    private double minDistanceToEye = Double.MAX_VALUE;
    private boolean usingMipmaps = false;

    public TextureTile(Sector sector)
    {
        super(sector);
    }

    public TextureTile(Sector sector, Level level, int row, int col)
    {
        super(sector, level, row, col);
    }

    @Override
    public final long getSizeInBytes()
    {
        long size = super.getSizeInBytes();

        if (this.textureData != null)
            size += this.textureData.getEstimatedMemorySize();

        return size;
    }

    public TextureTile getFallbackTile()
    {
        return this.fallbackTile;
    }

    public void setFallbackTile(TextureTile fallbackTile)
    {
        this.fallbackTile = fallbackTile;
    }

    public TextureData getTextureData()
    {
        return this.textureData;
    }

    public void setTextureData(TextureData textureData)
    {
        this.textureData = textureData;
        if (textureData.getMipmapData() != null)
            this.usingMipmaps = true;
    }

    public Texture getTexture(TextureCache tc)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return tc.get(this.getTileKey());
    }

    public boolean isTextureInMemory(TextureCache tc)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return this.getTexture(tc) != null || this.getTextureData() != null;
    }

    public void setTexture(TextureCache tc, Texture texture)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        tc.put(this.getTileKey(), texture);

        // No more need for texture data; allow garbage collector and memory cache to reclaim it.
        this.textureData = null;
        this.updateMemoryCache();
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

    public double getMinDistanceToEye()
    {
        return this.minDistanceToEye;
    }

    public void setMinDistanceToEye(double minDistanceToEye)
    {
        if (minDistanceToEye < 0)
        {
            String msg = Logging.getMessage("layers.TextureTile.MinDistanceToEyeNegative");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.minDistanceToEye = minDistanceToEye;
    }

    public Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.extent == null || !this.isExtentValid(dc))
        {
            this.extent = dc.getGlobe().computeBoundingCylinder(dc.getVerticalExaggeration(), this.getSector());
            this.extentVerticalExaggertion = dc.getVerticalExaggeration();
            this.globe = dc.getGlobe();
            this.globeStateKey = this.globe != null ? this.globe.getStateKey() : null;
            this.centroid = null;
        }

        return this.extent;
    }

    private boolean isExtentValid(DrawContext dc)
    {
        return !(dc.getGlobe() == null || this.globe == null || this.globeStateKey == null)
            && this.extentVerticalExaggertion == dc.getVerticalExaggeration()
            && this.globe == dc.getGlobe()
            && this.globeStateKey.equals(dc.getGlobe().getStateKey());

    }

    public TextureTile[] createSubTiles(Level nextLevel)
    {
        if (nextLevel == null)
        {
            String msg = Logging.getMessage("nullValue.LevelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        Angle p0 = this.getSector().getMinLatitude();
        Angle p2 = this.getSector().getMaxLatitude();
        Angle p1 = Angle.midAngle(p0, p2);

        Angle t0 = this.getSector().getMinLongitude();
        Angle t2 = this.getSector().getMaxLongitude();
        Angle t1 = Angle.midAngle(t0, t2);

        String nextLevelCacheName = nextLevel.getCacheName();
        int nextLevelNum = nextLevel.getLevelNumber();
        int row = this.getRow();
        int col = this.getColumn();

        TextureTile[] subTiles = new TextureTile[4];

        TileKey key = new TileKey(nextLevelNum, 2 * row, 2 * col, nextLevelCacheName);
        TextureTile subTile = this.getTileFromMemoryCache(key);
        if (subTile != null)
            subTiles[0] = subTile;
        else
            subTiles[0] = new TextureTile(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col);

        key = new TileKey(nextLevelNum, 2 * row, 2 * col + 1, nextLevelCacheName);
        subTile = this.getTileFromMemoryCache(key);
        if (subTile != null)
            subTiles[1] = subTile;
        else
            subTiles[1] = new TextureTile(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1);

        key = new TileKey(nextLevelNum, 2 * row + 1, 2 * col, nextLevelCacheName);
        subTile = this.getTileFromMemoryCache(key);
        if (subTile != null)
            subTiles[2] = subTile;
        else
            subTiles[2] = new TextureTile(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col);

        key = new TileKey(nextLevelNum, 2 * row + 1, 2 * col + 1, nextLevelCacheName);
        subTile = this.getTileFromMemoryCache(key);
        if (subTile != null)
            subTiles[3] = subTile;
        else
            subTiles[3] = new TextureTile(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1);

        return subTiles;
    }

    private TextureTile getTileFromMemoryCache(TileKey tileKey)
    {
        return (TextureTile) WorldWind.getMemoryCache(TextureTile.class.getName()).getObject(tileKey);
    }

    private void updateMemoryCache()
    {
        if (this.getTileFromMemoryCache(this.getTileKey()) != null)
            WorldWind.getMemoryCache(TextureTile.class.getName()).add(this.getTileKey(), this);
    }

    private Texture initializeTexture(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = this.getTexture(dc.getTextureCache());
        if (t != null)
            return t;

        if (this.getTextureData() == null)
        {
            String msg = Logging.getMessage("nullValue.TextureDataIsNull");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        try
        {
            t = TextureIO.newTexture(this.getTextureData());
        }
        catch (Exception e)
        {
            Logging.logger().log(
                java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
            return null;
        }

        this.setTexture(dc.getTextureCache(), t);
        t.bind();

        GL gl = dc.getGL();
        if (this.usingMipmaps && this.getSector().getMaxLatitude().degrees < 80d
            && this.getSector().getMinLatitude().degrees > -80)
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
        else
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
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
        if (t == null && this.getTextureData() != null)
        {
            t = this.initializeTexture(dc);
            if (t != null)
                return true; // texture was bound during initialization.
        }

        if (t == null && this.getFallbackTile() != null)
        {
            TextureTile resourceTile = this.getFallbackTile();
            t = resourceTile.getTexture(dc.getTextureCache());
            if (t == null)
            {
                t = resourceTile.initializeTexture(dc);
                if (t != null)
                    return true; // texture was bound during initialization.
            }
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
        if (t == null && this.getTextureData() != null)
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
            return;
        }

        // Use the tile's fallback texture if its primary texture is not available.
        TextureTile resourceTile = this.getFallbackTile();
        if (resourceTile == null) // no fallback specified
            return;

        t = resourceTile.getTexture(dc.getTextureCache());
        if (t == null && resourceTile.getTextureData() != null)
            t = resourceTile.initializeTexture(dc);

        if (t == null) // was not able to initialize the fallback texture
            return;

        // Apply necessary transforms to the fallback texture.
        GL gl = GLContext.getCurrent().getGL();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glLoadIdentity();

        if (t.getMustFlipVertically())
        {
            gl.glScaled(1, -1, 1);
            gl.glTranslated(0, -1, 0);
        }

        this.applyResourceTextureTransform(dc);
    }

    private void applyResourceTextureTransform(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (this.getLevel() == null)
            return;

        int levelDelta = this.getLevelNumber() - this.getFallbackTile().getLevelNumber();
        if (levelDelta <= 0)
            return;

        double twoToTheN = Math.pow(2, levelDelta);
        double oneOverTwoToTheN = 1 / twoToTheN;

        double sShift = oneOverTwoToTheN * (this.getColumn() % twoToTheN);
        double tShift = oneOverTwoToTheN * (this.getRow() % twoToTheN);

        dc.getGL().glTranslated(sShift, tShift, 0);
        dc.getGL().glScaled(oneOverTwoToTheN, oneOverTwoToTheN, 1);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final TextureTile tile = (TextureTile) o;

        return !(this.getTileKey() != null ? !this.getTileKey().equals(tile.getTileKey()) : tile.getTileKey() != null);
    }

    @Override
    public int hashCode()
    {
        return (this.getTileKey() != null ? this.getTileKey().hashCode() : 0);
    }

    @Override
    public String toString()
    {
        return this.getSector().toString();
    }
}
