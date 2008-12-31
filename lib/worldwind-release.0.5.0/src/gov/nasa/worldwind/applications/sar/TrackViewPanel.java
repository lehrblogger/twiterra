/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.geom.Position;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * @author tag
 * @version $Id: TrackViewPanel.java 5064 2008-04-16 15:41:01Z dcollins $
 */
public class TrackViewPanel extends JPanel
{
    // SAR logical components.
    private SARTrack sarTrack;
    private String elevationUnit;
    // "View" panel components
    private JCheckBox subsurfaceButton;
    private JRadioButton examineButton;
    private JRadioButton followButton;
    private JRadioButton freeButton;
    private JCheckBox overrideClipDistanceButton;
    private JSlider clipDistanceSlider;
    // "Position" panel components
    private JLabel latLabel;
    private JLabel lonLabel;
    private JLabel altLabel;
    private JLabel latReadout;
    private JLabel lonReadout;
    private JLabel altReadout;
    private JSpinner positionSpinner;
    private JSlider positionSlider;
    private JButton fastReverseButton;
    private JButton reverseButton;
    private JButton stopButton;
    private JButton forwardButton;
    private JButton fastForwardButton;
    private JLabel speedLabel;
    private JSlider speedSlider;
    // "Player" logical components.
    private static final int PLAY_FORWARD = 1;
    private static final int PLAY_BACKWARD = -1;
    private static final int PLAY_STOP = 0;
    private int playMode = PLAY_STOP;
    private Timer player;

    public static final String POSITION_CHANGE = "TrackViewPanel.PositionChange";
    public static final String VIEW_CHANGE = "TrackViewPanel.ViewChange";

    public TrackViewPanel()
    {
        initComponents();
        this.updateEnabledState();
    }

