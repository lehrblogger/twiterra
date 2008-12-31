package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.formats.tiff.*;
import gov.nasa.worldwind.util.*;

import javax.imageio.*;
import javax.imageio.spi.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author tag
 * @version $Id: SurfaceImageLayer.java 5202 2008-04-29 00:49:43Z tgaskins $
 */
public class SurfaceImageLayer extends RenderableLayer
{
    static
    {
        IIORegistry reg = IIORegistry.getDefaultInstance();
        reg.registerServiceProvider(GeotiffImageReaderSpi.inst());
    }

    private ConcurrentHashMap<String, SurfaceImage> imageTable = new ConcurrentHashMap<String, SurfaceImage>();

    public void addImage(String imagePath) throws IOException
    {
        if (imagePath == null)
        {
            String message = Logging.getMessage("nullValue.ImageSourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        File imageFile = new File(imagePath);
        BufferedImage image = ImageIO.read(imageFile);

        File worldFile = getWorldFile(imageFile.getAbsoluteFile());
        if (worldFile == null || !worldFile.exists())
        {
            System.out.println("World file for " + imagePath + "does not exist"); // TODO
        }

        Sector sector = decodeWorldFile(worldFile, image.getWidth(), image.getHeight());
        if (sector == null)
        {
            System.out.println("World file for " + imagePath + "can not be decoded"); // TODO
        }

        if (this.imageTable.contains(imagePath))
            this.removeImage(imagePath);

        SurfaceImage si = new SurfaceImage(image, sector);
        si.setOpacity(this.getOpacity());
        this.addRenderable(si);
        this.imageTable.put(imagePath, si);
    }

    public void removeImage(String imagePath)
    {
        SurfaceImage si = this.imageTable.get(imagePath);
        if (si != null)
        {
            this.removeRenderable(si);
            this.imageTable.remove(imagePath);
        }
    }

    @Override
    public void setOpacity(double opacity)
    {
        super.setOpacity(opacity);
        
        for (Map.Entry<String, SurfaceImage> entry : this.imageTable.entrySet())
        {
            entry.getValue().setOpacity(opacity);
        }
    }

    private static File getWorldFile(File imageFile)
    {
        File dir = imageFile.getParentFile();
        final String base = WWIO.replaceSuffix(imageFile.getName(), "");

        File[] wfiles = dir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.startsWith(base) && name.toLowerCase().endsWith("w");
            }
        });
        
        return (wfiles != null && wfiles.length > 0) ? wfiles[0] : null;
    }

    private static Sector decodeWorldFile(File wf, int imageWidth, int imageHeight) throws FileNotFoundException
    {
        Scanner scanner  = new Scanner(wf);

        double[] values = new double[6];

        for (int i = 0; i < 6; i++)
        {
            if (scanner.hasNextDouble())
            {
                values[i] = scanner.nextDouble();
            }
            else
            {
                System.out.println("World file missing value at line " + (i + 1));
                return null;
            }

        }

        Sector sector = parseDegrees(values, imageWidth, imageHeight);

        return sector;
    }

    private static Sector parseDegrees(double[] values, int imageWidth, int imageHeight)
    {
        Angle latOrigin = Angle.fromDegrees(values[5]);
        Angle latOffset = latOrigin.addDegrees(values[3] * imageHeight);
        Angle lonOrigin = Angle.fromDegrees(values[4]);
        Angle lonOffset = lonOrigin.addDegrees(values[0] * imageWidth);

        Angle minLon, maxLon;
        if (lonOrigin.degrees < lonOffset.degrees)
        {
            minLon = lonOrigin;
            maxLon = lonOffset;
        }
        else
        {
            minLon = lonOffset;
            maxLon = lonOrigin;
        }

        Angle minLat, maxLat;
        if (lonOrigin.degrees < lonOffset.degrees)
        {
            minLat = latOrigin;
            maxLat = latOffset;
        }
        else
        {
            minLat = latOffset;
            maxLat = latOrigin;
        }

        return new Sector(minLat, maxLat, minLon, maxLon);
    }
}
