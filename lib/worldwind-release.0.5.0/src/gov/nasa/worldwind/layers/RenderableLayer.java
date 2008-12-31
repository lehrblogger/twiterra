/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;

/**
 * The <code>RenderableLayer</code> class manages a collection of {@link gov.nasa.worldwind.render.Renderable} objects
 * for rendering, picking, and disposal.
 *
 * @author tag
 * @version $Id: RenderableLayer.java 4045 2007-12-22 00:23:29Z dcollins $
 * @see gov.nasa.worldwind.render.Renderable
 */
public class RenderableLayer extends AbstractLayer
{
    private java.util.Collection<Renderable> renderables = new java.util.ArrayList<Renderable>();
    private Iterable<Renderable> renderablesOverride;
    private final PickSupport pickSupport = new PickSupport();
    private final Layer delegateOwner;

    /**
     * Creates a new <code>RenderableLayer</code> with a null <code>delegateOwner</code>
     */
    public RenderableLayer()
    {
        this.delegateOwner = null;
    }

    /**
     * Creates a new <code>RenderableLayer</code> with the specified <code>delegateOwner</code>.
     *
     * @param delegateOwner Layer that is this layer's delegate owner.
     */
    public RenderableLayer(Layer delegateOwner)
    {
        this.delegateOwner = delegateOwner;
    }

    /**
     * Adds the specified <code>renderable</code> to this layer's internal collection.
     * If this layer's internal collection has been overriden with a call to {@link #setRenderables},
     * this will throw an exception.
     *
     * @param renderable Renderable to add.
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void addRenderable(Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.renderables.add(renderable);
    }

    /**
     * Adds the contents of the specified <code>renderables</code> to this layer's internal collection.
     * If this layer's internal collection has been overriden with a call to {@link #setRenderables},
     * this will throw an exception.
     *
     * @param renderables Renderables to add.
     * @throws IllegalArgumentException If <code>renderables</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void addRenderables(Iterable<Renderable> renderables)
    {
        if (renderables == null)
        {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        for (Renderable renderable : renderables)
        {
            // Internal list of renderables does not accept null values.
            if (renderable != null)
                this.renderables.add(renderable);
        }
    }

    /**
     * Removes the specified <code>renderable</code> from this layer's internal collection, if it exists.
     * If this layer's internal collection has been overriden with a call to {@link #setRenderables},
     * this will throw an exception.
     *
     * @param renderable Renderable to remove.
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void removeRenderable(Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.renderables.remove(renderable);
    }

    /**
     * Clears the contents of this layer's internal Renderable collection.
     * If this layer's internal collection has been overriden with a call to {@link #setRenderables},
     * this will throw an exception.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void removeAllRenderables()
    {
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable"); 
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        clearRenderables();
    }

    private void clearRenderables()
    {
        if (this.renderables != null && this.renderables.size() > 0)
            this.renderables.clear();
    }

    /**
     * Returns the Iterable of Renderables currently in use by this layer.
     * If the caller has specified a custom Iterable via {@link #setRenderables}, this will returns a reference
     * to that Iterable. If the caller passed <code>setRenderables</code> a null parameter,
     * or if <code>setRenderables</code> has not been called, this returns a view of this layer's internal
     * collection of Renderables.
     *
     * @return Iterable of currently active Renderables.
     */
    public Iterable<Renderable> getRenderables()
    {
        return getActiveRenderables();
    }

    /**
     * Returns the Iterable of currently active Renderables.
     * If the caller has specified a custom Iterable via {@link #setRenderables}, this will returns a reference
     * to that Iterable. If the caller passed <code>setRenderables</code> a null parameter,
     * or if <code>setRenderables</code> has not been called, this returns a view of this layer's internal
     * collection of Renderables.
     *
     * @return Iterable of currently active Renderables.
     */
    private Iterable<Renderable> getActiveRenderables()
    {
        if (this.renderablesOverride != null)
        {
            return this.renderablesOverride;
        }
        else
        {
            // Return an unmodifiable reference to the internal list of renderables.
            // This prevents callers from changing this list and invalidating any invariants we have established.
            return java.util.Collections.unmodifiableCollection(this.renderables);
        }
    }

    /**
     * Overrides the collection of currently active Renderables with the specified <code>renderableIterable</code>. 
     * This layer will maintain a reference to <code>renderableIterable</code> strictly for picking and rendering.
     * This layer will not modify the reference, or dispose of its contents. This will also clear and dispose of
     * the internal collection of Renderables, and will prevent any modification to its contents via
     * <code>addRenderable, addRenderables, removeRenderables, or dispose</code>.
     *
     * If the specified <code>renderableIterable</code> is null, this layer will revert to maintaining its internal
     * collection.
     *
     * @param renderableIterable Iterable to use instead of this layer's internal collection, or null to use this
     *                           layer's internal collection.
     */
    public void setRenderables(Iterable<Renderable> renderableIterable)
    {
        this.renderablesOverride = renderableIterable;
        // Dispose of the internal collection of Renderables.
        disposeRenderables();
        // Clear the internal collection of Renderables.
        clearRenderables();
    }

    /**
     * Disposes the contents of this layer's internal Renderable collection, but does not remove any elements from
     * that collection.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void dispose()
    {
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        disposeRenderables();
    }

    private void disposeRenderables()
    {
        if (this.renderables != null && this.renderables.size() > 0)
        {
            for (Renderable renderable : this.renderables)
            {
                if (renderable instanceof Disposable)
                    ((Disposable) renderable).dispose();
            }
        }
    }

    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        this.pickSupport.clearPickList();
        this.pickSupport.beginPicking(dc);

        for (Renderable renderable : getActiveRenderables())
        {
            // If the caller has specified their own Iterable,
            // then we cannot make any guarantees about its contents.
            if (renderable != null)
            {
                float[] inColor = new float[4];
                dc.getGL().glGetFloatv(GL.GL_CURRENT_COLOR, inColor, 0);
                java.awt.Color color = dc.getUniquePickColor();
                dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());

                renderable.render(dc);

                dc.getGL().glColor4fv(inColor, 0);

                if (renderable instanceof Locatable)
                {
                    this.pickSupport.addPickableObject(color.getRGB(), renderable,
                        ((Locatable) renderable).getPosition(), false);
                }
                else
                {
                    this.pickSupport.addPickableObject(color.getRGB(), renderable);
                }
            }
        }

        this.pickSupport.resolvePick(dc, pickPoint, this.delegateOwner != null ? this.delegateOwner : this);
        this.pickSupport.endPicking(dc);
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        for (Renderable renderable : getActiveRenderables())
        {
            // If the caller has specified their own Iterable,
            // then we cannot make any guarantees about its contents.
            if (renderable != null)
                renderable.render(dc);
        }
    }

    /**
     * Returns this layer's delegate owner, or null if none has been specified.
     *
     * @return Layer that is this layer's delegate owner.
     */
    public Layer getDelegateOwner()
    {
        return delegateOwner;
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.RenderableLayer.Name");
    }
}
