/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Mars;

import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.avlist.AVKey;

/**
 * Base (one image) layer for Mars MOC Colorized dataset.
 * @author Patrick Murris
 * @version $Id: MOCColorizedBaseLayer.java 5186 2008-04-26 15:41:44Z patrickmurris $
 */
public class MOCColorizedBaseLayer extends RenderableLayer
{
    public MOCColorizedBaseLayer()
    {
        String path = Configuration.getStringValue(AVKey.MARS_MOCCOLOR_ONE_IMAGE_PATH);
        if (path == null)
        {
            String message = Logging.getMessage("layers.Mars.MOCColorBaseLayer.PathNotGiven");
            throw new IllegalStateException(message);
        }

        this.setName(Logging.getMessage("layers.Mars.MOCColorBaseLayer.Name"));
        this.addRenderable(new SurfaceImage(path, Sector.FULL_SPHERE, this, "Mars/MOC_Colorized_Base"));

        // Disable picking for the layer because it covers the full sphere and will override a terrain pick.
        this.setPickEnabled(false);
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Mars.MOCColorBaseLayer.Name");
    }
}
