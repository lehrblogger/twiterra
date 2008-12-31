/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.layers.Earth.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.WWIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;

/**
 * @author tag
 * @version $Id: SAR2.java 5059 2008-04-14 19:05:09Z dcollins $
 */
public class SAR2 extends JFrame
{
    // Track and WWJ components.
    private TrackController trackController;
    private SARAnnotationSupport annotationSupport;
    private WorldWindow wwd;
    // Timer components.
    @SuppressWarnings({"FieldCanBeLocal"})
    private Timer redrawTimer;
    private static final int REDRAW_TIMER_DELAY = 1000;  // 1 sec
    // UI components.
    private ControlPanel controlPanel;
    private WWPanel wwPanel;
    private LayerMenu layerMenu;
    private JCheckBoxMenuItem feetMenuItem;
    private JCheckBoxMenuItem metersMenuItem;
    private HelpFrame helpFrame;
    private JFileChooser openFileChooser;
    private SaveTrackDialog saveTrackDialog;
    private static final int OK = 0;
    private static final int CANCELLED = 2;
    private static final int ERROR = 4;
    // Unit constants.
    private String elevationUnit;
    public static final String ELEVATION_UNIT = "SAR2.ElevationUnit";
    public static final String UNIT_IMPERIAL = "Imperial";
    public static final String UNIT_METRIC = "Metric";
    private final static double METER_TO_FEET = 3.280839895;

    public SAR2()
    {
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        initComponents();
        this.setTitle(SARApp.APP_NAME_AND_VERSION);

        this.wwd = this.wwPanel.getWwd();
        for (Layer layer : this.wwd.getModel().getLayers())
        {
            if (layer instanceof USGSDigitalOrtho)
            {
                layer.setOpacity(0.5);
                layer.setEnabled(false);
            }
            else if (layer instanceof USGSTopographicMaps)
            {
                layer.setEnabled(false);
                layer.setOpacity(0.5);
            }
            else if (layer instanceof USGSUrbanAreaOrtho)
            {
                layer.setEnabled(false);
            }
        }

        this.getAnalysisPanel().setWwd(this.wwd);

        trackController = new TrackController();
        this.trackController.setWwd(this.wwd);
        this.trackController.setTracksPanel(this.getTracksPanel());
        this.trackController.setAnalysisPanel(this.getAnalysisPanel());

        this.layerMenu.setWwd(this.wwd);

        this.annotationSupport = new SARAnnotationSupport();
        this.annotationSupport.setWwd(this.wwd);

        setElevationUnit(UNIT_IMPERIAL);

        // Setup and start redraw timer - to force downloads to completion without user interaction
        this.redrawTimer = new Timer(REDRAW_TIMER_DELAY, new ActionListener()   // 1 sec
        {
            public void actionPerformed(ActionEvent event)
            {
                wwd.redraw();
            }

        });
        this.redrawTimer.start();
    }

    public static void centerWindowInDesktop(Window window)
    {
        if (window != null)
        {
            int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
            int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(window.getGraphicsConfiguration());
            int desktopWidth = screenWidth - screenInsets.left - screenInsets.right;
            int desktopHeight = screenHeight - screenInsets.bottom - screenInsets.top;
            int frameWidth = window.getSize().width;
            int frameHeight = window.getSize().height;

            if (frameWidth > desktopWidth)
                frameWidth = Math.min(frameWidth, desktopWidth);
            if (frameHeight > desktopHeight)
                frameHeight = Math.min(frameHeight, desktopHeight);

            window.setPreferredSize(new Dimension(
                frameWidth,
                frameHeight));
            window.pack();
            window.setLocation(
                (desktopWidth - frameWidth) / 2 + screenInsets.left,
                (desktopHeight - frameHeight) / 2 + screenInsets.top);
        }
    }

    public static double metersToFeet(double meters)
    {
        return meters * METER_TO_FEET;
    }

    public static double feetToMeters(double feet)
    {
        return feet / METER_TO_FEET;
    }

    public String getElevationUnit()
    {
        return this.elevationUnit;
    }

    public void setElevationUnit(String unit)
    {
        String oldValue = this.elevationUnit;
        this.elevationUnit = unit;
        elevationUnitChanged(oldValue, this.elevationUnit);
    }

