/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.globes.FlatGlobe;
import gov.nasa.worldwind.WorldWindow;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Panel to control a flat world projection.
 *
 * @author Patrick Murris
 * @version $Id: FlatWorldPanel.java 5217 2008-04-30 05:08:02Z tgaskins $
 */

public class FlatWorldPanel extends JPanel
{
    private WorldWindow wwd;
    private FlatGlobe globe;
    private String projection;
    private JComboBox projectionCombo;

    public FlatWorldPanel(WorldWindow wwd)
    {
        super(new GridLayout(0, 1, 0, 0));
        this.wwd = wwd;
        this.globe = (FlatGlobe)wwd.getModel().getGlobe();
        this.makePanel();
    }

    private JPanel makePanel()
    {
        JPanel controlPanel = this;

        // Projection combo
        JPanel comboPanel = new JPanel(new GridLayout(0, 2, 0, 0));
        comboPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        comboPanel.add(new JLabel("  Projection:"));
        this.projectionCombo = new JComboBox(new String[] {"Mercator", "Lat-Lon", "Modified Sin.", "Sinusoidal"});
        this.projectionCombo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                String item = (String) projectionCombo.getSelectedItem();

                if(item.equals("Lat-Lon"))
                {
                    projection = FlatGlobe.PROJECTION_LAT_LON;
                }
                else if(item.equals("Mercator"))
                {
                    projection = FlatGlobe.PROJECTION_MERCATOR;
                }
                else if(item.equals("Sinusoidal"))
                {
                    projection = FlatGlobe.PROJECTION_SINUSOIDAL;
                }
                else if(item.equals("Modified Sin."))
                {
                    projection = FlatGlobe.PROJECTION_MODIFIED_SINUSOIDAL;
                }
                update();
            }
        });
        comboPanel.add(this.projectionCombo);

        controlPanel.add(comboPanel);
        controlPanel.setBorder(
            new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Flat World")));
        controlPanel.setToolTipText("Set the current projection");
        return controlPanel;
    }

    // Update globe projection
    private void update()
    {
        // Update globe projection
        this.globe.setProjection(this.projection);
        this.wwd.redraw();
    }

}
