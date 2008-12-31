/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.geom.Angle;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.event.CellEditorListener;
import javax.swing.table.*;
import java.awt.*;
import java.beans.*;
import java.util.EventObject;

/**
 * @author tag
 * @version $Id: PositionTable.java 4794 2008-03-22 20:55:21Z dcollins $
 */
public class PositionTable extends JTable
{
    private static final int ITEM_NUM_COLUMN = 0;
    private static final int LATITUDE_COLUMN = 1;
    private static final int LONGITUDE_COLUMN = 2;
    private static final int ALTITUDE_COLUMN = 3;

    private SARTrack sarTrack;
    private String elevationUnit;

    public PositionTable()
    {
        this.setToolTipText("Track Positions");
        this.setModel(new MyTableModel());
        this.setDefaultRenderer(Double.class, new DoubleCellRenderer("% 7.4f"));
        TableCellRenderer tcr = this.getTableHeader().getDefaultRenderer();
        this.getTableHeader().getColumnModel().getColumn(ALTITUDE_COLUMN).setHeaderRenderer(new AltitudeHeaderRenderer(tcr, this));
        this.getColumnModel().getColumn(ALTITUDE_COLUMN).setCellRenderer(new AltitudeCellRenderer("%,8.0f", this));
        TableCellEditor tce = this.getDefaultEditor(Double.class);
        this.getColumnModel().getColumn(ALTITUDE_COLUMN).setCellEditor(new AltitudeCellEditor(tce, "%f", this));
        {
            TableColumnModel cm = this.getColumnModel();
            cm.getColumn(0).setResizable(false);
            cm.getColumn(0).setMinWidth(35);
            cm.getColumn(0).setPreferredWidth(35);

            cm.getColumn(1).setResizable(false);
            cm.getColumn(1).setMinWidth(70);
            cm.getColumn(1).setPreferredWidth(80);

            cm.getColumn(2).setResizable(false);
            cm.getColumn(2).setMinWidth(70);
            cm.getColumn(2).setPreferredWidth(80);

            cm.getColumn(3).setResizable(false);
            cm.getColumn(3).setMinWidth(70);
            cm.getColumn(3).setPreferredWidth(70);
        }
    }

    public SARTrack getSarTrack()
    {
        return sarTrack;
    }

