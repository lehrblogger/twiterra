/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

/**
 * The <code>IconLayer</code> class manages a collection of {@link gov.nasa.worldwind.render.WWIcon} objects
 * for rendering and picking. <code>IconLayer</code> delegates to its internal
 * {@link gov.nasa.worldwind.render.IconRenderer} for rendering and picking operations.
 *
 * @author tag
 * @version $Id: IconLayer.java 4049 2007-12-22 15:59:29Z dcollins $
 * @see gov.nasa.worldwind.render.WWIcon
 * @see gov.nasa.worldwind.render.IconRenderer
 */
public class IconLayer extends AbstractLayer
{
    private final java.util.Collection<WWIcon> icons = new java.util.concurrent.ConcurrentLinkedQueue<WWIcon>();
    private Iterable<WWIcon> iconsOverride;
    private IconRenderer iconRenderer = new IconRenderer();
    private Pedestal pedestal;

    /**
     * Creates a new <code>IconLayer</code> with an empty collection of Icons.
     */
    public IconLayer()
    {
    }

    /**
     * Adds the specified <code>icon</code> to this layer's internal collection.
     * If this layer's internal collection has been overriden with a call to {@link #setIcons},
     * this will throw an exception.
     *
     * @param icon Icon to add.
     * @throws IllegalArgumentException If <code>icon</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setIcons</code>.
     */
    public void addIcon(WWIcon icon)
    {
        if (icon == null)
        {
            String msg = Logging.getMessage("nullValue.Icon");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.iconsOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.icons.add(icon);
    }

    /**
     * Adds the contents of the specified <code>icons</code> to this layer's internal collection.
     * If this layer's internal collection has been overriden with a call to {@link #setIcons},
     * this will throw an exception.
     *
     * @param icons Icons to add.
     * @throws IllegalArgumentException If <code>icons</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setIcons</code>.
     */
    public void addIcons(Iterable<WWIcon> icons)
    {
        if (icons == null)
        {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.iconsOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        for (WWIcon icon : icons)
        {
            // Internal list of icons does not accept null values.
            if (icon != null)
                this.icons.add(icon);
        }
    }

    /**
     * Removes the specified <code>icon</code> from this layer's internal collection, if it exists.
     * If this layer's internal collection has been overriden with a call to {@link #setIcons},
     * this will throw an exception.
     *
     * @param icon Icon to remove.
     * @throws IllegalArgumentException If <code>icon</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setIcons</code>.
     */
    public void removeIcon(WWIcon icon)
    {
        if (icon == null)
        {
            String msg = Logging.getMessage("nullValue.Icon");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.iconsOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.icons.remove(icon);
    }

    /**
     * Clears the contents of this layer's internal Icon collection.
     * If this layer's internal collection has been overriden with a call to {@link #setIcons},
     * this will throw an exception.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setIcons</code>.
     */
    public void removeAllIcons()
    {
        if (this.iconsOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        clearIcons();
    }

    private void clearIcons()
    {
        if (this.icons != null && this.icons.size() > 0)
            this.icons.clear();
    }

    /**
     * Returns the Iterable of Icons currently in use by this layer.
     * If the caller has specified a custom Iterable via {@link #setIcons}, this will returns a reference
     * to that Iterable. If the caller passed <code>setIcons</code> a null parameter,
     * or if <code>setIcons</code> has not been called, this returns a view of this layer's internal
     * collection of Icons.
     *
     * @return Iterable of currently active Icons.
     */
    public Iterable<WWIcon> getIcons()
    {
        return getActiveIcons();
    }

    /**
     * Returns the Iterable of currently active Icons.
     * If the caller has specified a custom Iterable via {@link #setIcons}, this will returns a reference
     * to that Iterable. If the caller passed <code>setIcons</code> a null parameter,
     * or if <code>setIcons</code> has not been called, this returns a view of this layer's internal
     * collection of Icons.
     *
     * @return Iterable of currently active Icons.
     */
    private Iterable<WWIcon> getActiveIcons()
    {
        if (this.iconsOverride != null)
        {
            return this.iconsOverride;
        }
        else
        {
            // Return an unmodifiable reference to the internal list of icons.
            // This prevents callers from changing this list and invalidating any invariants we have established.
            return java.util.Collections.unmodifiableCollection(this.icons);
        }
    }

    /**
     * Overrides the collection of currently active Icons with the specified <code>iconIterable</code>.
     * This layer will maintain a reference to <code>iconIterable</code> strictly for picking and rendering.
     * This layer will not modify the Iterable reference. However, this will clear
     * the internal collection of Icons, and will prevent any modification to its contents via
     * <code>addIcon, addIcons, or removeIcons</code>.
     *
     * If the specified <code>iconIterable</code> is null, this layer will revert to maintaining its internal
     * collection.
     *
     * @param iconIterable Iterable to use instead of this layer's internal collection, or null to use this
     *                     layer's internal collection.
     */
    public void setIcons(Iterable<WWIcon> iconIterable)
    {
        this.iconsOverride = iconIterable;
        // Clear the internal collection of Icons.
        clearIcons();
    }

    /**
     * Returns the <code>Pedestal</code> used by this layers internal <code>IconRenderer</code>.
     *
     * @return <code>Pedestal</code> used by this layers internal <code>IconRenderer</code>.
     */
    public Pedestal getPedestal()
    {
        return pedestal;
    }

    /**
     * Sets the <code>Pedestal</code> used by this layers internal <code>IconRenderer</code>.
     *
     * @param pedestal <code>Pedestal</code> to be used by this layers internal <code>IconRenderer</code>.
     */
    public void setPedestal(Pedestal pedestal)
    {
        this.pedestal = pedestal;
    }

    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        this.iconRenderer.setPedestal(this.pedestal);
        this.iconRenderer.pick(dc, getActiveIcons(), pickPoint, this);
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        this.iconRenderer.setPedestal(this.pedestal);
        this.iconRenderer.render(dc, getActiveIcons());
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.IconLayer.Name");
    }
}
