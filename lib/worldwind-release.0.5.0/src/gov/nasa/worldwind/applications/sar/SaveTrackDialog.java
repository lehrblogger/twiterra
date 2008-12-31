/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * @author dcollins
 * @version $Id: SaveTrackDialog.java 4966 2008-04-08 20:14:44Z dcollins $
 */
public class SaveTrackDialog
{
    private JFileChooser fileChooser;
    private JCheckBox saveAnnotations;

    private static final int[] FILE_FORMATS = {SARTrack.FORMAT_CSV, SARTrack.FORMAT_GPX, SARTrack.FORMAT_NMEA};

    public static final int APPROVE_OPTION = JFileChooser.APPROVE_OPTION;
    public static final int CANCEL_OPTION  = JFileChooser.CANCEL_OPTION;
    public static final int ERROR_OPTION   = JFileChooser.ERROR_OPTION;

    public SaveTrackDialog()
    {
        initComponents();
    }

    public File getSelectedFile()
    {
        File file = this.fileChooser.getSelectedFile();
        if (file != null)
        {
            String fmt = stringFromFormat(getFileFormat());
            if (fmt != null)
            {
                if (!fmt.startsWith("."))
                    fmt = "." + fmt;
                if (!file.getPath().endsWith(fmt) && !file.getPath().endsWith(fmt.toLowerCase()))
                    file = new File(file.getPath() + fmt.toLowerCase());
            }
        }
        return file;
    }

    public void setSelectedFile(File file)
    {
        this.fileChooser.setSelectedFile(file);
    }

    public void setSelectedFile(SARTrack track)
    {
        if (track != null)
        {
            if (track.getFile() != null)
                this.fileChooser.setSelectedFile(track.getFile());
            else if (track.getName() != null && this.fileChooser.getCurrentDirectory() != null)
                this.fileChooser.setSelectedFile(new File(this.fileChooser.getCurrentDirectory(), track.getName()));
        }
    }

    public boolean isSaveAnnotations()
    {
        return this.saveAnnotations.isSelected();
    }

    public void setSaveAnnotations(boolean saveAnnotations)
    {
        this.saveAnnotations.setSelected(saveAnnotations);
    }

    public int getFileFormat()
    {
        FileFilter ff = this.fileChooser.getFileFilter();
        return (ff != null) ? formatFromString(ff.getDescription()) : 0;
    }

    public void setFileFormat(int format)
    {
        FileFilter ff = filterForFormat(format);
        if (ff != null)
            this.fileChooser.setFileFilter(ff);
    }

    public void setFileFormat(SARTrack track)
    {
        if (track != null)
        {
            FileFilter ff = filterForFormat(track.getFormat());
            if (ff == null) // If the track format is invalid, default to CSV.
                ff = filterForFormat(SARTrack.FORMAT_CSV);
            if (ff != null)
                this.fileChooser.setFileFilter(ff);
        }
    }

    public File getCurrentDirectory()
    {
        return this.fileChooser.getCurrentDirectory();
    }

    public void setCurrentDirectory(File dir)
    {
        this.fileChooser.setCurrentDirectory(dir);
    }

    public String getDialogTitle()
    {
        return this.fileChooser.getDialogTitle();
    }

    public void setDialogTitle(String dialogTitle)
    {
        this.fileChooser.setDialogTitle(dialogTitle);
    }

    public void setDialogTitle(SARTrack track)
    {
        String title = null;
        String formatString = "Save \"%s\" As";
        if (track.getName() != null)
            title = String.format(formatString, track.getName());
        else if (track.getFile() != null)
            title = String.format(formatString, track.getFile().getName());

        if (title != null)
            this.fileChooser.setDialogTitle(title);
    }

    public int showSaveDialog(Component parent) throws HeadlessException
    {
        return this.fileChooser.showSaveDialog(parent);
    }

