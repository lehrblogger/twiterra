/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.avlist.AVKey;

/**
 * @author tag
 * @version $Id$
 */
public class BMNGOneImage extends RenderableLayer
{
    public BMNGOneImage()
    {
        String path = Configuration.getStringValue(AVKey.BMNG_ONE_IMAGE_PATH);
        if (path == null)
        {
            String message = Logging.getMessage("layers.BMNGOne.PathNotGiven");
            throw new IllegalStateException(message);
        }

        this.setName(Logging.getMessage("layers.Earth.BlueMarbleOneImageLayer.Name"));
        this.addRenderable(new SurfaceImage(path, Sector.FULL_SPHERE));

        // Disable picking for the layer because it covers the full sphere and will override a terrain pick.
        this.setPickEnabled(false);
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Earth.BlueMarbleOneImageLayer.Name");
    }
}
