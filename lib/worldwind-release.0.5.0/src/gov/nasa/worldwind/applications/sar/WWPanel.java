/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.TerrainProfileLayer;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.avlist.AVKey;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * @author tag
 * @version $Id: WWPanel.java 5176 2008-04-25 21:31:06Z patrickmurris $
 */
public class WWPanel extends JPanel
{
    private WorldWindowGLCanvas wwd;
    private StatusBar statusBar;

    private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener()
    {
        @SuppressWarnings({"StringEquality"})
        public void propertyChange(PropertyChangeEvent propertyChangeEvent)
        {
            if (propertyChangeEvent.getPropertyName() == SAR2.ELEVATION_UNIT)
                updateElevationUnit(propertyChangeEvent.getNewValue());
        }
    };

    public WWPanel()
    {
        super(new BorderLayout());

        this.wwd = new WorldWindowGLCanvas();
        this.wwd.setPreferredSize(new Dimension(800, 800));

        // Create the default model as described in the current worldwind properties.
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        this.wwd.setModel(m);

        this.wwd.addPropertyChangeListener(this.propertyChangeListener);

        this.add(this.wwd, BorderLayout.CENTER);

        this.statusBar = new StatusBar();
        this.add(statusBar, BorderLayout.PAGE_END);
        this.statusBar.setEventSource(wwd);
    }

    public WorldWindowGLCanvas getWwd()
    {
        return wwd;
    }

    public StatusBar getStatusBar()
    {
        return statusBar;
    }

    private void updateElevationUnit(Object newValue)
    {
        for (Layer layer : this.wwd.getModel().getLayers())
        {
            if (layer instanceof ScalebarLayer)
            {
                if (SAR2.UNIT_IMPERIAL.equals(newValue))
                    ((ScalebarLayer) layer).setUnit(ScalebarLayer.UNIT_IMPERIAL);
                else // Default to metric units.
                    ((ScalebarLayer) layer).setUnit(ScalebarLayer.UNIT_METRIC);
            }
            else if (layer instanceof TerrainProfileLayer)
            {
                if (SAR2.UNIT_IMPERIAL.equals(newValue))
                    ((TerrainProfileLayer) layer).setUnit(TerrainProfileLayer.UNIT_IMPERIAL);
                else // Default to metric units.
                    ((TerrainProfileLayer) layer).setUnit(TerrainProfileLayer.UNIT_METRIC);
            }
        }

        if (SAR2.UNIT_IMPERIAL.equals(newValue))
            this.statusBar.setElevationUnit(StatusBar.UNIT_IMPERIAL);
        else // Default to metric units.
            this.statusBar.setElevationUnit(StatusBar.UNIT_METRIC);
    }
}
