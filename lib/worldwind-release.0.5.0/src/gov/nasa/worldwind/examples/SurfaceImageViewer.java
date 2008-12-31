package gov.nasa.worldwind.examples;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Open and view arbitrary surface images with accompanying world file.
 *
 * @author tag
 * @version $Id: SurfaceImageViewer.java 4960 2008-04-08 08:19:05Z tgaskins $
 */
public class SurfaceImageViewer extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        private JFileChooser fileChooser = new JFileChooser();
        private JSlider opacitySlider;
        private SurfaceImageLayer layer;

        public AppFrame()
        {
            super(true, true, false);

            try
            {
                this.layer = new SurfaceImageLayer();
                this.layer.setOpacity(0.7);
                this.layer.setPickEnabled(false);
                this.layer.setName("Surface Images");

                insertBeforeCompass(this.getWwd(), layer);

                this.getLayerPanel().add(makeControlPanel(), BorderLayout.SOUTH);

                this.getLayerPanel().update(this.getWwd());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        private JPanel makeControlPanel()
        {
            JPanel controlPanel = new JPanel(new GridLayout(0, 1, 5, 5));
            JButton openButton = new JButton("Open Image File...");
            controlPanel.add(openButton);
            openButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    int status = fileChooser.showOpenDialog(AppFrame.this);
                    if (status != JFileChooser.APPROVE_OPTION)
                        return;

                    File imageFile = fileChooser.getSelectedFile();
                    if (imageFile == null)
                        return;

                    try
                    {
                        layer.addImage(imageFile.getAbsolutePath());
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            this.opacitySlider = new JSlider();
            this.opacitySlider.setMaximum(100);
            this.opacitySlider.setValue((int) (layer.getOpacity() * 100));
            this.opacitySlider.setEnabled(true);
            this.opacitySlider.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    int value = opacitySlider.getValue();
                    layer.setOpacity(value / 100d);
                    getWwd().repaint();
                }
            });
            JPanel opacityPanel = new JPanel(new BorderLayout(5, 5));
            opacityPanel.add(new JLabel("Opacity"), BorderLayout.WEST);
            opacityPanel.add(this.opacitySlider, BorderLayout.CENTER);

            controlPanel.add(opacityPanel);

            controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            return controlPanel;
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Surface Images", SurfaceImageViewer.AppFrame.class);
    }
}
