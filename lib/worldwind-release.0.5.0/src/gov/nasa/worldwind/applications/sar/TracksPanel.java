/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.applications.sar;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;
import java.beans.*;
import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: TracksPanel.java 4919 2008-04-04 17:32:58Z dcollins $
 */
public class TracksPanel extends JPanel
{
    private String elevationUnit;

    public TracksPanel()
    {
        initComponents();
    }

    private void initComponents()
    {
        this.tracksTabbedPane = new JTabbedPane();

        //======== this ========
        setLayout(new BorderLayout());

        //======== tracksTabbedPane ========
        {
            this.tracksTabbedPane.setMinimumSize(new Dimension(361, 223));
            this.tracksTabbedPane.setPreferredSize(new Dimension(361, 223));
            this.tracksTabbedPane.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    tracksTabbedPaneStateChanged(e);
                }
            });
        }
        add(this.tracksTabbedPane, BorderLayout.CENTER);
    }

    private JTabbedPane tracksTabbedPane;

    public SARTrack getCurrentTrack()
    {
        Component c = this.tracksTabbedPane.getSelectedComponent();
        return c != null ? ((TrackPanel) c).getTrack() : null;
    }

    public Iterable<SARTrack> getAllTracks()
    {
        ArrayList<SARTrack> tracks = new ArrayList<SARTrack>();
        for (int i = 0; i < this.tracksTabbedPane.getTabCount(); i++)
        {
            TrackPanel tp = (TrackPanel) this.tracksTabbedPane.getComponentAt(i);
            if (tp.getTrack() != null)
                tracks.add(tp.getTrack());
        }
        return tracks;
    }

    public void addTrack(SARTrack track)
    {
        TrackPanel tp = new TrackPanel();
        tp.setTrack(track);
        tp.setElevationUnit(this.elevationUnit);
        this.tracksTabbedPane.addTab(track.getName(), makeColorCircle(track.getColor()), tp);
        track.addPropertyChangeListener(new PropertyChangeListener()
        {
            @SuppressWarnings({"StringEquality"})
            public void propertyChange(PropertyChangeEvent propertyChangeEvent)
            {
                if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_REMOVE)
                    removeTrack((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_NAME)
                    renameTrack((SARTrack) propertyChangeEvent.getSource());
                else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_DIRTY_BIT)
                    updateTrackDirty((SARTrack) propertyChangeEvent.getSource());
            }
        });
        this.tracksTabbedPane.setSelectedComponent(tp);
    }

    public String getElevationUnit()
    {
        return this.elevationUnit;
    }

    public void setElevationUnit(String unit)
    {
        this.elevationUnit = unit;
        
        for (int i = 0; i < this.tracksTabbedPane.getTabCount(); i++)
        {
            TrackPanel tp = (TrackPanel) this.tracksTabbedPane.getComponentAt(i);
            tp.setElevationUnit(unit);
        }
    }

    private void removeTrack(SARTrack track)
    {
        TrackPanel tp = this.getTrackPanel(track);
        if (tp != null)
            this.tracksTabbedPane.remove(tp);
    }

    private void renameTrack(SARTrack track)
    {
        int index = getTrackPanelIndex(track);
        if (index != -1)
            this.tracksTabbedPane.setTitleAt(index, track.getName());
    }

    private void updateTrackDirty(SARTrack track)
    {
        int index = getTrackPanelIndex(track);
        if (index != -1)
            this.tracksTabbedPane.setTitleAt(index, track.getName() + (track.isDirty() ? "*" : ""));
    }

    private TrackPanel getTrackPanel(SARTrack track)
    {
        for (int i = 0; i < this.tracksTabbedPane.getTabCount(); i++)
        {
            TrackPanel tp = (TrackPanel) this.tracksTabbedPane.getComponentAt(i);
            if (tp.getTrack() == track)
                return tp;
        }
        return null;
    }

    private int getTrackPanelIndex(SARTrack track)
    {
        for (int i = 0; i < this.tracksTabbedPane.getTabCount(); i++)
        {
            TrackPanel tp = (TrackPanel) this.tracksTabbedPane.getComponentAt(i);
            if (tp.getTrack() == track)
                return i;
        }
        return -1;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void tracksTabbedPaneStateChanged(ChangeEvent e)
    {
        SARTrack track = this.getCurrentTrack();
        if (track == null)
            return;

        track.firePropertyChange(TrackController.TRACK_CURRENT, null, track);
    }

    private static Icon makeColorCircle(Color color)
    {
        BufferedImage bi = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics gr = bi.getGraphics();
        gr.setColor(color);
        gr.fillRoundRect(0, 0, 15, 15, 12, 12);

        return new ImageIcon(bi);
    }
}
