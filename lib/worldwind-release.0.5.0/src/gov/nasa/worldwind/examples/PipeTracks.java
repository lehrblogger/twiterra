/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.formats.gpx.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.tracks.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: PipeTracks.java 4840 2008-03-28 04:59:19Z tgaskins $
 */
public class PipeTracks extends ApplicationTemplate
{
    private static final String TRACK_FILE1 = "demodata/PipeTrackTest.gpx";
    private static final String TRACK_FILE2 = "demodata/PipeTracks2.gpx";

    private static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame() throws IOException, ParserConfigurationException, SAXException
        {
            super(true, true, false);

            TrackPipesLayer layer = this.buildTracksLayer(TRACK_FILE1);
            layer.setPipeMaterial(Material.WHITE);
            layer.setJunctionMaterial(Material.RED);
            insertBeforeCompass(this.getWwd(), layer);

            layer = this.buildTracksLayer(TRACK_FILE2);
            layer.setPipeMaterial(Material.GREEN);
            layer.setJunctionMaterial(Material.YELLOW);
            insertBeforeCompass(this.getWwd(), layer);
        }

        private TrackPipesLayer buildTracksLayer(String fileName)
            throws IOException, SAXException, ParserConfigurationException
        {
            GpxReader reader = new GpxReader();
            reader.readFile(fileName);
            List<Track> tracks = reader.getTracks();
            return new TrackPipesLayer(tracks);
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Pipe Tracks", AppFrame.class);
    }
}
