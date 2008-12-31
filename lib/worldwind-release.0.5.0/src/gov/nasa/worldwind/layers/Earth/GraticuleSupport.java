/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.render.*;

import java.awt.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: GraticuleSupport.java 4707 2008-03-15 07:56:52Z dcollins $
 */
class GraticuleSupport
{
    private static class Pair
    {
        final Object a;
        final Object b;

        Pair(Object a, Object b)
        {
            this.a = a;
            this.b = b;
        }
    }

    private Collection<Pair> renderables = new ArrayList<Pair>();
    private Map<String, GraticuleRenderingParams> namedParams = new HashMap<String, GraticuleRenderingParams>();
    private AVList defaultParams;
    private GeographicTextRenderer textRenderer = new GeographicTextRenderer();

    public GraticuleSupport()
    {
    }

    public void addRenderable(Object renderable, String paramsKey)
    {
        if (renderable == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.renderables.add(new Pair(renderable, paramsKey));
    }

    public void removeAllRenderables()
    {
        this.renderables.clear();
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Collection<GeographicText> text = new ArrayList<GeographicText>();

        for (Pair pair : this.renderables)
        {
            Object renderable = pair.a;
            String paramsKey = (pair.b != null && pair.b instanceof String) ? (String) pair.b : null;
            GraticuleRenderingParams renderingParams = paramsKey != null ? this.namedParams.get(paramsKey) : null;
            
            if (renderable != null && renderable instanceof Polyline)
            {
                if (renderingParams == null || renderingParams.isDrawLines())
                {
                    applyRenderingParams(renderingParams, (Polyline) renderable);
                    ((Polyline) renderable).render(dc);
                }
            }
            else if (renderable != null && renderable instanceof GeographicText)
            {
                if (renderingParams == null || renderingParams.isDrawLabels())
                {
                    applyRenderingParams(renderingParams, (GeographicText) renderable);
                    text.add((GeographicText) renderable);
                }
            }
        }

        this.textRenderer.render(dc, text);
    }

    public GraticuleRenderingParams getRenderingParams(String key)
    {
        if (key == null)
        {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GraticuleRenderingParams value = this.namedParams.get(key);
        if (value == null)
        {
            value = new GraticuleRenderingParams();
            initRenderingParams(value);
            if (this.defaultParams != null)
                value.setValues(this.defaultParams);
            
            this.namedParams.put(key, value);
        }

        return value;
    }

    public Collection<Map.Entry<String, GraticuleRenderingParams>> getAllRenderingParams()
    {
        return this.namedParams.entrySet();
    }

    public void setRenderingParams(String key, GraticuleRenderingParams renderingParams)
    {
        if (key == null)
        {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        initRenderingParams(renderingParams);        
        this.namedParams.put(key, renderingParams);
    }

    public AVList getDefaultParams()
    {
        return this.defaultParams;
    }

    public void setDefaultParams(AVList defaultParams)
    {
        this.defaultParams = defaultParams;
    }

    private AVList initRenderingParams(AVList params)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.AVListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params.getValue(GraticuleRenderingParams.KEY_DRAW_LINES) == null)
            params.setValue(GraticuleRenderingParams.KEY_DRAW_LINES, Boolean.TRUE);

        if (params.getValue(GraticuleRenderingParams.KEY_LINE_COLOR) == null)
            params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, Color.WHITE);

        if (params.getValue(GraticuleRenderingParams.KEY_LINE_WIDTH) == null)
            //noinspection UnnecessaryBoxing
            params.setValue(GraticuleRenderingParams.KEY_LINE_WIDTH, new Double(1));

        if (params.getValue(GraticuleRenderingParams.KEY_LINE_STYLE) == null)
            params.setValue(GraticuleRenderingParams.KEY_LINE_STYLE, GraticuleRenderingParams.VALUE_LINE_STYLE_SOLID);

        if (params.getValue(GraticuleRenderingParams.KEY_DRAW_LABELS) == null)
            params.setValue(GraticuleRenderingParams.KEY_DRAW_LABELS, Boolean.TRUE);

        if (params.getValue(GraticuleRenderingParams.KEY_LABEL_COLOR) == null)
            params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.WHITE);

        if (params.getValue(GraticuleRenderingParams.KEY_LABEL_FONT) == null)
            params.setValue(GraticuleRenderingParams.KEY_LABEL_FONT, Font.decode("Arial-Bold-12"));

        return params;
    }

    private void applyRenderingParams(AVList params, Polyline polyline)
    {
        if (params != null && polyline != null)
        {
            // Apply "line" properties to the Polyline.
            Object o = params.getValue(GraticuleRenderingParams.KEY_LINE_COLOR);
            if (o != null && o instanceof Color)
            {
                polyline.setColor((Color) o);
            }

            Double lineWidth = AVListImpl.getDoubleValue(params, GraticuleRenderingParams.KEY_LINE_WIDTH);
            if (lineWidth != null)
            {
                polyline.setLineWidth(lineWidth);
            }

            String s = params.getStringValue(GraticuleRenderingParams.KEY_LINE_STYLE);
            // Draw a solid line.
            if (GraticuleRenderingParams.VALUE_LINE_STYLE_SOLID.equalsIgnoreCase(s))
            {
                polyline.setStipplePattern((short) 0xAAAA);
                polyline.setStippleFactor(0);
            }
            // Draw the line as longer strokes with space in between.
            else if (GraticuleRenderingParams.VALUE_LINE_STYLE_DASHED.equalsIgnoreCase(s))
            {
                int baseFactor = (int) (lineWidth != null ? Math.round(lineWidth) : 1.0);
                polyline.setStipplePattern((short) 0xAAAA);
                polyline.setStippleFactor(3 * baseFactor);
            }
            // Draw the line as a evenly spaced "square" dots.
            else if (GraticuleRenderingParams.VALUE_LINE_STYLE_DOTTED.equalsIgnoreCase(s))
            {
                int baseFactor = (int) (lineWidth != null ? Math.round(lineWidth) : 1.0);
                polyline.setStipplePattern((short) 0xAAAA);
                polyline.setStippleFactor(baseFactor);
            }
            // Set the line terrain conformance.
            Double d = AVListImpl.getDoubleValue(params, GraticuleRenderingParams.KEY_LINE_CONFORMANCE);
            if (d != null)
            {
                polyline.setTerrainConformance(d);
            }
        }
    }

    private void applyRenderingParams(AVList params, GeographicText text)
    {
        if (params != null && text != null)
        {
            // Apply "label" properties to the GeographicText.
            Object o = params.getValue(GraticuleRenderingParams.KEY_LABEL_COLOR);
            if (o != null && o instanceof Color)
            {
                Color color = (Color) o;
                float[] compArray = new float[4];
                Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), compArray);
                int colorValue = compArray[2] < .5f ? 255 : 0;
                text.setColor(color);
                text.setBackgroundColor(new Color(colorValue, colorValue, colorValue, color.getAlpha()));
            }

            o = params.getValue(GraticuleRenderingParams.KEY_LABEL_FONT);
            if (o != null && o instanceof Font)
            {
                text.setFont((Font) o);
            }
        }
    }
}
