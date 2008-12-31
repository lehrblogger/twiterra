/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.rpf.wizard.RPFImportWizard;
import gov.nasa.worldwind.layers.rpf.wizard.RPFWizardUtil;
import gov.nasa.worldwind.util.wizard.Wizard;
import gov.nasa.worldwind.WorldWindow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Application demonstrating how to import and view local imagery in WWJ.
 * Currently only RPF (CADRG and CIB) data is supported.
 * 
 * <h5>How to view RPF imagery</h5>
 * <ol>
 *   <li>Click "Import CADRG/CIB".</li>
 *   <li>A dialog will appear.</li>
 *   <li>Select a folder to search for data. This should be a folder you know contains RPF data.</li>
 *   <li>Click "Next".</li>
 *   <li>Select the data series you want to import.
 *       Note: A new RPF layer will be created for each data series selected.</li>
 *   <li>Click "Next".</li>
 *   <li>Wait for preprocessing to complete for each data series you selected.</li>
 *   <li>Click "Finish".</li>
 * </ol>
 *
 * <h5>Key RPF features</h5>
 * <ul>
 *   <li>Wizard UI walks user through process of selecting, preprocessing, and importing RPF imagery.</li>
 *   <li>Preprocessing enables the layer to create images spanning thousands of RPF files very quickly.</li>
 *   <li>Impact of preprocessed data on users hard drive is constant - all selected files are preprocessed.
 *       These files are roughly equivalent in size to the original data.</li>
 *   <li>Imagery is created only when user views it, and then it is stored in the WWJ file cache.</li>
 *   <li>Impact of actual imagery on users hard drive is commensurate to areas the user has viewed.</li>
 * </ul>
 *
 * @author dcollins
 * @version $Id: LocallyGeneratedImagery.java 5259 2008-05-01 18:36:07Z dcollins $
 */
public class LocallyGeneratedImagery extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        @SuppressWarnings({"FieldCanBeLocal"})
        private RPFPanel rpfPanel;

        public AppFrame()
        {
            super(true, false, false);
            initComponents();
        }

        private void initComponents()
        {
            this.rpfPanel = new RPFPanel(getWwd());
            getContentPane().add(this.rpfPanel, BorderLayout.WEST);
        }
    }

    private static class RPFPanel extends JPanel
    {
        private LayerPanel layerPanel;
        private WorldWindow wwd;

        public RPFPanel(WorldWindow wwd)
        {
            this.wwd = wwd;
            initComponents();
        }

        private void onImportRPFPressed()
        {
            runRPFImportWizard(this.wwd);
            this.layerPanel.update(this.wwd);

        }

        private void initComponents()
        {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 0, 10, 0));

            JPanel btnPanel = new JPanel();
            btnPanel.setLayout(new BorderLayout());
            btnPanel.setBorder(new EmptyBorder(20, 10, 20, 10));
            JButton importBtn = new JButton("Import CADRG/CIB");
            importBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    onImportRPFPressed();
                }
            });
            btnPanel.add(importBtn, BorderLayout.CENTER);
            add(btnPanel, BorderLayout.SOUTH);

            this.layerPanel = new LayerPanel(this.wwd, null);
            add(this.layerPanel, BorderLayout.CENTER);
        }
    }

    public static void runRPFImportWizard(WorldWindow wwd)
    {
        RPFImportWizard wizard = new RPFImportWizard();
        wizard.setTitle("Import CADRG/CIB Imagery");
        wizard.getDialog().setPreferredSize(new Dimension(500, 400));
        centerComponentOnScreen(wizard.getDialog());

        int returnCode = wizard.showModalDialog();
        if (returnCode == Wizard.FINISH_RETURN_CODE)
        {
            List<Layer> layerList = RPFWizardUtil.getLayerList(wizard.getModel());
            if (layerList != null)
            {
                for (Layer layer : layerList)
                {
                    ApplicationTemplate.insertBeforePlacenames(wwd, layer);
                }
            }
        }
    }

    private static void centerComponentOnScreen(Component c)
    {
        // Center the application on the screen.
        Dimension prefSize = c.getPreferredSize();
        java.awt.Point parentLocation = new java.awt.Point(0, 0);
        Dimension parentSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = parentLocation.x + (parentSize.width - prefSize.width) / 2;
        int y = parentLocation.y + (parentSize.height - prefSize.height) / 2;
        c.setLocation(x, y);
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Locally Generated Imagery", AppFrame.class);
    }
}