    private void elevationUnitChanged(String oldValue, String newValue)
    {
        // Update unit menu selection.
        if (UNIT_IMPERIAL.equals(newValue))
            this.feetMenuItem.setSelected(true);
        else if (UNIT_METRIC.equals(newValue))
            this.metersMenuItem.setSelected(true);

        // The TracksPanel doesn't listen to the WorldWindow. Handle it as a special case.
        getTracksPanel().setElevationUnit(newValue);

        // Use the WorldWindow as a vehicle for communicating the value change.
        // Components that need to know the current unit will listen on this WorldWindow
        // for a change with the name ELEVATION_UNIT.
        this.wwd.setValue(ELEVATION_UNIT, newValue);
        this.wwd.firePropertyChange(ELEVATION_UNIT, oldValue, newValue);
        this.wwd.redraw();
    }

    public SARTrack getCurrentTrack()
    {
        return getTracksPanel().getCurrentTrack();
    }

    public TracksPanel getTracksPanel()
    {
        return controlPanel.getTracksPanel();
    }

    public AnalysisPanel getAnalysisPanel()
    {
        return controlPanel.getAnalysisPanel1();
    }

    private void newTrack(String name)
    {
        Object inputValue = JOptionPane.showInputDialog(this, "Enter a new track name", "Add New Track",
            JOptionPane.QUESTION_MESSAGE, null, null, name);
        if (inputValue == null)
            return;

        name = inputValue.toString();
        
        SARTrack st = new SARTrack(name);
        trackController.addTrack(st);

        st.clearDirtyBit();        
    }

