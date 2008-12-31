/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import javax.swing.*;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: PositionsContextMenu.java 4017 2007-12-20 20:37:54Z tgaskins $
 */
public class PositionsContextMenu extends MouseAdapter
{
    private final PositionTable positionTable;

    public PositionsContextMenu(final PositionTable positionTable)
    {
        this.positionTable = positionTable;
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent)
    {
        this.checkPopup(mouseEvent);
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent)
    {
        this.checkPopup(mouseEvent);
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent)
    {
        this.checkPopup(mouseEvent);
    }

    private void checkPopup(MouseEvent e)
    {
        if (!e.isPopupTrigger())
            return;

        boolean itemSelected = positionTable.getSarTrack().size() > 0 && positionTable.getSelectedRowCount() > 0;

        JMenuItem mi;
        JPopupMenu pum = new JPopupMenu();

        mi = new JMenuItem(new DeletePositionsAction(positionTable));
        mi.setEnabled(itemSelected);
        pum.add(mi);

        mi = new JMenuItem(new AppendPositionAction(positionTable));
        pum.add(mi);

        mi = new JMenuItem(new InsertPositionAction(true, positionTable));
        mi.setEnabled(itemSelected);
        pum.add(mi);

        mi = new JMenuItem(new InsertPositionAction(false, positionTable));
        mi.setEnabled(itemSelected);
        pum.add(mi);

        pum.show(positionTable, e.getX(), e.getY());
    }
}
