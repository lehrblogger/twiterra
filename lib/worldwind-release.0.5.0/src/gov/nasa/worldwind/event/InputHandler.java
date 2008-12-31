/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.event;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVList;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

/**
 * @author tag
 * @version $Id: InputHandler.java 5121 2008-04-22 17:54:54Z tgaskins $
 */
public interface InputHandler extends AVList, java.beans.PropertyChangeListener
{
    void setEventSource(WorldWindow newWorldWindow);

    WorldWindow getEventSource();

    void setHoverDelay(int delay);

    int getHoverDelay();

    void addSelectListener(SelectListener listener);

    void removeSelectListener(SelectListener listener);

    void addMouseListener(MouseListener listener);

    void removeMouseListener(MouseListener listener);

    void addMouseMotionListener(MouseMotionListener listener);

    void removeMouseMotionListener(MouseMotionListener listener);

    void addMouseWheelListener(MouseWheelListener listener);

    void removeMouseWheelListener(MouseWheelListener listener);

    void clear();
}
