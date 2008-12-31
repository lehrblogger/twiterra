/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package localhost;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.examples.ClickAndGoSelectListener;
import gov.nasa.worldwind.examples.LayerPanel;
import gov.nasa.worldwind.examples.StatisticsPanel;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a base application framework for simple WorldWind examples. Although this class will run stand-alone, it is
 * not meant to be used that way. But it has a main method to show how a derived class would call it.
 *
 * @version $Id: ApplicationTemplate.java 5176 2008-04-25 21:31:06Z patrickmurris $
 */
public class TwiTerraApp
{
    
    protected static class AppFrame extends JFrame
    {
    	private Dimension canvasSize;
        private TwiTerraAppPanel wwjPanel;
        private LayerPanel layerPanel;
        private StatisticsPanel statsPanel;

        public AppFrame()
        {	
        	this.setUndecorated(true);
        	this.setExtendedState(Frame.MAXIMIZED_BOTH);
        	Toolkit tk = Toolkit.getDefaultToolkit();
    		int xSize = ((int) tk.getScreenSize().getWidth());
    		int ySize = ((int) tk.getScreenSize().getHeight());
    		
            this.initialize(xSize, ySize + 20, false, false, false);
        }

        public AppFrame(int width, int height, boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel)
        {
            this.initialize(width, height, includeStatusBar, includeLayerPanel, includeStatsPanel);
        }

        private void initialize(int width, int height, boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel)
        {
            // Create the WorldWindow.
        	this.canvasSize = new Dimension(width, height);
            this.wwjPanel = new TwiTerraAppPanel(canvasSize, includeStatusBar);
            this.wwjPanel.setPreferredSize(canvasSize);

            // Put the pieces together.
            this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
            if (includeLayerPanel)
            {
                this.layerPanel = new LayerPanel(this.wwjPanel.getWwd(), null);
                this.getContentPane().add(this.layerPanel, BorderLayout.WEST);
            }
            if (includeStatsPanel)
            {
                this.statsPanel = new StatisticsPanel(this.wwjPanel.getWwd(), new Dimension(250, canvasSize.height));
                this.getContentPane().add(this.statsPanel, BorderLayout.EAST);
                this.wwjPanel.getWwd().addRenderingListener(new RenderingListener()
                {
                    public void stageChanged(RenderingEvent event)
                    {
                        if (event.getSource() instanceof WorldWindow)
                        {
                            EventQueue.invokeLater(new Runnable()
                            {
                                public void run()
                                {
                                    statsPanel.update(wwjPanel.getWwd());
                                }
                            });
                        }
                    }
                });
            }
            this.pack();

            // Center the application on the screen.
            Dimension prefSize = this.getPreferredSize();
            Dimension parentSize;
            java.awt.Point parentLocation = new java.awt.Point(0, 0);
            parentSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = parentLocation.x + (parentSize.width - prefSize.width) / 2;
            int y = parentLocation.y + (parentSize.height - prefSize.height) / 2;
            this.setLocation(x, y);
            this.setResizable(true);
        }

        public Dimension getCanvasSize()
        {
            return canvasSize;
        }

        public TwiTerraAppPanel getWwjPanel()
        {
            return wwjPanel;
        }

        public WorldWindowGLCanvas getWwd()
        {
            return this.wwjPanel.getWwd();
        }

        public StatusBar getStatusBar()
        {
            return this.wwjPanel.getStatusBar();
        }

        public LayerPanel getLayerPanel()
        {
            return layerPanel;
        }

        public StatisticsPanel getStatsPanel()
        {
            return statsPanel;
        }
    }

    public static void insertBeforeCompass(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just before the compass.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof CompassLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }

    public static void insertBeforePlacenames(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just before the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof PlaceNameLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }

    public static void insertAfterPlacenames(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just after the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof PlaceNameLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition + 1, layer);
    }

    public static void insertBeforeLayerName(WorldWindow wwd, Layer layer, String targetName) {
        // Insert the layer into the layer list just before the target layer.
        int targetPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l.getName().indexOf(targetName) != -1)
            {
                targetPosition = layers.indexOf(l);
                break;
            }
        }
        layers.add(targetPosition, layer);
    }

    static
    {
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "TwiTerra");
            //System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        }
    }

    public static void start(String appName, Class appFrameClass)
    {
        if (Configuration.isMacOS() && appName != null)
        {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        }

        try
        {
            final AppFrame frame = (AppFrame) appFrameClass.newInstance();
            //frame.setTitle(appName);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.setVisible(true);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        // Call the static start method like this from the main method of your derived class.
        // Substitute your application's name for the first argument.
        TwiTerraApp.start("TwiTerra - revealing how people use Twitter to share and re-share ideas, building connections that encircle the world - http://twiterra.com", AppFrame.class);
    }
}