    public void setSarTrack(SARTrack sarTrack)
    {
        this.sarTrack = sarTrack;
        updateTableData();
        
        this.sarTrack.addPropertyChangeListener(TrackController.TRACK_MODIFY, new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent propertyChangeEvent)
            {
                updateTableData();
            }
        });
    }

    public String getElevationUnit()
    {
        return this.elevationUnit;
    }

    public void setElevationUnit(String unit)
    {
        this.elevationUnit = unit;
    }

    public void updateTableData()
    {
         ((AbstractTableModel) this.getModel()).fireTableDataChanged();
    }

    private class MyTableModel extends AbstractTableModel
    {
        String[] columnNames = new String[] {
            "#", "Latitude\u00B0", "Longitude\u00B0", "Altitude"
        };

        Class[] columnTypes = new Class[] {
            Integer.class, Double.class, Double.class, Double.class
        };

        boolean[] columnEditable = new boolean[] {
            false, true, true, true
        };

        public MyTableModel()
        {
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return this.columnTypes[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return this.columnEditable[columnIndex];
        }

        public int getRowCount()
        {
            return sarTrack != null ? sarTrack.size() : 0;
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            return this.columnNames[columnIndex];
        }

        public int getColumnCount()
        {
            return 4;
        }

        public Object getValueAt(int row, int col)
        {
            if (sarTrack == null)
                return null;

            switch (col)
            {
                case ITEM_NUM_COLUMN:
                    return row;
                case LATITUDE_COLUMN:
                    return sarTrack.get(row).getLatitude().degrees;
                case LONGITUDE_COLUMN:
                    return sarTrack.get(row).getLongitude().degrees;
                case ALTITUDE_COLUMN:
                    return sarTrack.get(row).getElevation();
            }

            return null;
        }

        @Override
        public void setValueAt(Object object, int row, int col)
        {
            if (sarTrack == null)
                return;

            if (!(object instanceof Double))
                return;

            double newVal = (Double) object;

            SARPosition curPos = sarTrack.get(row);
            SARPosition newPos;

            switch (col)
            {
                case LATITUDE_COLUMN:
                    newPos = newVal < -90 || newVal > 90 ? curPos :
                        new SARPosition(Angle.fromDegrees(newVal), curPos.getLongitude(), curPos.getElevation());
                    break;
                case LONGITUDE_COLUMN:
                    newPos = newVal < -180 || newVal > 180 ? curPos :
                        new SARPosition(curPos.getLatitude(), Angle.fromDegrees(newVal), curPos.getElevation());
                    break;
                case ALTITUDE_COLUMN:
                    // The value stored in a SARPosition's elevation will always be in meters.
                    // So when the altitude is displayed in feet, we will convert the incoming
                    // value back to meters. This allows the user entring a value to operate in
                    // whatever units are being displayed without thinking about conversion.
                    if (SAR2.UNIT_IMPERIAL.equals(elevationUnit))
                        newVal = SAR2.feetToMeters(newVal);
                    newPos = new SARPosition(curPos.getLatitude(), curPos.getLongitude(), newVal);
                    break;
                default:
                    return;
            }

            sarTrack.set(row, newPos);
        }
    }

    private static class AltitudeHeaderRenderer implements TableCellRenderer
    {
        private TableCellRenderer delegate;
        private PositionTable table;

        public AltitudeHeaderRenderer(TableCellRenderer delegate, PositionTable table)
        {
            this.delegate = delegate;
            this.table = table;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            if (this.delegate == null)
                return null;

            Component c = this.delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c == null || !(c instanceof JLabel))
                return c;

            JLabel label = (JLabel) c;
            if (label.getText() == null)
                return c;

            if (SAR2.UNIT_IMPERIAL.equals(this.table.elevationUnit))
                label.setText(label.getText() + " (ft)");
            else // Default to metric units.
                label.setText(label.getText() + " (m)");
            return label;
        }
    }

    private static class DoubleCellRenderer extends DefaultTableCellRenderer
    {
        private final String formatString;

        public DoubleCellRenderer(String formatString)
        {
            this.formatString = formatString;
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object object, boolean b, boolean b1, int i,
            int i1)
        {
            Component c = super.getTableCellRendererComponent(jTable, object, b, b1, i, i1);
            if (!(c instanceof JLabel))
                return c;

            JLabel label = (JLabel) c;
            if (label.getText() == null)
                return c;

            double value = Double.parseDouble(label.getText());
            label.setText(String.format(this.formatString, value));
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            return c;
        }
    }

    private String makeElevationDescription(String formatString, double metersElevation)
    {
        String s;
        if (SAR2.UNIT_IMPERIAL.equals(this.elevationUnit))
            s = String.format(formatString, SAR2.metersToFeet(metersElevation));
        else // Default to metric units.
            s = String.format(formatString, metersElevation);
        return s;
    }

    private static class AltitudeCellRenderer extends DefaultTableCellRenderer
    {
        private final String formatString;
        private PositionTable table;

        private AltitudeCellRenderer(String formatString, PositionTable table)
        {
            this.formatString = formatString;
            this.table = table;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c == null || !(c instanceof JLabel))
                return c;

            JLabel label = (JLabel) c;
            if (label.getText() == null)
                return c;

            double d = Double.parseDouble(label.getText());
            label.setText(this.table.makeElevationDescription(this.formatString, d));
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            return c;
        }
    }

    private static class AltitudeCellEditor implements TableCellEditor
    {
        private TableCellEditor delegate;
        private final String formatString;
        private PositionTable table;

        public AltitudeCellEditor(TableCellEditor delegate, String formatString, PositionTable table)
        {
            this.delegate = delegate;
            this.formatString = formatString;
            this.table = table;
        }

        public Object getCellEditorValue()
        {
            return this.delegate.getCellEditorValue();
        }

        public boolean isCellEditable(EventObject eventObject)
        {
            return this.delegate.isCellEditable(eventObject);
        }

        public boolean shouldSelectCell(EventObject eventObject)
        {
            return this.delegate.shouldSelectCell(eventObject);
        }

        public boolean stopCellEditing()
        {
            return this.delegate.stopCellEditing();
        }

        public void cancelCellEditing()
        {
            this.delegate.cancelCellEditing();
        }

        public void addCellEditorListener(CellEditorListener cellEditorListener)
        {
            this.delegate.addCellEditorListener(cellEditorListener);
        }

        public void removeCellEditorListener(CellEditorListener cellEditorListener)
        {
            this.delegate.removeCellEditorListener(cellEditorListener);
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {

            Component c = this.delegate.getTableCellEditorComponent(table, value, isSelected, row, column);
            if (c == null || !(c instanceof JTextComponent))
                return c;

            JTextComponent label = (JTextComponent) c;
            if (label.getText() == null)
                return c;

            double d = Double.parseDouble(label.getText());
            label.setText(this.table.makeElevationDescription(this.formatString, d));
            return c;
        }
    }
}