    public void setCurrentTrack(SARTrack sarTrack)
    {
        this.sarTrack = sarTrack;
        if (this.sarTrack != null)
        {
            this.sarTrack.addPropertyChangeListener(new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent propertyChangeEvent)
                {
                    updatePositionList(false);
                }
            });
        }

        this.updatePositionList(true);
        this.updateEnabledState();
    }

    public String getElevationUnit()
    {
        return this.elevationUnit;
    }

    public void setElevationUnit(String elevationUnit)
    {
        this.elevationUnit = elevationUnit;
    }

    private void updatePositionList(boolean resetPosition)
    {
        String[] strings = new String[this.sarTrack != null ? this.sarTrack.size() : 0];

        for (int i = 0; i < strings.length; i++)
            strings[i] = String.format("%,4d", i);

        if (strings.length == 0)
            strings = new String[] {"   0"};

        Object currentSpinnerValue = this.positionSpinner.getValue();
        int currentSliderValue = this.positionSlider.getValue();
        this.positionSpinner.setModel(new SpinnerListModel(strings));
        this.positionSpinner.setValue(resetPosition ? strings[0] : currentSpinnerValue);
        this.positionSlider.setValue(resetPosition ? 0 : currentSliderValue);
    }

    private void setPositionSpinnerNumber(int n)
    {
        this.positionSpinner.setValue(String.format("%,4d", n));
    }

    private void updateEnabledState()
    {
        boolean state = this.sarTrack != null;

        this.examineButton.setEnabled(state);
        this.followButton.setEnabled(state);
        this.positionSpinner.setEnabled(state);
        this.positionSlider.setEnabled(state);
        this.speedLabel.setEnabled(state);
        this.latLabel.setEnabled(state);
        this.lonLabel.setEnabled(state);
        this.altLabel.setEnabled(state);

        this.fastReverseButton.setEnabled(state);
        this.reverseButton.setEnabled(state);
//        this.stopButton.setEnabled(state);
        this.forwardButton.setEnabled(state);
        this.fastForwardButton.setEnabled(state);
        this.speedLabel.setEnabled(state);
        this.speedSlider.setEnabled(state);

        this.updateReadout(this.sarTrack != null && sarTrack.size() > 0 ? sarTrack.get(0) : null);
    }

    private void positionSpinnerStateChanged()
    {
        this.positionSlider.setValue(0);
        this.firePropertyChange(POSITION_CHANGE, -1, 0);
    }

    private void positionSliderStateChanged()
    {
        this.firePropertyChange(POSITION_CHANGE, -1, 0);
    }

    private void examineButtonItemStateChanged()
    {
        this.subsurfaceButton.setSelected(false);
        this.firePropertyChange(VIEW_CHANGE, -1, 0);
    }

    private void followButtonItemStateChanged()
    {
        this.subsurfaceButton.setSelected(true);
        this.firePropertyChange(VIEW_CHANGE, -1, 0);
    }

    private void freeButtonItemStateChanged()
    {
        this.subsurfaceButton.setSelected(false);
        this.firePropertyChange(VIEW_CHANGE, -1, 0);
    }

    private void subsurfaceButtonItemStateChanged()
    {
        this.firePropertyChange(VIEW_CHANGE, -1, 0);
    }

    private void overrideClipDistanceButtonStateChanged()
    {
        this.subsurfaceButton.setEnabled(!this.overrideClipDistanceButton.isSelected());
        this.firePropertyChange(POSITION_CHANGE, -1, 0);
    }

    private void clipDistanceStateChanged()
    {
        this.firePropertyChange(POSITION_CHANGE, -1, 0);
    }

    public int getCurrentPositionNumber()
    {
        Object o = this.positionSpinner.getValue();
        if (o == null)
            return -1;

        return Integer.parseInt(o.toString().trim().replaceAll(",", ""));
    }

    private boolean isLastPosition(int n)
    {
        return n >= this.sarTrack.size() - 1;
    }

    public double getPositionDelta()
    {
        int i = this.positionSlider.getValue();
        int min = this.positionSlider.getMinimum();
        int max = this.positionSlider.getMaximum();
        return (double) i / ((double) max - (double) min);
    }

    public boolean isSubsurfaceOkay()
    {
        return this.subsurfaceButton.isSelected();
    }

    public boolean isExamineViewMode()
    {
        return this.examineButton.isSelected();
    }

    public boolean isFollowViewMode()
    {
        return this.followButton.isSelected();
    }

    public boolean isFreeViewMode()
    {
        return this.freeButton.isSelected();
    }

    public boolean isOverrideClipDistance()
    {
        return this.overrideClipDistanceButton.isSelected();
    }

    public double getClipDistance()
    {
        return this.clipDistanceSlider.getValue();
    }

    public void updateReadout(Position pos)
    {
        this.latReadout.setText(pos == null ? "" : String.format("% 7.4f\u00B0", pos.getLatitude().getDegrees()));
        this.lonReadout.setText(pos == null ? "" : String.format("% 7.4f\u00B0", pos.getLongitude().getDegrees()));

        if (SAR2.UNIT_IMPERIAL.equals(this.elevationUnit))
            this.altReadout.setText(pos == null ? "" : String.format("% 8.0f ft", SAR2.metersToFeet(pos.getElevation())));
        else // Default to metric units.
           this.altReadout.setText(pos == null ? "" : String.format("% 8.0f m", pos.getElevation()));
    }

    // Player Controls

    private void fastReverseButtonActionPerformed()
    {
        if (this.getCurrentPositionNumber() > 0)
            setPositionSpinnerNumber(this.getCurrentPositionNumber() - 1);
    }

    private void reverseButtonActionPerformed()
    {
        setPlayMode(PLAY_BACKWARD);
    }

    private void stopButtonActionPerformed()
    {
        setPlayMode(PLAY_STOP);
    }

    private void forwardButtonActionPerformed()
    {
        setPlayMode(PLAY_FORWARD);
    }

    private void fastForwardButtonActionPerformed()
    {
        if (!isLastPosition(this.getCurrentPositionNumber()))
            setPositionSpinnerNumber(this.getCurrentPositionNumber() + 1);
    }

    public boolean isPlayerActive()
    {
        return this.playMode != PLAY_STOP;
    }

    private void setPlayMode(int mode)
    {
        this.playMode = mode;
        if (player == null)
            initPlayer();
        player.start();
    }

    private void initPlayer()
    {
        if (player != null)
            return;

        player = new Timer(50, new ActionListener()
        {
            // Animate the view motion by controlling the positionSpinner and segmentSlider
            public void actionPerformed(ActionEvent actionEvent)
            {
                runPlayer();    
            }
        });
    }

    private void runPlayer()
    {
        int curPosition = getCurrentPositionNumber();
        int curValue = this.positionSlider.getValue();
        int valueSpeed = this.speedSlider.getValue() / 10;

        if (this.playMode == PLAY_STOP)
        {
            this.stopButton.setEnabled(false);
            this.player.stop();
        }
        else if (this.playMode == PLAY_FORWARD)
        {
            this.stopButton.setEnabled(true);
            if (curPosition >= (this.sarTrack.size() - 1))
            {
                this.playMode = PLAY_STOP;
                setPositionSpinnerNumber(this.sarTrack.size() - 1);
            }
            else
            {
                int nextValue = curValue + valueSpeed;
                if (nextValue > this.positionSlider.getMaximum())
                {
                    int nextPosition = curPosition + 1;
                    if (nextPosition >= (this.sarTrack.size() - 1))
                    {
                        this.playMode = PLAY_STOP;
                        setPositionSpinnerNumber(this.sarTrack.size() - 1);
                    }
                    else
                    {
                        setPositionSpinnerNumber(nextPosition);
                    }
                }
                else
                {
                    this.positionSlider.setValue(nextValue);
                }
            }
        }
        else if (this.playMode == PLAY_BACKWARD)
        {
            this.stopButton.setEnabled(true);
            if (curPosition >= (this.sarTrack.size() - 1))
            {
                setPositionSpinnerNumber(curPosition - 1);
                this.positionSlider.setValue(this.positionSlider.getMaximum());
            }
            else
            {
                int nextValue = curValue - valueSpeed;
                if (nextValue < this.positionSlider.getMinimum())
                {
                    int nextPosition = curPosition - 1;
                    if (nextPosition < 0)
                    {
                        this.playMode = PLAY_STOP;
                        setPositionSpinnerNumber(0);
                        this.positionSlider.setValue(0);
                    }
                    else
                    {
                        setPositionSpinnerNumber(nextPosition);
                        this.positionSlider.setValue(this.positionSlider.getMaximum());
                    }
                }
                else
                {
                    this.positionSlider.setValue(nextValue);
                }
            }
        }
    }

    private void initComponents()
    {
        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        //======== "View" Section ========
        Box viewPanel = Box.createVerticalBox();
        {
            //viewBox.setBorder(new EmptyBorder(5, 10, 12, 5));
            viewPanel.setBorder(new CompoundBorder(
                new TitledBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.GRAY), "View"),
                new EmptyBorder(5, 5, 5, 5)));

            //======== View Mode Panel ========
            Box modePanel = Box.createHorizontalBox();
            modePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            {
                //---- "Subsurface" Button ----
                this.subsurfaceButton = new JCheckBox();
                this.subsurfaceButton.setText("Subsurface");
                this.subsurfaceButton.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        subsurfaceButtonItemStateChanged();
                    }
                });
                modePanel.add(this.subsurfaceButton);
                modePanel.add(Box.createHorizontalStrut(20));

                //---- "EXAMINE" Button ----
                this.examineButton = new JRadioButton();
                this.examineButton.setText("EXAMINE");
                this.examineButton.setHorizontalAlignment(SwingConstants.CENTER);
                this.examineButton.setEnabled(false);
                this.examineButton.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        examineButtonItemStateChanged();
                    }
                });
                modePanel.add(this.examineButton);
                modePanel.add(Box.createHorizontalStrut(20));

                //---- "FLY-IT" Button ----
                this.followButton = new JRadioButton();
                this.followButton.setText("FLY-IT");
                this.followButton.setHorizontalAlignment(SwingConstants.CENTER);
                this.followButton.setEnabled(false);
                this.followButton.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        followButtonItemStateChanged();
                    }
                });
                modePanel.add(this.followButton);
                modePanel.add(Box.createHorizontalStrut(20));

                //---- "FREE" Button ----
                this.freeButton = new JRadioButton();
                this.freeButton.setText("FREE");
                this.freeButton.setSelected(true);
                this.freeButton.setHorizontalAlignment(SwingConstants.CENTER);
                this.freeButton.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        freeButtonItemStateChanged();
                    }
                });
                modePanel.add(this.freeButton);

                //--------
                modePanel.add(Box.createHorizontalGlue());

                //---- View Mode ButtonGroup ----
                ButtonGroup viewModeButtonGroup = new ButtonGroup();
                viewModeButtonGroup.add(this.examineButton);
                viewModeButtonGroup.add(this.followButton);
                viewModeButtonGroup.add(this.freeButton);
            }
            viewPanel.add(modePanel);
            viewPanel.add(Box.createVerticalStrut(10));

            //======== Clip Control Panel ========
            Box clipPanel = Box.createHorizontalBox();
            clipPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            {
                //---- "Override Clip Distance" Button ----
                this.overrideClipDistanceButton = new JCheckBox();
                this.overrideClipDistanceButton.setText("Override Clip Distance:");
                this.overrideClipDistanceButton.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        overrideClipDistanceButtonStateChanged();
                    }
                });
                clipPanel.add(this.overrideClipDistanceButton);
                clipPanel.add(Box.createHorizontalStrut(3));

                int minDistance = 10;
                int maxDistance = 100000;
                this.clipDistanceSlider = new JSlider();
                this.clipDistanceSlider.setMinimum(minDistance);
                this.clipDistanceSlider.setMaximum(maxDistance);
                this.clipDistanceSlider.setValue(minDistance + (maxDistance - minDistance) / 2);
                this.clipDistanceSlider.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent event) {
                        clipDistanceStateChanged();
                    }
                });
                clipPanel.add(this.clipDistanceSlider);
            }
            viewPanel.add(clipPanel);
        }
        add(viewPanel);

        //======== "Position" Section ========
        Box positionPanel = Box.createVerticalBox();
        positionPanel.setBorder(new CompoundBorder(
                new TitledBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.GRAY), "Position"),
                new EmptyBorder(5, 5, 5, 5)));
        {
            //======== Position Readout ========
            JPanel readoutPanel = new JPanel();
            readoutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            readoutPanel.setLayout(new GridLayout(1, 3));
            {
                //======== Latitude ========
                Box lat = Box.createHorizontalBox();
                {
                    this.latLabel = new JLabel();
                    this.latLabel.setText("Lat:");
                    lat.add(this.latLabel);
                    lat.add(Box.createHorizontalStrut(3));

                    this.latReadout = new JLabel();
                    this.latReadout.setText("-90.0000");
                    lat.add(this.latReadout);
                    lat.add(Box.createHorizontalGlue());                    
                }
                readoutPanel.add(lat);

                //======== Longitude ========
                Box lon = Box.createHorizontalBox();
                {
                    this.lonLabel = new JLabel();
                    this.lonLabel.setText("Lon:");
                    lon.add(this.lonLabel);
                    lon.add(Box.createHorizontalStrut(3));

                    //---- lonReadout ----
                    this.lonReadout = new JLabel();
                    this.lonReadout.setText("-180.0000");
                    lon.add(this.lonReadout);
                    lon.add(Box.createHorizontalGlue());
                }
                readoutPanel.add(lon);

                //======== Altitude ========
                Box alt = Box.createHorizontalBox();
                {
                    this.altLabel = new JLabel();
                    this.altLabel.setText("Alt:");
                    alt.add(this.altLabel);
                    alt.add(Box.createHorizontalStrut(3));

                    this.altReadout = new JLabel();
                    this.altReadout.setText("50,000.000");
                    alt.add(this.altReadout);
                    alt.add(Box.createHorizontalGlue());
                }
                readoutPanel.add(alt);
            }
            positionPanel.add(readoutPanel);
            positionPanel.add(Box.createVerticalStrut(5));

            //======== Position Spinner, Slider ========
            JPanel positionControlPanel = new JPanel();
            positionControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            positionControlPanel.setLayout(new BorderLayout(5, 0));
            {
                //---- Position Spinner ----
                this.positionSpinner = new JSpinner();
                this.positionSpinner.setModel(new SpinnerListModel(new String[] {"   0"}));
                this.positionSpinner.setEnabled(false);
                this.positionSpinner.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        positionSpinnerStateChanged();
                    }
                });
                positionControlPanel.add(this.positionSpinner, BorderLayout.WEST);

                //---- Position Slider ----
                this.positionSlider = new JSlider();
                this.positionSlider.setMaximum(1000);
                this.positionSlider.setValue(0);
                this.positionSlider.setEnabled(false);
                this.positionSlider.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        positionSliderStateChanged();
                    }
                });
                positionControlPanel.add(this.positionSlider, BorderLayout.CENTER);
            }
            positionPanel.add(positionControlPanel);
            positionPanel.add(Box.createVerticalStrut(10));

            //======== "VCR" Panel ========
            Box vcrPanel = Box.createHorizontalBox();
            vcrPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            {
                //---- "<<" Button ----
                this.fastReverseButton = new JButton();
                this.fastReverseButton.setText("<<");
                this.fastReverseButton.setEnabled(false);
                this.fastReverseButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        fastReverseButtonActionPerformed();
                    }
                });
                vcrPanel.add(this.fastReverseButton);
                vcrPanel.add(Box.createHorizontalStrut(3));

                //---- "<" Button----
                this.reverseButton = new JButton();
                this.reverseButton.setText("<");
                this.reverseButton.setEnabled(false);
                this.reverseButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        reverseButtonActionPerformed();
                    }
                });
                vcrPanel.add(this.reverseButton);
                vcrPanel.add(Box.createHorizontalStrut(3));

                //---- "Stop" Button ----
                this.stopButton = new JButton();
                this.stopButton.setText("Stop");
                this.stopButton.setEnabled(false);
                this.stopButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        stopButtonActionPerformed();
                    }
                });
                vcrPanel.add(this.stopButton);
                vcrPanel.add(Box.createHorizontalStrut(3));

                //---- ">" Button ----
                this.forwardButton = new JButton();
                this.forwardButton.setText(">");
                this.forwardButton.setBorder(UIManager.getBorder("Button.border"));
                this.forwardButton.setEnabled(false);
                this.forwardButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        forwardButtonActionPerformed();
                    }
                });
                vcrPanel.add(this.forwardButton);
                vcrPanel.add(Box.createHorizontalStrut(3));

                //---- ">>" Button ----
                this.fastForwardButton = new JButton();
                this.fastForwardButton.setText(">>");
                this.fastForwardButton.setEnabled(false);
                this.fastForwardButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        fastForwardButtonActionPerformed();
                    }
                });
                vcrPanel.add(this.fastForwardButton);

                //--------
                vcrPanel.add(Box.createHorizontalGlue());
            }
            positionPanel.add(vcrPanel);
            positionPanel.add(Box.createVerticalStrut(5));

            //======== "Speed" Panel ========
            Box speedPanel = Box.createHorizontalBox();
            speedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            {
                //---- "Speed:" Label ----
                this.speedLabel = new JLabel();
                this.speedLabel.setText("Speed:");
                speedPanel.add(this.speedLabel);
                speedPanel.add(Box.createHorizontalStrut(5));

                //---- Speed Slider ----
                this.speedSlider = new JSlider();
                this.speedSlider.setMaximum(200);
                this.speedSlider.setMajorTickSpacing(50);
                this.speedSlider.setPaintLabels(true);
                this.speedSlider.setValue(20);
                this.speedSlider.setEnabled(false);
                speedPanel.add(this.speedSlider);
            }
            positionPanel.add(speedPanel);
        }
        add(positionPanel);
    }
}
