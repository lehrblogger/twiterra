/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.applications.sar;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
 * @author tag
 * @version $Id: TrackPanel.java 4808 2008-03-25 23:57:35Z dcollins $
 */
public class TrackPanel extends JPanel
{
    private String elevationUnit;

    public TrackPanel()
    {
        initComponents();
        this.scrollPane.addMouseListener(new PositionsContextMenu(this.positionTable));
        this.positionTable.addMouseListener(new PositionsContextMenu(this.positionTable));
    }

    public void setTrack(SARTrack sarTrack)
    {
        this.positionTable.setSarTrack(sarTrack);
    }

    public SARTrack getTrack()
    {
        return this.positionTable.getSarTrack();
    }

    public String getElevationUnit()
    {
        return this.elevationUnit;
    }

    public void setElevationUnit(String unit)
    {
        String oldValue = this.elevationUnit;
        this.elevationUnit = unit;
        
        this.positionTable.setElevationUnit(unit);
        this.positionTable.updateTableData();
        this.changeOffsetUnit(oldValue, this.elevationUnit);
    }

    private void enterPositionsItemStateChanged(ItemEvent e)
    {
        String request = e.getStateChange() == ItemEvent.SELECTED
            ? TrackController.BEGIN_TRACK_POINT_ENTRY : TrackController.END_TRACK_POINT_ENTRY;
        this.positionTable.getSarTrack().firePropertyChange(request, null, this.positionTable.getSarTrack());
    }

    private void visibilityActionPerformed(ActionEvent e)
    {
        String vis = this.visibilityFlag.isSelected() ? TrackController.TRACK_ENABLE : TrackController.TRACK_DISABLE;
        this.positionTable.getSarTrack().firePropertyChange(vis, null, this.positionTable.getSarTrack());
    }

    private boolean ignoreOffsetChange = false;

    private void offsetSliderStateChanged(ChangeEvent e)
    {
        if (this.ignoreOffsetChange)
        {
            this.ignoreOffsetChange = false;
            return;
        }

        int offset = this.offsetToggleCheckBox.isSelected() ? this.offsetSlider.getValue() : 0;
        this.offsetReadout.setText(Integer.toString(offset));
        applyTrackOffset(offset);
    }

    private void offsetToggleCheckBoxItemStateChanged(ItemEvent e)
    {
        double offset = this.offsetToggleCheckBox.isSelected() ? Double.parseDouble(this.offsetReadout.getText()) : 0d;
        applyTrackOffset(offset);
    }

    private void offsetReadoutActionPerformed(ActionEvent e)
    {
        double offset = this.offsetToggleCheckBox.isSelected() ? Double.parseDouble(this.offsetReadout.getText()) : 0d;
        this.offsetReadout.setText(Integer.toString((int) offset));

        this.ignoreOffsetChange = true;
        if (offset >= this.offsetSlider.getMaximum())
            this.offsetSlider.setValue(this.offsetSlider.getMaximum());
        else if (offset <= this.offsetSlider.getMinimum())
            this.offsetSlider.setValue(this.offsetSlider.getMinimum());
        else
            this.offsetSlider.setValue((int) offset);

        applyTrackOffset(offset);
    }

    private void applyTrackOffset(double offset)
    {
        // The actual track offset will always be in meters. If the
        // user is working in imperial units, convert the slider
        // value to meters before passing it to SarTrack.
        double trackOffset;
        if (SAR2.UNIT_IMPERIAL.equals(this.elevationUnit))
            trackOffset = SAR2.feetToMeters(offset);
        else // Default to metric units.
            trackOffset = offset;

        this.positionTable.getSarTrack().setOffset(trackOffset);
        this.positionTable.getSarTrack().firePropertyChange(TrackController.TRACK_MODIFY, null,
            this.positionTable.getSarTrack());
    }

    private void initComponents()
    {
        this.panel1 = new JPanel();
        this.topPanel = new JPanel();
        this.visibilityFlag = new JCheckBox();
        this.checkBox1 = new JCheckBox();
        this.scrollPane = new JScrollPane();
        this.positionTable = new PositionTable();
        this.offsetPanel = new JPanel();
        this.offsetSlider = new JSlider();
        this.panel2 = new JPanel();
        this.offsetToggleCheckBox = new JCheckBox();
        this.offsetReadout = new JTextField();

        //======== this ========
        setToolTipText("Track Positions");
        setBackground(Color.white);
        setLayout(new BorderLayout());

        //======== panel1 ========
        {
            this.panel1.setLayout(new BorderLayout());

            //======== topPanel ========
            {
                this.topPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
                this.topPanel.setLayout(new BorderLayout(5, 5));

                //---- visibilityFlag ----
                this.visibilityFlag.setText("Visible");
                this.visibilityFlag.setSelected(true);
                this.visibilityFlag.setToolTipText("Display track on globe");
                this.visibilityFlag.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        visibilityActionPerformed(e);
                    }
                });
                this.topPanel.add(this.visibilityFlag, BorderLayout.WEST);

