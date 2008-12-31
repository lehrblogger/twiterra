/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.applications.sar;

import java.awt.*;
import javax.swing.*;

/**
 * @author tag
 * @version $Id: ControlPanel.java 4908 2008-04-03 19:31:02Z dcollins $
 */
public class ControlPanel extends JPanel
{
    private TracksPanel tracksPanel;
	private AnalysisPanel analysisPanel1;

    public ControlPanel()
    {
        initComponents();
	}

    public TracksPanel getTracksPanel()
    {
        return this.tracksPanel;
    }

    public AnalysisPanel getAnalysisPanel1()
    {
        return this.analysisPanel1;
    }

    private void initComponents()
    {
		this.tracksPanel = new TracksPanel();
		this.analysisPanel1 = new AnalysisPanel();

		//======== this ========
		setLayout(new BorderLayout());

		//======== SplitPane ========
        JSplitPane splitPane = new JSplitPane();
        {
			splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitPane.setResizeWeight(1); // Give all available extra space to TracksPanel
            splitPane.setTopComponent(this.tracksPanel);
			splitPane.setBottomComponent(this.analysisPanel1);
		}
		add(splitPane, BorderLayout.CENTER);
	}
}
