/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.formats.gpx.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.tracks.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: Tracks.java 5274 2008-05-02 00:42:42Z tgaskins $
 */
public class Tracks extends ApplicationTemplate
{
    private static final String TRACK_FILE = "demodata/tuolumne.gpx";

    private static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            super(true, true, false);

            TrackMarkerLayer layer = this.buildTracksLayer();
            layer.setMaterial(Material.WHITE);
//            layer.setMarkerShape("Cylinder");
            insertBeforeCompass(this.getWwd(), layer);
            this.getLayerPanel().update(this.getWwd());

            this.getWwd().addSelectListener(new SelectListener()
            {
                public void selected(SelectEvent event)
                {
                    if (event.getTopObject() != null)
                    {
                        if (event.getTopPickedObject().getParentLayer() instanceof TrackLayer)
                        {
                            PickedObject po = event.getTopPickedObject();
                            System.out.printf("Track position %s, %s\n", po.getValue(AVKey.PICKED_OBJECT_ID).toString(),
                                po.getPosition());
                        }
                    }
                }
            });

        }

        private TrackMarkerLayer buildTracksLayer()
        {
            try
            {
                GpxReader reader = new GpxReader();
                reader.readFile(TRACK_FILE);
                List<Track> tracks = reader.getTracks();
                TrackMarkerLayer layer = new TrackMarkerLayer(tracks);
                layer.setOverrideElevation(true);
                layer.setElevation(0);
                return layer;
            }
            catch (ParserConfigurationException e)
            {
                e.printStackTrace();
            }
            catch (SAXException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Tracks", AppFrame.class);
    }
}