    public static int showSaveChangesPrompt(Component parent, String title, String message, SARTrack track)
    {
        if (title == null)
            title = "Save";

        String formatString = "Save changes to the Track\n\"%s\" before closing?";
        if (message == null)
        {
            if (track != null && track.getName() != null)
                message = String.format(formatString, track.getName());
            else if (track != null && track.getFile() != null)
                message = String.format(formatString, track.getFile().getName());
        }

        return JOptionPane.showOptionDialog(
            parent, // parentComponent
            message,
            title,
            JOptionPane.YES_NO_CANCEL_OPTION, // optionType
            JOptionPane.WARNING_MESSAGE, // messageType
            null, // icon
            new Object[] {"Save As...", "Don't Save", "Cancel"}, // options
            "Save As..."); // initialValue
    }

    public static int showOverwritePrompt(Component parent, String title, String message, File file)
    {
        if (title == null)
            title = "Save";

        if (message == null)
        {
            if (file != null)
                message = String.format("Overwrite existing file\n\"%s\"?", file.getPath());
            else
                message = "Overwrite existing file?";
        }

        return JOptionPane.showOptionDialog(
                parent, // parentComponent
                message,
                title,
                JOptionPane.YES_NO_OPTION, // optionType
                JOptionPane.WARNING_MESSAGE, // messageType
                null, // icon
                new Object[] {"Overwrite", "Cancel"}, // options
                "Overwrite"); // initialValue
    }

    private void initComponents()
    {
        this.fileChooser = new JFileChooser()
        {
            public void approveSelection()
            {
                if (doApproveSelection())
                    super.approveSelection();
            }
        };
        this.fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        this.fileChooser.setMultiSelectionEnabled(false);
        makeAccessory();
        makeFileFilters(FILE_FORMATS);
    }

    private boolean doApproveSelection()
    {
        File f = this.fileChooser.getSelectedFile();
        if (f != null && f.exists())
        {
            int state = showOverwritePrompt(this.fileChooser, null, null, f);
            if (state != JOptionPane.YES_OPTION)
                return false;
        }

        return true;
    }

    private void makeAccessory()
    {
        Box box = Box.createVerticalBox();
        box.setBorder(new EmptyBorder(0, 10, 0, 10));

        JLabel label = new JLabel("Options");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(label);
        box.add(Box.createVerticalStrut(5));

        this.saveAnnotations = new JCheckBox("Save Annotations");
        this.saveAnnotations.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.saveAnnotations.setSelected(true);
        box.add(this.saveAnnotations);

        this.fileChooser.setAccessory(box);
    }

    private void makeFileFilters(int[] formats)
    {
        final javax.swing.filechooser.FileFilter allFilter = this.fileChooser.getAcceptAllFileFilter();
        for (int i : formats)
        {
            final String description = stringFromFormat(i);
            FileFilter ff = new FileFilter()
            {
                public boolean accept(File file)
                {
                    return allFilter.accept(file);
                }

                public String getDescription()
                {
                    return description;
                }
            };
            this.fileChooser.addChoosableFileFilter(ff);
        }

        this.fileChooser.setAcceptAllFileFilterUsed(false);
    }

    private String stringFromFormat(int format)
    {
        switch (format)
        {
            case SARTrack.FORMAT_CSV:
                return "CSV";
            case SARTrack.FORMAT_GPX:
                return "GPX";
            case SARTrack.FORMAT_NMEA:
                return "NMEA";
        }
        return null;
    }

    private FileFilter filterForFormat(int format)
    {
        FileFilter result = null;
        String s = stringFromFormat(format);
        if (s != null)
        {
            for (FileFilter ff : this.fileChooser.getChoosableFileFilters())
            {
                if (s.equalsIgnoreCase(ff.getDescription()))
                {
                    result = ff;
                    break;
                }
            }
        }
        return result;
    }

    private int formatFromString(String s)
    {
        if ("CSV".equalsIgnoreCase(s))
            return SARTrack.FORMAT_CSV;
        else if ("GPX".equalsIgnoreCase(s))
            return SARTrack.FORMAT_GPX;
        else if ("NMEA".equalsIgnoreCase(s))
            return SARTrack.FORMAT_NMEA;
        return 0;
    }
}