    private void newTrackFromFile(String filePath, String name)
    {
        if (filePath == null)
        {
            File file = showOpenDialog("Choose a track file");
            if (file != null)
                filePath = file.getPath();
        }

        if (filePath == null)
            return;

        SARTrack track = null;
        try
        {
            track = SARTrack.fromFile(filePath);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (track == null)
            return;

        if (name != null)
            track.setName(name);
        
        trackController.addTrack(track);

        try
        {
            String annotationFilePath = getAnnotationsPath(filePath);
            this.annotationSupport.readAnnotations(annotationFilePath, track);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        track.clearDirtyBit();
    }

    private File showOpenDialog(String title)
    {
        if (this.openFileChooser == null)
            this.openFileChooser = new JFileChooser();

        if (title == null)
            title = "Open Track";

        this.openFileChooser.setDialogTitle(title);
        this.openFileChooser.setMultiSelectionEnabled(false);
        int state = this.openFileChooser.showOpenDialog(this);
        return (state == JFileChooser.APPROVE_OPTION) ? this.openFileChooser.getSelectedFile() : null;
    }

    private void newTrackFromURL(String urlString, String name)
    {
        if (urlString == null)
        {
            Object input = JOptionPane.showInputDialog(SAR2.this, "Enter a track URL", "Add New Track",
                JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (input != null)
                urlString = input.toString();
        }

        if (urlString == null)
            return;

        URL url = makeURL(urlString);
        if (url == null)
            return;

        SARTrack track = null;
        try
        {
            ByteBuffer bb = WWIO.readURLContentToBuffer(url);
            File file = WWIO.saveBufferToTempFile(bb, ".xml");
            track = SARTrack.fromFile(file.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (track == null)
            return;

        if (name == null)
            name = urlString;

        track.setFile(null);
        track.setName(name);
        trackController.addTrack(track);
        track.markDirty();
    }

    private static URL makeURL(String urlString)
    {
        URL url = null;
        try
        {
            if (urlString != null)
                url = new URL(urlString);
        }
        catch (MalformedURLException e)
        {
            url = null;
        }
        return url;
    }

    private int removeTrack(SARTrack track, boolean forceSavePrompt)
    {
        if (track == null)
            return OK;

        int status = OK;
        if (track.isDirty() || forceSavePrompt)
        {

            int option = SaveTrackDialog.showSaveChangesPrompt(this, null, null, track);
            // Show a save track dialog that will
            // always prompt the user to choose a location.
            if (option == JOptionPane.YES_OPTION)
                status = saveTrack(track, true);
            else if (option == JOptionPane.CANCEL_OPTION)
                status = CANCELLED;
        }

        if (status != OK)
            return status;

        try
        {
            track.firePropertyChange(TrackController.TRACK_REMOVE, null, track);
            this.trackController.refreshCurrentTrack();
            this.annotationSupport.removeAnnotationsForTrack(track);
            this.wwd.redraw();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ERROR;
        }

        return OK;
    }

    private int removeAllTracks(boolean forceSavePrompt)
    {
        int status = OK;
        for (SARTrack track : getTracksPanel().getAllTracks())
        {
            status |= removeTrack(track, forceSavePrompt);
            if ((status & CANCELLED) != 0)
                return status;
        }

        return status;
    }

    private int saveTrack(SARTrack track, boolean forceSavePrompt)
    {
        return saveTrack(
            track,
            null,  // Use track's file, or prompt user.
            0,     // Use track's format.
            true,  // Save annotations
            forceSavePrompt);
    }

    private int saveTrack(SARTrack track, File file, int format, boolean saveAnnotations, boolean forceSavePrompt)
    {
        if (track == null)
            return OK;

        if (file == null)
            file = track.getFile();
        if (format == 0)
            format = track.getFormat();

        // Show the "Overwrite?" dialog if:
        // * The current track has a source file.
        // * AND The caller has specified not to show the save dialog.
        //if (file != null && !forceSavePrompt)
        //{
        //    int result = SaveTrackDialog.showOverwritePrompt(this, null, null, JOptionPane.YES_NO_CANCEL_OPTION, file);
        //    if (result == JOptionPane.CANCEL_OPTION)
        //        return CANCELLED;
        //    else if (result == JOptionPane.NO_OPTION)
        //        forceSavePrompt = true;
        //}

        // Show the "Save As..." dialog if either:
        // * The current track has no source file.
        // * The caller has specified that the user should prompted to select a file,
        if (file == null || forceSavePrompt)
        {
            int result = showSaveDialog(track);
            if (result == SaveTrackDialog.CANCEL_OPTION)
                return CANCELLED;
            else if (result == SaveTrackDialog.ERROR_OPTION)
                return ERROR;

            file = this.saveTrackDialog.getSelectedFile();
            format = this.saveTrackDialog.getFileFormat();
            saveAnnotations = this.saveTrackDialog.isSaveAnnotations();
        }

        try
        {
            // Get the file's last modified time,
            // or zero if the file does not exist.
            long time = file.exists() ? file.lastModified() : 0;

            SARTrack.toFile(track, file.getPath(), format);
            if (saveAnnotations)
            {
                String annotationFilePath = getAnnotationsPath(file.getPath());                
                this.annotationSupport.writeAnnotations(annotationFilePath, track);
            }

            // If the track was saved sucessfully (it exists and
            // is newer than is was before the save operation),
            // then adopt the properties of the new
            // file and format, and clear the track's dirty bit.
            if (file.exists() && time <= file.lastModified())
            {
                track.setFile(file);
                track.setFormat(format);
                track.setName(file.getName());
                track.clearDirtyBit();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ERROR;
        }

        return OK;
    }

    private int showSaveDialog(SARTrack track)
    {
        if (this.saveTrackDialog == null)
            this.saveTrackDialog = new SaveTrackDialog();
        this.saveTrackDialog.setDialogTitle(track);
        this.saveTrackDialog.setFileFormat(track);
        this.saveTrackDialog.setSelectedFile(track);
        return this.saveTrackDialog.showSaveDialog(this);
    }

    private SARAnnotation getCurrentAnnotation()
    {
        return this.annotationSupport.getCurrent();
    }

    private void newAnnotation()
    {
        newAnnotation(null, getCurrentTrack());
    }

    private void newAnnotation(String text, SARTrack track)
    {
        this.annotationSupport.addNew(text, track);
        this.wwd.redraw();
    }

    private void removeAnnotation(SARAnnotation annotation)
    {
        if (annotation != null)
        {
            this.annotationSupport.remove(annotation);
        }
        this.wwd.redraw();
    }

    private void setAnnotationsEnabled(boolean show)
    {
        this.annotationSupport.setEnabled(show);
        this.wwd.redraw();
    }

    private void showHelp()
    {
        try
        {
            if (this.helpFrame == null)
                this.helpFrame = new HelpFrame();
            this.helpFrame.setVisible(true);
        }
        catch (IOException e1)
        {
            System.err.println("Unable to open Help window");
            e1.printStackTrace();
        }
    }

    public void showAbout()
    {
        SARAboutDialog dialog = new SARAboutDialog();
        dialog.showDialog(this);
    }

    public boolean exit()
    {
        int status = removeAllTracks(false);
        if ((status & CANCELLED) != 0)
            return false;

        dispose();
        System.exit(0);
        return true;
    }

    private String getAnnotationsPath(String trackFilePath)
    {
        return (trackFilePath != null) ? trackFilePath + ".sta" : null;
    }

    private void initComponents()
    {
        //======== this ========
        setTitle("World Wind Search and Rescue");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                exit();
            }

            public void windowClosed(WindowEvent event) {
                exit();
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        controlPanel = new ControlPanel();
        contentPane.add(controlPanel, BorderLayout.WEST);

        //---- WWPanel ----
        wwPanel = new WWPanel();
        wwPanel.setPreferredSize(new Dimension(1000, 800));
        contentPane.add(wwPanel, BorderLayout.CENTER);

        //======== MenuBar ========
        JMenuBar menuBar = new JMenuBar();
        {

            JMenu fileMenu = new JMenu();
            //======== "File" ========
        	{
        		fileMenu.setText("File");
        		fileMenu.setMnemonic('F');

                //---- "New Track" ----
        		JMenuItem newTrack = new JMenuItem();
                newTrack.setText("New Track...");
                newTrack.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        newTrack(null);
                    }
                });
                fileMenu.add(newTrack);

                //---- "Open Track File" ----
        		JMenuItem openTrackFile = new JMenuItem();
                openTrackFile.setText("Open Track File...");
                openTrackFile.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        newTrackFromFile(null, null);
                    }
                });
                fileMenu.add(openTrackFile);

        		//---- "Open Track URL..." ----
                JMenuItem openTrackURL = new JMenuItem();
                openTrackURL.setText("Open Track URL...");
                openTrackURL.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        newTrackFromURL(null, null);
                    }
                });
                fileMenu.add(openTrackURL);

