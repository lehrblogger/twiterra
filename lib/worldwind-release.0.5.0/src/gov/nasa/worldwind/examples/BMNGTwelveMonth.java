/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Earth.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.render.SurfaceImage;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Shows twelve month of Blue Marble Next Generation for the year 2004.
 *
 * @author Patrick Murris
 * @version $Id: BMNGTwelveMonth.java 5279 2008-05-02 20:49:52Z patrickmurris $
 * @see gov.nasa.worldwind.render.SurfaceImage
 * @see gov.nasa.worldwind.layers.Earth.BMNGWMSLayer
 */
public class BMNGTwelveMonth extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        private static final String BMNG_BASE_URL = "http://worldwind28.arc.nasa.gov/public/world.topo.bathy.";
        private static final String BMNG_IMAGE_SUFFIX = ".jpg";
        private static final int DEFAULT_ANIMATION_DELAY = 5000; // 5 sec
        private static final int REDRAW_TIMER_DELAY = 500;  // 1/2 sec

        private Layer[] BMNGBaseLayers;
        private Layer[] BMNGTiledLayers;
        private int baseLayerID = -1;
        private int tiledLayerID = -1;
        private int month = 5;
        private Timer monthAnimator;
        private JSlider monthSlider;
        @SuppressWarnings({"FieldCanBeLocal"})
        private Timer redrawTimer;

        public AppFrame()
        {
            super(true, true, false);

            // Find Blue Marble layers index numbers
            LayerList layers = this.getWwd().getModel().getLayers();
            for(int i = 0; i < layers.size(); i++)
            {
                if(layers.get(i) instanceof BMNGOneImage)
                    baseLayerID = i;
                else if(layers.get(i) instanceof BMNGSurfaceLayer ||
                        layers.get(i) instanceof BMNGWMSLayer)
                    tiledLayerID = i;
                else if(layers.get(i) instanceof EarthNASAPlaceNameLayer ||
                        layers.get(i) instanceof NASAWFSPlaceNameLayer)
                    layers.get(i).setEnabled(false);  // turn off placenames
            }

            // Instantiate the twelve layers (base and tiled)
            BMNGBaseLayers = new Layer[12];
            BMNGTiledLayers = new Layer[12];
            for (int i = 1; i <= 12; i++)
            {
                // Tiled layer
                TiledImageLayer tiledLayer = new BMNGWMSLayer(i);
                tiledLayer.setMaxActiveAltitude(10000e3);
                BMNGTiledLayers[i - 1] = tiledLayer;
                // Base layer
                RenderableLayer baselayer = new RenderableLayer();
                baselayer.setName("BMNG Base " + String.format("%02d-2004", i));
                baselayer.setPickEnabled(false);
                // Use a surface image from a NASA server
                baselayer.addRenderable(new SurfaceImage(
                        BMNG_BASE_URL + String.format("2004%02d", i) + BMNG_IMAGE_SUFFIX,
                        Sector.FULL_SPHERE, baselayer, "Earth/BMNG Twelve Month/"));
                BMNGBaseLayers[i - 1] = baselayer;
            }

            // Update layer list with current month
            update();

            // Setup month animator timer
            this.monthAnimator = new Timer(DEFAULT_ANIMATION_DELAY, new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    monthSlider.setValue(month < 12 ? month + 1 : 1);
                }

            });
            this.monthAnimator.setInitialDelay(0);

            // Setup and start redraw timer - to force downloads to completion without user interaction
            this.redrawTimer = new Timer(REDRAW_TIMER_DELAY, new ActionListener() 
            {
                public void actionPerformed(ActionEvent event)
                {
                    getWwd().redraw();
                }

            });
            this.redrawTimer.start();

            // Add control panel
            this.getLayerPanel().add(makeControlPanel(),  BorderLayout.SOUTH);
        }

        private JPanel makeControlPanel()
        {
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

            // Month combo
            JPanel comboPanel = new JPanel(new GridLayout(0, 2, 0, 0));
            comboPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            comboPanel.add(new JLabel("Month:"));

            // Month slider
            JPanel sliderPanel = new JPanel(new GridLayout(0, 1, 0, 0));
            sliderPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            monthSlider = new JSlider(1, 12, 5);
            monthSlider.setPaintTicks(true);
            monthSlider.setPaintLabels(true);
            monthSlider.setMajorTickSpacing(1);
            monthSlider.setSnapToTicks(true);
            monthSlider.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent event)
                {
                    month = monthSlider.getValue();
                    update();
                }
            });
            sliderPanel.add(monthSlider);

            // Animation start/stop button
            JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 0, 0));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            final JButton animateButton = new JButton("Animate");
            animateButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    if(monthAnimator.isRunning())
                    {
                        monthAnimator.stop();
                        animateButton.setText("Animate");
                    }
                    else
                    {
                        monthAnimator.start();
                        animateButton.setText("Stop");
                    }
                }
            });
            buttonPanel.add(animateButton);

            // Speed slider
            JPanel speedPanel = new JPanel();
            speedPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            speedPanel.setLayout(new BoxLayout(speedPanel, BoxLayout.X_AXIS));
            speedPanel.add(new JLabel("Speed:"));
            final JSlider speedSlider = new JSlider(0, 10, 2);
            speedSlider.setPaintTicks(true);
            speedSlider.setPaintLabels(true);
            speedSlider.setMajorTickSpacing(1);
            double speed = speedSlider.getMaximum()
                    - (double)(DEFAULT_ANIMATION_DELAY - 100) / 6e3 * speedSlider.getMaximum();
            speedSlider.setValue((int)speed);
            speedSlider.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent event)
                {
                    double delay = (double)(speedSlider.getMaximum() - speedSlider.getValue())
                            / (double)speedSlider.getMaximum() * 6e3 + 100;
                    monthAnimator.setDelay((int)delay);
                }
            });
            speedPanel.add(speedSlider);

            // Help text
            JPanel helpPanel = new JPanel(new GridLayout(0, 1, 0, 0));
            helpPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            JLabel line1 = new JLabel("Tip: go through the twelve month once");
            line1.setAlignmentX(SwingConstants.CENTER);
            JLabel line2 = new JLabel("before increasing speed.");
            line2.setAlignmentX(SwingConstants.CENTER);
            helpPanel.add(line1);
            helpPanel.add(line2);

            // Control panel assembly
            controlPanel.add(comboPanel);
            controlPanel.add(sliderPanel);
            controlPanel.add(buttonPanel);
            controlPanel.add(speedPanel);
            controlPanel.add(helpPanel);
            controlPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9),
                    new TitledBorder("Blue Marble Next Generation 2004")));
            controlPanel.setToolTipText("Set the current BMNG month");
            return controlPanel;
        }

        // Update worldwind layer list
        private void update()
        {
            LayerList layers = this.getWwd().getModel().getLayers();
            layers.remove(tiledLayerID); // remove tiled first
            layers.remove(baseLayerID);
            layers.add(baseLayerID, BMNGBaseLayers[month - 1]);
            layers.add(tiledLayerID, BMNGTiledLayers[month - 1]);
            this.getLayerPanel().update(this.getWwd());
            this.getWwd().redraw();
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Blue Marble Twelve Month 2004", AppFrame.class);
    }
}
