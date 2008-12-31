/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;

import javax.swing.*;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: LayerMenu.java 3872 2007-12-11 01:18:37Z tgaskins $
 */
public class LayerMenu extends JMenu
{
    private WorldWindow wwd;

    public LayerMenu()
    {
        super("Layers");
    }

    public WorldWindow getWwd()
    {
        return wwd;
    }

    public void setWwd(WorldWindow wwd)
    {
        this.wwd = wwd;
        for (Layer layer : this.wwd.getModel().getLayers())
        {
            JCheckBoxMenuItem mi = new JCheckBoxMenuItem(new LayerVisibilityAction(this.wwd, layer));
            mi.setState(layer.isEnabled());
            this.add(mi);
        }
    }

    private static class LayerVisibilityAction extends AbstractAction
    {
        private final Layer layer;
        private final WorldWindow wwd;

        public LayerVisibilityAction(WorldWindow wwd, Layer layer)
        {
            super(layer.getName());
            this.layer = layer;
            this.wwd = wwd;
        }

        public void actionPerformed(ActionEvent actionEvent)
        {
            layer.setEnabled(((JCheckBoxMenuItem) actionEvent.getSource()).getState());
            this.wwd.redraw();
        }
    }
}