                //---- checkBox1 ----
                this.checkBox1.setText("Mouse Entry (+ALT)");
                this.checkBox1.setToolTipText("Enter track points with mouse");
                this.checkBox1.addItemListener(new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        enterPositionsItemStateChanged(e);
                    }
                });
                this.topPanel.add(this.checkBox1, BorderLayout.CENTER);
            }
            this.panel1.add(this.topPanel, BorderLayout.NORTH);

            //======== scrollPane ========
            {
                this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

                //---- positionTable ----
                this.positionTable.setPreferredScrollableViewportSize(new Dimension(340, 300));
                this.scrollPane.setViewportView(this.positionTable);
            }
            this.panel1.add(this.scrollPane, BorderLayout.CENTER);
        }
        add(this.panel1, BorderLayout.CENTER);

        //======== offsetPanel ========
        {
            this.offsetPanel.setLayout(new BorderLayout(0, 2));

            //---- offsetSlider ----
            this.offsetSlider.setOrientation(SwingConstants.VERTICAL);
            this.offsetSlider.setPaintLabels(true);            
            this.offsetSlider.setValue(0);
            this.offsetSlider.setToolTipText("Elevation offset");
            this.offsetSlider.setPaintTicks(true);
            this.offsetSlider.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    offsetSliderStateChanged(e);
                }
            });
            this.offsetPanel.add(this.offsetSlider, BorderLayout.CENTER);

            //======== panel2 ========
            {
                this.panel2.setBorder(new EmptyBorder(0, 2, 2, 2));
                this.panel2.setLayout(new BorderLayout());

                //---- offsetToggleCheckBox ----
                this.offsetToggleCheckBox.setText("Offset");
                this.offsetToggleCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
                this.offsetToggleCheckBox.setSelected(true);
                this.offsetToggleCheckBox.addItemListener(new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        offsetToggleCheckBoxItemStateChanged(e);
                    }
                });
                this.panel2.add(this.offsetToggleCheckBox, BorderLayout.NORTH);

                //---- offsetReadout ----
                this.offsetReadout.setText("0");
                this.offsetReadout.setHorizontalAlignment(SwingConstants.RIGHT);
                this.offsetReadout.setBorder(new CompoundBorder(
                    new EtchedBorder(),
                    new EmptyBorder(0, 0, 0, 4)));
                this.offsetReadout.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        offsetReadoutActionPerformed(e);
                    }
                });
                this.panel2.add(this.offsetReadout, BorderLayout.SOUTH);
            }
            this.offsetPanel.add(this.panel2, BorderLayout.NORTH);
        }
        add(this.offsetPanel, BorderLayout.EAST);
    }

    private void changeOffsetUnit(String oldUnit, String newUnit)
    {
        int value = this.offsetSlider.getValue();

        //---- offsetSlider ----
        if (SAR2.UNIT_IMPERIAL.equals(newUnit))
        {
            this.offsetSlider.setMaximum(16000);
            this.offsetSlider.setMinimum(-16000);
            this.offsetSlider.setMajorTickSpacing(3200);
            Hashtable ht = this.offsetSlider.createStandardLabels(3200);
            this.offsetSlider.setLabelTable(ht);
            if (SAR2.UNIT_METRIC.equals(oldUnit))
                value = (int) SAR2.metersToFeet(value);
        }
        else // Default to metric units.
        {
            this.offsetSlider.setMaximum(5000);
            this.offsetSlider.setMinimum(-5000);
            this.offsetSlider.setMajorTickSpacing(1000);
            Hashtable ht = this.offsetSlider.createStandardLabels(1000);
            this.offsetSlider.setLabelTable(ht);
            if (SAR2.UNIT_IMPERIAL.equals(oldUnit))
                value = (int) SAR2.feetToMeters(value);
        }
        if (value > this.offsetSlider.getMaximum())
            value = this.offsetSlider.getMaximum();
        if (value < this.offsetSlider.getMinimum())
            value = this.offsetSlider.getMinimum();
        this.offsetSlider.setValue(value);

        //---- offsetReadout ----
        this.offsetReadout.setText(Integer.toString(value));
    }

    private JPanel panel1;
    private JPanel topPanel;
    private JCheckBox visibilityFlag;
    private JCheckBox checkBox1;
    private JScrollPane scrollPane;
    private PositionTable positionTable;
    private JPanel offsetPanel;
    private JSlider offsetSlider;
    private JPanel panel2;
    private JCheckBox offsetToggleCheckBox;
    private JTextField offsetReadout;
}
