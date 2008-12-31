/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.*;

/**
 * Implements <code>WWObject</code> functionality. Meant to be either subclassed or aggretated by classes implementing
 * <code>WWObject</code>.
 *
 * @author Tom Gaskins
 * @version $Id: WWObjectImpl.java 4816 2008-03-26 19:46:19Z tgaskins $
 */
public class WWObjectImpl extends AVListImpl implements WWObject
{
    /**
     * Constructs a new <code>WWObjectImpl</code>.
     */
    public WWObjectImpl()
    {
    }

    public WWObjectImpl(Object source)
    {
        super(source);
    }

    /**
     * The property change listener for <em>this</em> instance.
     * Recieves property change notifications that this instance has registered with other proprty change notifiers.
     * @param propertyChangeEvent the event
     * @throws IllegalArgumentException if <code>propertyChangeEvent</code> is null
     */
    public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent)
    {
        if (propertyChangeEvent == null)
        {
            String msg = Logging.getMessage("nullValue.PropertyChangeEventIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Notify all *my* listeners of the change that I caught
        this.changeSupport.firePropertyChange(propertyChangeEvent);
    }
}