        		//---- "Close Track" ----
                JMenuItem removeTrack = new JMenuItem();
                removeTrack.setText("Close Track");
                removeTrack.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        removeTrack(getCurrentTrack(), false);
                    }
                });
                fileMenu.add(removeTrack);

                //--------
                fileMenu.addSeparator();

                //---- "Save Track" ----
                JMenuItem saveTrack = new JMenuItem();
                saveTrack.setText("Save Track");
                saveTrack.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        // Show a save track dialog that won't prompt the user
                        // to choose a location unless it has to.
                        saveTrack(getCurrentTrack(), true);
                    }
                });
                fileMenu.add(saveTrack);

                //---- "Save Track As..." ----
                JMenuItem saveTrackAs = new JMenuItem();
                saveTrackAs.setText("Save Track As...");
                saveTrackAs.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        // Show a save track dialog that will always
                        // prompt the user to choose a location.
                        saveTrack(getCurrentTrack(), true);
                    }
                });
                fileMenu.add(saveTrackAs);

                //--------
                fileMenu.addSeparator();

                //---- urlTrackFetch1 ----
        		JMenuItem urlTrackFetch1 = new JMenuItem();
                urlTrackFetch1.setText("PipeTrackTest.gpx");
                urlTrackFetch1.setActionCommand("http://worldwind.arc.nasa.gov/java/apps/SARApp/PipeTrackTest.xml");
        		urlTrackFetch1.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
                        newTrackFromURL(e.getActionCommand(), null);
        			}
        		});
        		fileMenu.add(urlTrackFetch1);

        		//---- urlTrackFetch2 ----
                JMenuItem urlTrackFetch2 = new JMenuItem();
                urlTrackFetch2.setText("PipeTracks2.gpx");
                urlTrackFetch2.setActionCommand("http://worldwind.arc.nasa.gov/java/apps/SARApp/PipeTracks2.xml");
                urlTrackFetch2.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				newTrackFromURL(e.getActionCommand(), null);
        			}
        		});
        		fileMenu.add(urlTrackFetch2);

        		//---- urlTrackFetch3 ----
                JMenuItem urlTrackFetch3 = new JMenuItem();
                urlTrackFetch3.setText("PipeTracks3.gpx");
                urlTrackFetch3.setActionCommand("http://worldwind.arc.nasa.gov/java/apps/SARApp/PipeTracks3.xml");
        		urlTrackFetch3.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				newTrackFromURL(e.getActionCommand(), null);
        			}
        		});
        		fileMenu.add(urlTrackFetch3);

                if (!Configuration.isMacOS())
                {
                    //--------
                    fileMenu.addSeparator();

                    JMenuItem exit = new JMenuItem();
                    exit.setText("Exit");
                    exit.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            exit();
                        }
                    });
                    fileMenu.add(exit);
                }
                else
                {
                    try
                    {
                        OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("exit", (Class[]) null));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        	menuBar.add(fileMenu);

            //======== "View" ========
            JMenu viewMenu = new JMenu();
            {
                viewMenu.setText("Units");
                viewMenu.setMnemonic('V');

                //---- "Meters" ----
                metersMenuItem = new JCheckBoxMenuItem();
                metersMenuItem.setText("Meters");
                metersMenuItem.setActionCommand(UNIT_METRIC);
                metersMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setElevationUnit(e.getActionCommand());
                    }
                });
                viewMenu.add(metersMenuItem);

                //---- "Feet" ----
                feetMenuItem = new JCheckBoxMenuItem();
                feetMenuItem.setText("Feet");
                feetMenuItem.setActionCommand(UNIT_IMPERIAL);
                feetMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setElevationUnit(e.getActionCommand());
                    }
                });
                viewMenu.add(feetMenuItem);

                ButtonGroup unitGroup = new ButtonGroup();
                unitGroup.add(metersMenuItem);
                unitGroup.add(feetMenuItem);
            }
            menuBar.add(viewMenu);

        	//======== "Annotation" ========
            JMenu annotationMenu = new JMenu();
            {
        		annotationMenu.setText("Annotation");
        		annotationMenu.setMnemonic('A');

        		//---- "New Annotation..." ----
                JMenuItem newAnnotation = new JMenuItem();
                newAnnotation.setText("New Annotation...");
        		newAnnotation.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				newAnnotation();
        			}
        		});
        		annotationMenu.add(newAnnotation);

                //---- "Remove Annotation" ----
                JMenuItem removeAnnotation = new JMenuItem();
                removeAnnotation.setText("Remove Annotation");
                removeAnnotation.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        removeAnnotation(getCurrentAnnotation());
                    }
                });
                annotationMenu.add(removeAnnotation);

                //---- "Show Annotations" ----
                JCheckBoxMenuItem showAnnotations = new JCheckBoxMenuItem();
                showAnnotations.setText("Show Annotations");
        		showAnnotations.setSelected(true);
        		showAnnotations.addItemListener(new ItemListener() {
        			public void itemStateChanged(ItemEvent e) {
        				setAnnotationsEnabled(e.getStateChange() == ItemEvent.SELECTED);
        			}
        		});
        		annotationMenu.add(showAnnotations);
        	}
        	menuBar.add(annotationMenu);

        	//======== "Layers" ========
            layerMenu = new LayerMenu();
            {
        		layerMenu.setMnemonic('L');
            }
        	menuBar.add(layerMenu);

        	//======== "Help" ========
            JMenu helpMenu = new JMenu();
            {
        		helpMenu.setText("Help");
        		helpMenu.setMnemonic('H');

        		//---- "Search and Rescue Help" ----
                JMenuItem sarHelp = new JMenuItem();
                sarHelp.setText("Search and Rescue Help");
        		sarHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        		sarHelp.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				showHelp();
        			}
        		});
        		helpMenu.add(sarHelp);

                //---- "About [World Wind Search and Rescue Prototype]" ----
                if (!Configuration.isMacOS())
                {
                    JMenuItem about = new JMenuItem();
                    about.setText("About");
                    about.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            showAbout();
                        }
                    });
                    helpMenu.add(about);
                }
                else
                {
                    try
                    {
                        OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAbout", (Class[]) null));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

            }
        	menuBar.add(helpMenu);
        }
        setJMenuBar(menuBar);

        pack();
        centerWindowInDesktop(this);
    }
}
