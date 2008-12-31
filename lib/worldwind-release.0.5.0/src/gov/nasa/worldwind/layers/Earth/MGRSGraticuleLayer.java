/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GeographicText;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.UserFacingText;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.view.OrbitView;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 * @author Patrick Murris
 * @version $Id: MGRSGraticuleLayer.java 5175 2008-04-25 21:12:21Z patrickmurris $
 */

public class MGRSGraticuleLayer extends UTMGraticuleLayer
{
    /**
     * Graticule for the UTM grid.
     */
    public static final String GRATICULE_UTM_GRID = "Graticule.UTM.Grid";
    /**
     * Graticule for the 100,000 meter grid, nested inside the UTM grid.
     */
    public static final String GRATICULE_100000M = "Graticule.100000m";
    /**
     * Graticule for the 10,000 meter grid, nested inside the UTM grid.
     */
    public static final String GRATICULE_10000M = "Graticule.10000m";
    /**
     * Graticule for the 1,000 meter grid, nested inside the UTM grid.
     */
    public static final String GRATICULE_1000M = "Graticule.1000m";
    /**
     * Graticule for the 100 meter grid, nested inside the UTM grid.
     */
    public static final String GRATICULE_100M = "Graticule.100m";
    /**
     * Graticule for the 10 meter grid, nested inside the UTM grid.
     */
    public static final String GRATICULE_10M = "Graticule.10m";
    /**
     * Graticule for the 1 meter grid, nested inside the UTM grid.
     */
    public static final String GRATICULE_1M = "Graticule.1m";

    private GridZone[][] gridZones = new GridZone[20][60]; // row/col
    private double zoneMaxAltitude   = 5000e3;
    private double squareMaxAltitude = 3000e3;

    private MetricScaleSupport metricScaleSupport = new MetricScaleSupport();
    private int renderablesCount = 0;
    private int visibleCellsCount = 0;
    private long frameCount = 0;
    private double polylineTerrainConformance = 50;
    private Frustum viewFrustum;
    private Vec4 lastEyePoint;
    private double lastViewHeading = 0;
    private double lastViewPitch = 0;
    private double lastViewFOV = 0;
    private double lastVerticalExaggeration = 1;
    private Globe globe;

    /**
     * Creates a new <code>MGRSGraticuleLayer</code>, with default graticule attributes.
     */
    public MGRSGraticuleLayer()
    {
        initRenderingParams();
        this.setName(Logging.getMessage("layers.Earth.MGRSGraticule.Name"));
    }

    /**
     * Returns the maxiumum resolution graticule that will be rendered, or null if no graticules will be rendered.
     * By default, all graticules are rendered, and this will return GRATICULE_1M.
     *
     * @return maximum resolution rendered.
     */
    public String getMaximumGraticuleResolution()
    {
        String maxTypeDrawn = null;
        String[] orderedTypeList = getOrderedTypes();
        for (String type : orderedTypeList)
        {
            GraticuleRenderingParams params = getRenderingParams(type);
            if (params.isDrawLines())
            {
                maxTypeDrawn = type;
            }
        }
        return maxTypeDrawn;
    }

    /**
     * Sets the maxiumum resolution graticule that will be rendered.
     *
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setMaximumGraticuleResolution(String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        boolean pastTarget = false;
        String[] orderedTypeList = getOrderedTypes();
        for (String type : orderedTypeList)
        {
            // Enable all graticulte BEFORE and INCLUDING the target.
            // Disable all graticules AFTER the target.
            GraticuleRenderingParams params = getRenderingParams(type);
            params.setDrawLines(!pastTarget);

            if (!pastTarget && type.equals(graticuleType))
            {
                pastTarget = true;
            }
        }
    }

    /**
     * Returns the line color of the specified graticule.
     *
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @return Color of the the graticule line.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public Color getGraticuleLineColor(String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return getRenderingParams(graticuleType).getLineColor();
    }

    /**
     * Sets the line rendering color for the specified graticule.
     *
     * @param color the line color for the specified graticule.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if<code>color</code> is null,
     *                                  if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setGraticuleLineColor(Color color, String graticuleType)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(graticuleType).setLineColor(color);
    }

    /**
     * Sets the line rendering color for the specified graticules.
     *
     * @param color the line color for the specified graticules.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if<code>color</code> is null,
     *                                  if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setGraticuleLineColor(Color color, Iterable<String> graticuleType)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (String type : graticuleType)
        {
            setGraticuleLineColor(color, type);
        }
    }

    /**
     * Sets the line rendering color for all graticules.
     *
     * @param color the line color.
     * @throws IllegalArgumentException if <code>color</code> is null.
     */
    public void setGraticuleLineColor(Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] graticuleType = getOrderedTypes();
        for (String type : graticuleType)
        {
            setGraticuleLineColor(color, type);
        }
    }

    /**
     * Returns the line width of the specified graticule.
     *
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @return width of the graticule line.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public double getGraticuleLineWidth(String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return getRenderingParams(graticuleType).getLineWidth();
    }

    /**
     * Sets the line rendering width for the specified graticule.
     *
     * @param lineWidth the line rendering width for the specified graticule.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setGraticuleLineWidth(double lineWidth, String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(graticuleType).setLineWidth(lineWidth);
    }

    /**
     * Sets the line rendering width for the specified graticules.
     *
     * @param lineWidth the line rendering width for the specified graticules.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setGraticuleLineWidth(double lineWidth, Iterable<String> graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (String type : graticuleType)
        {
            setGraticuleLineWidth(lineWidth, type);
        }
    }

    /**
     * Sets the line rendering width for all graticules.
     *
     * @param lineWidth the line rendering width.
     */
    public void setGraticuleLineWidth(double lineWidth)
    {
        String[] graticuleType = getOrderedTypes();
        for (String type : graticuleType)
        {
            setGraticuleLineWidth(lineWidth, type);
        }
    }

    /**
     * Returns the line rendering style of the specified graticule.
     *
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @return line rendering style of the graticule.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public String getGraticuleLineStyle(String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return getRenderingParams(graticuleType).getLineStyle();
    }

    /**
     * Sets the line rendering style for the specified graticule.
     *
     * @param lineStyle the line rendering style for the specified graticule.
     *                  One of LINE_STYLE_PLAIN, LINE_STYLE_DASHED, or LINE_STYLE_DOTTED.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M
     * @throws IllegalArgumentException if <code>lineStyle</code> is null,
     *                                  if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setGraticuleLineStyle(String lineStyle, String graticuleType)
    {
        if (lineStyle == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(graticuleType).setLineStyle(lineStyle);
    }

    /**
     * Sets the line rendering style for the specified graticules.
     *
     * @param lineStyle the line rendering style for the specified graticules.
     *                  One of LINE_STYLE_PLAIN, LINE_STYLE_DASHED, or LINE_STYLE_DOTTED.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M
     * @throws IllegalArgumentException if <code>lineStyle</code> is null,
     *                                  if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setGraticuleLineStyle(String lineStyle, Iterable<String> graticuleType)
    {
        if (lineStyle == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (String type : graticuleType)
        {
            setGraticuleLineStyle(lineStyle, type);
        }
    }

    /**
     * Sets the line rendering style for all graticules.
     *
     * @param lineStyle the line rendering style.
     *                  One of LINE_STYLE_PLAIN, LINE_STYLE_DASHED, or LINE_STYLE_DOTTED.
     * @throws IllegalArgumentException if <code>lineStyle</code> is null.
     */
    public void setGraticuleLineStyle(String lineStyle)
    {
        if (lineStyle == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] graticuleType = getOrderedTypes();
        for (String type : graticuleType)
        {
            setGraticuleLineStyle(lineStyle, type);
        }
    }

    /**
     * Returns whether specified graticule labels will be rendered.
     *
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @return true if graticule labels are will be rendered; false otherwise.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public boolean isDrawLabels(String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return getRenderingParams(graticuleType).isDrawLabels();
    }

    /**
     * Sets whether the specified graticule labels will be rendered.
     * If true, the graticule labels will be rendered.
     * Otherwise, the graticule labels will not be rendered, but other graticules will not be affected.
     *
     * @param drawLabels true to render graticule labels; false to disable rendering.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setDrawLabels(boolean drawLabels, String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(graticuleType).setDrawLabels(drawLabels);
    }

    /**
     * Sets whether the specified graticule labels will be rendered.
     * If true, the graticule labels will be rendered.
     * Otherwise, the graticule labels will not be rendered, but other graticules will not be affected.
     *
     * @param drawLabels true to render graticule labels; false to disable rendering.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setDrawLabels(boolean drawLabels, Iterable<String> graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (String type : graticuleType)
        {
            setDrawLabels(drawLabels, type);
        }
    }

    /**
     * Sets whether all graticule labels will be rendered.
     * If true, all graticule labels will be rendered.
     * Otherwise, all graticule labels will not be rendered.
     *
     * @param drawLabels true to render all graticule labels; false to disable rendering.
     */
    public void setDrawLabels(boolean drawLabels)
    {
        String[] graticuleType = getOrderedTypes();
        for (String type : graticuleType)
        {
            setDrawLabels(drawLabels, type);
        }
    }

    /**
     * Returns the label color of the specified graticule.
     *
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @return Color of the the graticule label.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public Color getLabelColor(String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return getRenderingParams(graticuleType).getLabelColor();
    }

    /**
     * Sets the label rendering color for the specified graticule.
     *
     * @param color the label color for the specified graticule.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if<code>color</code> is null,
     *                                  if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setLabelColor(Color color, String graticuleType)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(graticuleType).setLabelColor(color);
    }

    /**
     * Sets the label rendering color for the specified graticules.
     *
     * @param color the label color for the specified graticules.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if<code>color</code> is null,
     *                                  if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setLabelColor(Color color, Iterable<String> graticuleType)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (String type : graticuleType)
        {
            setLabelColor(color, type);
        }
    }

    /**
     * Sets the label rendering color for all graticules.
     *
     * @param color the label color.
     * @throws IllegalArgumentException if <code>color</code> is null.
     */
    public void setLabelColor(Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] graticuleType = getOrderedTypes();
        for (String type : graticuleType)
        {
            setLabelColor(color, type);
        }
    }

    /**
     * Returns the label font of the specified graticule.
     *
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @return Font of the graticule label.
     * @throws IllegalArgumentException if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public Font getLabelFont(String graticuleType)
    {
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return getRenderingParams(graticuleType).getLabelFont();
    }

    /**
     * Sets the label rendering font for the specified graticule.
     *
     * @param font the label font for the specified graticule.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if<code>font</code> is null,
     *                                  if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setLabelFont(Font font, String graticuleType)
    {
        if (font == null)
        {
            String message = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(graticuleType).setLabelFont(font);
    }

    /**
     * Sets the label rendering font for the specified graticules.
     *
     * @param font the label font for the specified graticules.
     * @param graticuleType one of GRATICULE_UTM, GRATICULE_UTM_GRID, GRATICULE_100000M, GRATICULE_10000M,
     *                      GRATICULE_1000M, GRATICULE_100M, GRATICULE_10M, or GRATICULE_1M.
     * @throws IllegalArgumentException if<code>font</code> is null,
     *                                  if <code>graticuleType</code> is null,
     *                                  or if <code>graticuleType</code> is not a valid type.
     */
    public void setLabelFont(Font font, Iterable<String> graticuleType)
    {
        if (font == null)
        {
            String message = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (graticuleType == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (String type : graticuleType)
        {
            setLabelFont(font, type);
        }
    }

    /**
     * Sets the label rendering font for all graticules.
     *
     * @param font the label font.
     * @throws IllegalArgumentException if <code>font</code> is null.
     */
    public void setLabelFont(Font font)
    {
        if (font == null)
        {
            String message = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] graticuleType = getOrderedTypes();
        for (String type : graticuleType)
        {
            setLabelFont(font, type);
        }
    }

    private void initRenderingParams()
    {
        GraticuleRenderingParams params;
        // UTM graticule
        params = new GraticuleRenderingParams();
        params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, Color.YELLOW);
        params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.YELLOW);
        params.setValue(GraticuleRenderingParams.KEY_LABEL_FONT, Font.decode("Arial-Bold-16"));
        setRenderingParams(GRATICULE_UTM_GRID, params);
        // 100,000 meter graticule
        params = new GraticuleRenderingParams();
        params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, Color.GREEN);
        params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.GREEN);
        params.setValue(GraticuleRenderingParams.KEY_LABEL_FONT, Font.decode("Arial-Bold-14"));
        setRenderingParams(GRATICULE_100000M, params);
        // 10,000 meter graticule
        params = new GraticuleRenderingParams();
        params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, new Color(0, 102, 255));
        params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, new Color(0, 102, 255));
        setRenderingParams(GRATICULE_10000M, params);
        // 1,000 meter graticule
        params = new GraticuleRenderingParams();
        params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, Color.CYAN);
        params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.CYAN);
        setRenderingParams(GRATICULE_1000M, params);
        // 100 meter graticule
        params = new GraticuleRenderingParams();
        params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, new Color(0, 153, 153));
        params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, new Color(0, 153, 153));
        setRenderingParams(GRATICULE_100M, params);
        // 10 meter graticule
        params = new GraticuleRenderingParams();
        params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, new Color(102, 255, 204));
        params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, new Color(102, 255, 204));
        setRenderingParams(GRATICULE_10M, params);
        // 1 meter graticule
        params = new GraticuleRenderingParams();
        params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, new Color(153, 153, 255));
        params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, new Color(153, 153, 255));
        setRenderingParams(GRATICULE_1M, params);
    }

    private String[] getOrderedTypes()
    {
        return new String[] {
            GRATICULE_UTM_GRID,
            GRATICULE_100000M,
            GRATICULE_10000M,
            GRATICULE_1000M,
            GRATICULE_100M,
            GRATICULE_10M,
            GRATICULE_1M,
        };
    }

    private String getTypeFor(int resolution)
    {
        String graticuleType = null;
        switch (resolution)
        {
            case 100000: // 100,000 meters
                graticuleType = GRATICULE_100000M;
                break;
            case 10000:  // 10,000 meters
                graticuleType = GRATICULE_10000M;
                break;
            case 1000:   // 1000 meters
                graticuleType = GRATICULE_1000M;
                break;
            case 100:    // 100 meters
                graticuleType = GRATICULE_100M;
                break;
            case 10:     // 10 meters
                graticuleType = GRATICULE_10M;
                break;
            case 1:      // 1 meter
                graticuleType = GRATICULE_1M;
                break;
        }

        return graticuleType;
    }

    // --- Renderable layer --------------------------------------------------------------

    public void doRender(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (this.needsToUpdate(dc))
        {
            // Init frame
            removeAllRenderables();
            this.renderablesCount = 0;
            this.visibleCellsCount = 0;
            this.frameCount++;
            this.polylineTerrainConformance = computeTerrainConformance(dc);
            applyTerrainConformance();
            this.viewFrustum = dc.getView().getFrustumInModelCoordinates();
            this.lastEyePoint = dc.getView().getEyePoint();
            this.lastViewFOV = dc.getView().getFieldOfView().degrees;
            if (dc.getView() instanceof OrbitView)
            {
                this.lastViewHeading = ((OrbitView)dc.getView()).getHeading().degrees;
                this.lastViewPitch = ((OrbitView)dc.getView()).getPitch().degrees;
            }
            this.lastVerticalExaggeration = dc.getVerticalExaggeration();
            this.globe = dc.getGlobe();

            this.metricScaleSupport.clear();
            this.metricScaleSupport.computeZone(dc);
            Sector vs = computeVisibleSector(dc);
            // Select renderables
            if (dc.getView().getEyePosition().getElevation() <= this.zoneMaxAltitude)
            {
                selectMGRSRenderables(dc, vs);
                this.metricScaleSupport.selectRenderables(dc);
                //System.out.println("MGRS visible: " + visibleCellsCount + " renderables: " + this.renderablesCount + " vs: " + vs);
            }
            else
            {
                super.selectUTMRenderables(dc);
            }
        }
        // Render
        super.renderGraticule(dc);
    }

    private void applyTerrainConformance()
    {
        String[] graticuleType = getOrderedTypes();
        for (String type : graticuleType)
        {
            getRenderingParams(type).setValue(
                GraticuleRenderingParams.KEY_LINE_CONFORMANCE, this.polylineTerrainConformance);
        }
    }

    /**
     * Determines whether the grid should be updated. It returns true if:
     * <ul>
     * <li>the eye has moved more than 1% of its altitude above ground
     * <li>the view FOV, heading or pitch have changed more than 1 degree
     * <li>vertical exaggeration has changed
     * </ul
     * @param dc
     * @return true if the graticule should be updated.
     */
    private boolean needsToUpdate(DrawContext dc)
    {
        if (this.lastEyePoint == null)
            return true;

        View view = dc.getView();
        double altitudeAboveGround = computeAltitudeAboveGround(dc);
        if (view.getEyePoint().distanceTo3(this.lastEyePoint) > altitudeAboveGround / 100)  // 1% of AAG
            return true;

        if (this.lastVerticalExaggeration != dc.getVerticalExaggeration())
            return true;

        if (view instanceof OrbitView)
        {
            if (Math.abs(this.lastViewHeading - ((OrbitView)view).getHeading().degrees) > 1)
                return true;
            if (Math.abs(this.lastViewPitch - ((OrbitView)view).getPitch().degrees) > 1)
                return true;
        }

        if (Math.abs(this.lastViewFOV - view.getFieldOfView().degrees) > 1)
            return true;

        return false;
    }

    private double computeTerrainConformance(DrawContext dc)
    {
        int value = 100;
        double alt = dc.getView().getEyePosition().getElevation();
        if (alt < 10e3)
            value = 20;
        else if (alt < 50e3)
            value = 30;
        else if (alt < 100e3)
            value = 40;
        else if (alt < 1000e3)
            value = 60;

        return value;
    }

    private Sector computeVisibleSector(DrawContext dc)
    {
        return dc.getVisibleSector();
    }

    private void selectMGRSRenderables(DrawContext dc, Sector vs)
    {
        ArrayList<GridZone> zoneList = getVisibleZones(dc);
        if (zoneList.size() > 0)
        {
            for (GridZone gz : zoneList)
            {
                // Select visible grid zones elements
                gz.selectRenderables(dc, vs, this);
            }
        }
    }

    private ArrayList<GridZone> getVisibleZones(DrawContext dc)
    {
        ArrayList<GridZone> zoneList = new ArrayList<GridZone>();
        Sector vs = dc.getVisibleSector();
        if (vs != null)
        {
            Rectangle2D gridRectangle = getGridRectangleForSector(vs);
            if (gridRectangle != null)
            {
                for (int row = (int)gridRectangle.getY(); row <= gridRectangle.getY() + gridRectangle.getHeight(); row++)
                {
                    for (int col = (int)gridRectangle.getX(); col <= gridRectangle.getX() + gridRectangle.getWidth(); col++)
                    {
                        if (row != 19 || (col != 31 && col != 33 && col != 35)) // ignore X32, 34 and 36
                        {
                            if (gridZones[row][col] == null)
                                gridZones[row][col] = new GridZone(getGridSector(row, col));
                            if (gridZones[row][col].isInView(dc))
                                zoneList.add(gridZones[row][col]);
                            else
                                gridZones[row][col].clearRenderables();
                        }
                    }
                }
            }
        }
        return zoneList;
    }

    private Rectangle2D getGridRectangleForSector(Sector sector)
    {
        Rectangle2D rectangle = null;
        if (sector.getMinLatitude().degrees < 84 && sector.getMaxLatitude().degrees > -80)
        {
            Sector gridSector = Sector.fromDegrees(
                    Math.max(sector.getMinLatitude().degrees, -80), Math.min(sector.getMaxLatitude().degrees, 84),
                    sector.getMinLongitude().degrees, sector.getMaxLongitude().degrees);
            int x1 = getGridColumn(gridSector.getMinLongitude().degrees);
            int x2 = getGridColumn(gridSector.getMaxLongitude().degrees);
            int y1 = getGridRow(gridSector.getMinLatitude().degrees);
            int y2 = getGridRow(gridSector.getMaxLatitude().degrees);
            // Adjust rectangle to include special zones
            if (y1 <= 17 && y2 >= 17 && x2 == 30) // 32V Norway
                x2 = 31;
            if (y1 <= 19 && y2 >= 19) // X band
            {
                if (x1 == 31) // 31X
                    x1 = 30;
                if (x2 == 31) // 33X
                    x2 = 32;
                if (x1 == 33) // 33X
                    x1 = 32;
                if (x2 == 33) // 35X
                    x2 = 34;
                if (x1 == 35) // 35X
                    x1 = 34;
                if (x2 == 35) // 37X
                    x2 = 36;
            }
            rectangle = new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }
        return rectangle;
    }

    private int getGridColumn(Double longitude)
    {
        int col = (int)Math.floor((longitude + 180) / 6d);
        return Math.min(col, 59);
    }

    private int getGridRow(Double latitude)
    {
        int row = (int)Math.floor((latitude + 80) / 8d);
        return Math.min(row, 19);
    }

    private Sector getGridSector(int row, int col)
    {
        int minLat = -80 + row * 8;
        int maxLat = minLat + (minLat != 72 ? 8 : 12);
        int minLon = -180 + col * 6;
        int maxLon = minLon + 6;
        // Special sectors
        if (row == 17 && col == 30)         // 31V
            maxLon -= 3;
        else if (row == 17 && col == 31)    // 32V
            minLon -= 3;
        else if (row == 19 && col == 30 )   // 31X
            maxLon += 3;
        else if (row == 19 && col == 31 )   // 32X does not exist
            {minLon += 3; maxLon -= 3;}
        else if (row == 19 && col == 32 )   // 33X
            {minLon -= 3; maxLon += 3;}
        else if (row == 19 && col == 33 )   // 34X does not exist
            {minLon += 3; maxLon -= 3;}
        else if (row == 19 && col == 34 )   // 35X
            {minLon -= 3; maxLon += 3;}
        else if (row == 19 && col == 35 )   // 36X does not exist
            {minLon += 3; maxLon -= 3;}
        else if (row == 19 && col == 36 )   // 37X
            minLon -= 3;
        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
    }

    private boolean isNorthNeighborInView(GridZone gz, DrawContext dc)
    {
        int row = getGridRow(gz.sector.getCentroid().getLatitude().degrees);
        int col = getGridColumn(gz.sector.getCentroid().getLongitude().degrees);
        GridZone neighbor = row + 1 <= 19 ? this.gridZones[row + 1][col] : null;
        return neighbor != null ? neighbor.isInView(dc) : false;
    }

    private boolean isEastNeighborInView(GridZone gz, DrawContext dc)
    {
        int row = getGridRow(gz.sector.getCentroid().getLatitude().degrees);
        int col = getGridColumn(gz.sector.getCentroid().getLongitude().degrees);
        GridZone neighbor = col + 1 <= 59 ? this.gridZones[row][col + 1] : null;
        return neighbor != null ? neighbor.isInView(dc) : false;
    }


    private Vec4 getSurfacePoint(DrawContext dc, Angle latitude, Angle longitude)
    {
        Vec4 surfacePoint = dc.getSurfaceGeometry().getSurfacePoint(latitude, longitude);
        if (surfacePoint == null)
            surfacePoint = dc.getGlobe().computePointFromPosition(new Position(latitude, longitude,
                    dc.getGlobe().getElevation(latitude, longitude)));
        return surfacePoint;
    }

    private double computeAltitudeAboveGround(DrawContext dc)
    {
        View view = dc.getView();
        Position eyePosition = view.getEyePosition();
        Vec4 surfacePoint = getSurfacePoint(dc, eyePosition.getLatitude(), eyePosition.getLongitude());
        return view.getEyePoint().distanceTo3(surfacePoint);
    }


    //--- Metric scale support -----------------------------------------------------------

    private class MetricScaleSupport
    {
        private int zone;

        private double offsetFactorX = -.5;
        private double offsetFactorY = -.5;
        private double visibleDistanceFactor = 10;

        // 5 levels 100km to 10m
        UTMExtremes[] extremes = new UTMExtremes[5];

        private class UTMExtremes
        {
            protected double minX, maxX, minY, maxY;
            protected char minYHemisphere, maxYHemisphere;

            public UTMExtremes()
            {
                this.clear();
            }

            public void clear()
            {
                minX = 1e6;
                maxX = 0;
                minY = 10e6;
                minYHemisphere = 'N';
                maxY = 0;
                maxYHemisphere = 'S';
            }
        }

        public int getZone()
        {
            return this.zone;
        }

        private void computeZone(DrawContext dc)
        {
            this.zone = 0;
            try
            {
                Position centerPos = ((OrbitView)dc.getView()).getCenterPosition();
                if (centerPos != null)
                {
                    UTMCoord UTM = UTMCoord.fromLatLon(centerPos.getLatitude(), centerPos.getLongitude(), dc.getGlobe());
                    this.zone = UTM.getZone();
                }
            }
            catch (Exception ex)
            {
                this.zone = 0;
            }
        }

        public void clear()
        {
            for (int i = 0; i < 5; i++)
            {
                if (this.extremes[i] == null)
                    this.extremes[i] = new UTMExtremes();
                this.extremes[i].clear();
            }
        }

        public void computeMetricScaleExtremes(GridZone gz, GridElement ge, double size)
        {
            if (gz.UTMZone != this.zone)
                return;
            if (size < 1 || size > 100e3)
                return;

            UTMExtremes levelExtremes = this.extremes[(int)Math.log10(size) - 1];

            if (ge.type.equals(GridElement.TYPE_LINE_EASTING)
                    || ge.type.equals(GridElement.TYPE_LINE_EAST)
                    || ge.type.equals(GridElement.TYPE_LINE_WEST))
            {
                levelExtremes.minX = ge.value < levelExtremes.minX ? ge.value : levelExtremes.minX;
                levelExtremes.maxX = ge.value > levelExtremes.maxX ? ge.value : levelExtremes.maxX;
            }
            else if (ge.type.equals(GridElement.TYPE_LINE_NORTHING)
                    || ge.type.equals(GridElement.TYPE_LINE_SOUTH)
                    || ge.type.equals(GridElement.TYPE_LINE_NORTH))
            {
                if (gz.hemisphere == levelExtremes.minYHemisphere)
                    levelExtremes.minY = ge.value < levelExtremes.minY ? ge.value : levelExtremes.minY;
                else if (gz.hemisphere == 'S')
                {
                    levelExtremes.minY = ge.value;
                    levelExtremes.minYHemisphere = gz.hemisphere;
                }
                if (gz.hemisphere == levelExtremes.maxYHemisphere)
                    levelExtremes.maxY = ge.value > levelExtremes.maxY ? ge.value : levelExtremes.maxY;
                else if (gz.hemisphere == 'N')
                {
                    levelExtremes.maxY = ge.value;
                    levelExtremes.maxYHemisphere = gz.hemisphere;
                }
            }
        }

        public void selectRenderables(DrawContext dc)
        {
            try
            {
                OrbitView view = (OrbitView)dc.getView();
                // Compute easting and northing label offsets
                Double pixelSize = view.computePixelSizeAtDistance(view.getZoom());
                Double eastingOffset = view.getViewport().width * pixelSize * offsetFactorX / 2;
                Double northingOffset = view.getViewport().height * pixelSize * offsetFactorY / 2;
                // Derive labels center pos from the view center
                Position centerPos = view.getCenterPosition();
                UTMCoord UTM = UTMCoord.fromLatLon(centerPos.getLatitude(), centerPos.getLongitude(), dc.getGlobe());
                double labelEasting = UTM.getEasting() + eastingOffset;
                double labelNorthing = UTM.getNorthing() + northingOffset;
                char labelHemisphere = UTM.getHemisphere();

                for (int i = 0; i < 5; i++)
                {
                    UTMExtremes levelExtremes = this.extremes[i];
                    double gridStep = Math.pow(10, i);
                    double gridStepTimesTen = gridStep * 10;
                    String graticuleType = getTypeFor((int) gridStep);
                    if (levelExtremes.minX <= levelExtremes.maxX)
                    {
                        // Process easting scale labels for this level
                        for (double easting = levelExtremes.minX; easting <= levelExtremes.maxX; easting += gridStep)
                        {
                            if (i == 4 || easting % gridStepTimesTen != 0)
                            {
                                try
                                {
                                    UTM = UTMCoord.fromUTM(this.zone,  labelHemisphere, easting, labelNorthing,
                                            dc.getGlobe());
                                    Angle lat = UTM.getLatitude();
                                    Angle lon = UTM.getLongitude();
                                    Vec4 surfacePoint = getSurfacePoint(dc, lat, lon);
                                    if(viewFrustum.contains(surfacePoint) && isPointInRange(dc, surfacePoint))
                                    {
                                        String text = String.valueOf((int)(easting % 100e3));
                                        GeographicText gt = new UserFacingText(text, new Position(lat, lon, 0));
                                        addRenderable(gt, graticuleType);
                                        renderablesCount++;
                                    }
                                }
                                catch (IllegalArgumentException ignore) {}
                            }
                        }
                    }
                    if (!(levelExtremes.maxYHemisphere == 'S' && levelExtremes.maxY == 0))
                    {
                        // Process northing scale labels for this level
                        char currentHemisphere = levelExtremes.minYHemisphere;
                        for (double northing = levelExtremes.minY; (northing <= levelExtremes.maxY)
                                || (currentHemisphere != levelExtremes.maxYHemisphere); northing += gridStep)
                        {
                            if (i == 4 || northing % gridStepTimesTen != 0)
                            {
                                try
                                {
                                    UTM = UTMCoord.fromUTM(this.zone, currentHemisphere, labelEasting, northing,
                                            dc.getGlobe());
                                    Angle lat = UTM.getLatitude();
                                    Angle lon = UTM.getLongitude();
                                    Vec4 surfacePoint = getSurfacePoint(dc, lat, lon);
                                    if(viewFrustum.contains(surfacePoint) && isPointInRange(dc, surfacePoint))
                                    {
                                        String text = String.valueOf((int)(northing % 100e3));
                                        GeographicText gt = new UserFacingText(text, new Position(lat, lon, 0));
                                        addRenderable(gt, graticuleType);
                                        renderablesCount++;
                                    }
                                }
                                catch (IllegalArgumentException ignore) {}

                                if (currentHemisphere != levelExtremes.maxYHemisphere && northing >= 10e6 - gridStep)
                                {
                                    // Switch hemisphere
                                    currentHemisphere = levelExtremes.maxYHemisphere;
                                    northing = -gridStep;
                                }
                            }
                        }
                    } // end northing
                } // for levels
            }
            catch (IllegalArgumentException ignore) {}
        }

        private boolean isPointInRange(DrawContext dc, Vec4 point)
        {
            double altitudeAboveGround = computeAltitudeAboveGround(dc);
            return dc.getView().getEyePoint().distanceTo3(point)
                    < altitudeAboveGround * this.visibleDistanceFactor;
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++)
            {
                sb.append("level ");
                sb.append(String.valueOf(i));
                sb.append(" : ");
                UTMExtremes levelExtremes = this.extremes[i];
                if (levelExtremes.minX < levelExtremes.maxX ||
                        !(levelExtremes.maxYHemisphere == 'S' && levelExtremes.maxY == 0))
                {
                    sb.append(levelExtremes.minX);
                    sb.append(", ");
                    sb.append(levelExtremes.maxX);
                    sb.append(" - ");
                    sb.append(levelExtremes.minY);
                    sb.append(levelExtremes.minYHemisphere);
                    sb.append(", ");
                    sb.append(levelExtremes.maxY);
                    sb.append(levelExtremes.maxYHemisphere);
                }
                else
                {
                    sb.append("empty");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    //--- Grid zone ----------------------------------------------------------------------

    /**
     * Represent a UTM zone / latitude band intersection
     */
    private class GridZone
    {
        private static final double ONEHT = 100e3;

        private Sector sector;
        private String name = "";
        private int UTMZone = 0;
        private char hemisphere = ' ';
        private Extent extent;
        private double extentVerticalExaggeration = 1;

        private ArrayList<GridElement> gridElements;
        private ArrayList<SquareZone> squares;

        public GridZone(Sector sector)
        {
            this.sector = sector;
            try
            {
                MGRSCoord MGRS = MGRSCoord.fromLatLon(sector.getCentroid().getLatitude(),
                        sector.getCentroid().getLongitude(), globe);
                this.name = MGRS.toString().substring(0, 3);
                UTMCoord UTM = UTMCoord.fromLatLon(sector.getCentroid().getLatitude(),
                        sector.getCentroid().getLongitude(), globe);
                this.UTMZone = UTM.getZone();
                this.hemisphere = UTM.getHemisphere();
            }
            catch (IllegalArgumentException ignore) {}
        }

        public Extent getExtent(Globe globe, double ve)
        {
            if (this.extent == null || ve != this.extentVerticalExaggeration)
            {
                this.extent = globe.computeBoundingCylinder(ve, this.sector);
                this.extentVerticalExaggeration = ve;
            }
            return this.extent;
        }

        public boolean isInView(DrawContext dc)
        {
            return viewFrustum.intersects(this.getExtent(dc.getGlobe(), dc.getVerticalExaggeration()));
        }

        public void selectRenderables(DrawContext dc, Sector vs, MGRSGraticuleLayer layer)
        {
            // Select zone elements
            if (this.gridElements == null)
                createRenderables();

            for (GridElement ge : this.gridElements)
            {
                if (ge.isInView(dc, vs))
                {
                    if (ge.type.equals(GridElement.TYPE_LINE_NORTH) && isNorthNeighborInView(this, dc))
                        continue;
                    if (ge.type.equals(GridElement.TYPE_LINE_EAST) && isEastNeighborInView(this, dc))
                        continue;

                    layer.addRenderable(ge.renderable, GRATICULE_UTM_GRID);
                    renderablesCount++;
                }
            }

            if (dc.getView().getEyePosition().getElevation() > MGRSGraticuleLayer.this.squareMaxAltitude)
                return;

            // Select 100km squares elements
            if (this.squares == null)
                createSquares();
            for (SquareZone sz : this.squares)
            {
                if (sz.isInView(dc))
                {
                    sz.selectRenderables(dc, vs, layer);
                    visibleCellsCount++;
                }
                else
                    sz.clearRenderables();
            }
        }

        public void clearRenderables()
        {
            if (this.gridElements != null)
            {
                this.gridElements.clear();
                this.gridElements = null;
            }
            if (this.squares != null)
            {
                for (SquareZone sz : this.squares)
                    sz.clearRenderables();
                this.squares.clear();
                this.squares = null;
            }
        }

        private void createSquares()
        {
            this.squares = new ArrayList<SquareZone>();
            try
            {
                // Find grid zone easting and northing boundaries
                UTMCoord UTM;
                UTM = UTMCoord.fromLatLon(this.sector.getMinLatitude(), this.sector.getCentroid().getLongitude(), globe);
                double minNorthing = UTM.getNorthing();
                UTM = UTMCoord.fromLatLon(this.sector.getMaxLatitude(), this.sector.getCentroid().getLongitude(), globe);
                double maxNorthing = UTM.getNorthing();
                maxNorthing = maxNorthing == 0 ? 10e6 : maxNorthing;
                UTM = UTMCoord.fromLatLon(this.sector.getMinLatitude(), this.sector.getMinLongitude(), globe);
                double minEasting = UTM.getEasting();
                UTM = UTMCoord.fromLatLon(this.sector.getMaxLatitude(), this.sector.getMinLongitude(), globe);
                minEasting = UTM.getEasting() < minEasting ? UTM.getEasting() : minEasting;
                double maxEasting = 1e6 - minEasting;

                // Compensate for some distorted zones
                if (this.name.equals("32V")) // catch KS and LS in 32V
                    maxNorthing += 20e3;
                if (this.name.equals("31X")) // catch GA and GV in 31X
                    maxEasting += ONEHT;

                // Create squares
                //int count = 0;
                double startEasting = Math.floor(minEasting / ONEHT) * ONEHT;
                double startNorthing = Math.floor(minNorthing / ONEHT) * ONEHT;
                int cols = (int)Math.ceil((maxEasting - startEasting) / ONEHT);
                int rows = (int)Math.ceil((maxNorthing - startNorthing) / ONEHT);
                SquareZone[][] squaresArray = new SquareZone[rows][cols];
                int col = 0;
                for (double easting = startEasting; easting < maxEasting; easting += ONEHT)
                {
                    int row = 0;
                    for (double northing = startNorthing; northing < maxNorthing; northing += ONEHT)
                    {
                        SquareZone sz = new SquareZone(this, easting, northing, ONEHT);
                        if (sz.boundingSector != null && !sz.isOutsideGridZone())
                        {
                            this.squares.add(sz);
                            squaresArray[row][col] = sz;
                            //count++;
                        }
                        row++;
                    }
                    col++;
                }

                // Keep track of neighbors
                for (col = 0; col < cols; col++)
                    for (int row = 0; row < rows; row++)
                    {
                        SquareZone sz = squaresArray[row][col];
                        if (sz != null)
                        {
                            sz.setNorthNeighbor(row + 1 < rows ? squaresArray[row + 1][col] : null);
                            sz.setEastNeighbor(col + 1 < cols ? squaresArray[row][col + 1] : null);
                        }
                    }
            }
            catch (IllegalArgumentException ignore) {}
        }

        private void createRenderables()
        {
            this.gridElements = new ArrayList<GridElement>();

            ArrayList<Position> positions = new ArrayList<Position>();

            // left meridian segment
            positions.clear();
            //for (double lat = this.sector.getMinLatitude().degrees; lat <= this.sector.getMaxLatitude().degrees; lat += this.sector.getDeltaLat().degrees / 5d)
            //    positions.add(new Position(Angle.fromDegrees(lat), this.sector.getMinLongitude(), 10e3));
            positions.add(new Position(this.sector.getMinLatitude(), this.sector.getMinLongitude(), 10e3));
            positions.add(new Position(this.sector.getMaxLatitude(), this.sector.getMinLongitude(), 10e3));
            Polyline polyline = new Polyline(positions);
            polyline.setPathType(Polyline.LINEAR);
            polyline.setFollowTerrain(true);
            polyline.setTerrainConformance(50);
            Sector lineSector = new Sector(this.sector.getMinLatitude(), this.sector.getMaxLatitude(),
                    this.sector.getMinLongitude(), this.sector.getMinLongitude());
            this.gridElements.add(new GridElement(lineSector, polyline, GridElement.TYPE_LINE_WEST));

            // right meridian segment
            positions.clear();
            //for (double lat = this.sector.getMinLatitude().degrees; lat <= this.sector.getMaxLatitude().degrees; lat += this.sector.getDeltaLat().degrees / 5d)
            //    positions.add(new Position(Angle.fromDegrees(lat), this.sector.getMaxLongitude(), 10e3));
            positions.add(new Position(this.sector.getMinLatitude(), this.sector.getMaxLongitude(), 10e3));
            positions.add(new Position(this.sector.getMaxLatitude(), this.sector.getMaxLongitude(), 10e3));
            polyline = new Polyline(positions);
            polyline.setPathType(Polyline.LINEAR);
            polyline.setFollowTerrain(true);
            polyline.setTerrainConformance(50);
            lineSector = new Sector(this.sector.getMinLatitude(), this.sector.getMaxLatitude(),
                    this.sector.getMaxLongitude(), this.sector.getMaxLongitude());
            this.gridElements.add(new GridElement(lineSector, polyline, GridElement.TYPE_LINE_EAST));

            // bottom parallel segment
            positions.clear();
            //for (double lon = this.sector.getMinLongitude().degrees; lon <= this.sector.getMaxLongitude().degrees; lon += this.sector.getDeltaLon().degrees / 5d)
            //    positions.add(new Position(this.sector.getMinLatitude(), Angle.fromDegrees(lon), 10e3));
            positions.add(new Position(this.sector.getMinLatitude(), this.sector.getMinLongitude(), 10e3));
            positions.add(new Position(this.sector.getMinLatitude(), this.sector.getMaxLongitude(), 10e3));
            polyline = new Polyline(positions);
            polyline.setPathType(Polyline.LINEAR);
            polyline.setFollowTerrain(true);
            polyline.setTerrainConformance(20);
            lineSector = new Sector(this.sector.getMinLatitude(), this.sector.getMinLatitude(),
                    this.sector.getMinLongitude(), this.sector.getMaxLongitude());
            this.gridElements.add(new GridElement(lineSector, polyline, GridElement.TYPE_LINE_SOUTH));

            // top parallel segment
            positions.clear();
            //for (double lon = this.sector.getMinLongitude().degrees; lon <= this.sector.getMaxLongitude().degrees; lon += this.sector.getDeltaLon().degrees / 5d)
            //    positions.add(new Position(this.sector.getMaxLatitude(), Angle.fromDegrees(lon), 10e3));
            positions.add(new Position(this.sector.getMaxLatitude(), this.sector.getMinLongitude(), 10e3));
            positions.add(new Position(this.sector.getMaxLatitude(), this.sector.getMaxLongitude(), 10e3));
            polyline = new Polyline(positions);
            polyline.setPathType(Polyline.LINEAR);
            polyline.setFollowTerrain(true);
            polyline.setTerrainConformance(20);
            lineSector = new Sector(this.sector.getMaxLatitude(), this.sector.getMaxLatitude(),
                    this.sector.getMinLongitude(), this.sector.getMaxLongitude());
            this.gridElements.add(new GridElement(lineSector, polyline, GridElement.TYPE_LINE_NORTH));

            // Label
            GeographicText text = new UserFacingText(this.name, new Position(this.sector.getCentroid(), 0));
            this.gridElements.add(new GridElement(this.sector, text, GridElement.TYPE_GRIDZONE_LABEL));
        }

        public boolean isPositionInside(Position position)
        {
            return position != null ? this.sector.contains(position.getLatLon()) : false;
        }

        /**
         * Computes the intersection point position between a great circle segment and a meridian.
         * @param p1 the great circle segment start position.
         * @param p2 the great circle segment end position.
         * @param longitude the meridian longitude <code>Angle</code>
         * @return the intersection <code>Position</code> or null if there was no intersection found.
         */
        public LatLon greatCircleIntersectionAtLongitude(LatLon p1, LatLon p2, Angle longitude)
        {
            if (p1.getLongitude().degrees == longitude.degrees)
                return p1;
            if (p2.getLongitude().degrees == longitude.degrees)
                return p2;
            LatLon pos = null;
            Double deltaLon = getDeltaLongitude(p1, p2.getLongitude()).degrees;
            if (getDeltaLongitude(p1, longitude).degrees < deltaLon
                    && getDeltaLongitude(p2, longitude).degrees < deltaLon)
            {
                int count = 0;
                double precision = 1d / 6378137d; // 1m angle in radians
                LatLon a = p1;
                LatLon b = p2;
                LatLon midPoint = greatCircleMidPoint(a, b);
                while (getDeltaLongitude(midPoint, longitude).radians > precision && count <= 20)
                {
                    count++;
                    if (getDeltaLongitude(a, longitude).degrees < getDeltaLongitude(b, longitude).degrees)
                        b = midPoint;
                    else
                        a = midPoint;
                    midPoint = greatCircleMidPoint(a, b);
                }
                pos = midPoint;
                if (count >= 20)
                    System.out.println("Warning dichotomy loop aborted: " + p1 + " - " + p2 + " for lon " + longitude + " = " + pos);
            }
            // Adjust final longitude for an exact match
            if (pos != null)
                pos = new LatLon(pos.getLatitude(), longitude);
            return pos;
        }

        /**
         * Computes the intersection point position between a great circle segment and a parallel.
         * @param p1 the great circle segment start position.
         * @param p2 the great circle segment end position.
         * @param latitude the parallel latitude <code>Angle</code>
         * @return the intersection <code>Position</code> or null if there was no intersection found.
         */
        public LatLon greatCircleIntersectionAtLatitude(LatLon p1, LatLon p2, Angle latitude)
        {
            LatLon pos = null;
            if (Math.signum(p1.getLatitude().degrees - latitude.degrees)
                    != Math.signum(p2.getLatitude().degrees - latitude.degrees))
            {
                int count = 0;
                double precision = 1d / 6378137d; // 1m angle in radians
                LatLon a = p1;
                LatLon b = p2;
                LatLon midPoint = greatCircleMidPoint(a, b);
                while (Math.abs(midPoint.getLatitude().radians - latitude.radians) > precision && count <= 20)
                {
                    count++;
                    if (Math.signum(a.getLatitude().degrees - latitude.degrees)
                            != Math.signum(midPoint.getLatitude().degrees - latitude.degrees))
                        b = midPoint;
                    else
                        a = midPoint;
                    midPoint = greatCircleMidPoint(a, b);
                }
                pos = midPoint;
                if (count >= 20)
                    System.out.println("Warning dichotomy loop aborted: " + p1 + " - " + p2 + " for lat " + latitude + " = " + pos);
            }
            // Adjust final latitude for an exact match
            if (pos != null)
                pos = new LatLon(latitude, pos.getLongitude());
            return pos;
        }

        public LatLon greatCircleMidPoint(LatLon p1, LatLon p2)
        {
            Angle azimuth = LatLon.greatCircleAzimuth(p1, p2);
            Angle distance = LatLon.greatCircleDistance(p1, p2);
            return LatLon.greatCircleEndPosition(p1, azimuth.radians, distance.radians / 2);
        }

        public Angle getDeltaLongitude(LatLon p1, Angle longitude)
        {
            double deltaLon = Math.abs(p1.getLongitude().degrees - longitude.degrees);
            return Angle.fromDegrees(deltaLon < 180 ? deltaLon : 360 - deltaLon);
        }

    }

    // --- UTM square zone ------------------------------------------------------------------

    /**
     * Represent a generic UTM square area
     */
    private class SquareSector
    {
        public static final int MIN_CELL_SIZE_PIXELS = 50;

        protected GridZone gridZone;
        protected double SWEasting;
        protected double SWNorthing;
        protected double size;

        protected Position sw, se, nw, ne;  // Four corners position
        protected Sector boundingSector;
        protected LatLon centroid;
        protected LatLon squareCenter;
        protected Vec4 centerPoint;
        private Extent extent;
        private double extentVerticalExaggeration = 1;
        protected boolean isTruncated = false;

        public SquareSector(GridZone parent, double SWEasting, double SWNorthing, double size)
        {
            this.gridZone = parent;
            this.SWEasting = SWEasting;
            this.SWNorthing = SWNorthing;
            this.size = size;

            // Compute corners positions
            this.sw = computePositionFromUTM(parent.UTMZone, parent.hemisphere,  SWEasting, SWNorthing);
            this.se = computePositionFromUTM(parent.UTMZone, parent.hemisphere,  SWEasting + size, SWNorthing);
            this.nw = computePositionFromUTM(parent.UTMZone, parent.hemisphere,  SWEasting, SWNorthing + size);
            this.ne = computePositionFromUTM(parent.UTMZone, parent.hemisphere,  SWEasting + size, SWNorthing + size);

            // Compute approximate bounding sector and center point
            if (this.sw != null && this.se != null && this.nw != null && this.ne != null)
            {
                this.boundingSector = Sector.boundingSector(sw, ne).union(Sector.boundingSector(nw, se));
                this.squareCenter = this.boundingSector.getCentroid();
                this.boundingSector = this.gridZone.sector.intersection(this.boundingSector);
            }
            if (this.boundingSector != null)
                this.centroid = this.boundingSector.getCentroid();

            // Check whether this square is truncated by the grid zone boundary
            this.isTruncated = !isInsideGridZone();
        }

        public Position computePositionFromUTM(int zone, char hemisphere, double easting, double northing)
        {
            try
            {
                UTMCoord UTM = UTMCoord.fromUTM(zone, hemisphere, easting, northing, globe);
                return new Position(Angle.fromRadiansLatitude(UTM.getLatitude().radians),
                        Angle.fromRadiansLongitude(UTM.getLongitude().radians), 10e3);
            }
            catch (IllegalArgumentException e)
            {
                return null;
            }
        }

        public Extent getExtent(Globe globe, double ve)
        {
            if (this.extent == null || ve != this.extentVerticalExaggeration)
            {
                this.extent = globe.computeBoundingCylinder(ve, this.boundingSector);
                this.extentVerticalExaggeration = ve;
            }
            return this.extent;
        }

        public boolean isInView(DrawContext dc)
        {
            if (!viewFrustum.intersects(this.getExtent(dc.getGlobe(), dc.getVerticalExaggeration())))
                return false;

            // Check apparent size
            if (getSizeInPixels(dc) <= MIN_CELL_SIZE_PIXELS)
                return false;

            return true;
        }

        /**
         * Determines whether this square is fully inside its parent grid zone.
         * @return true if this square is totaly inside its parent grid zone.
         */
        public boolean isInsideGridZone()
        {
            if (!this.gridZone.isPositionInside(this.nw))
                return false;
            if (!this.gridZone.isPositionInside(this.ne))
                return false;
            if (!this.gridZone.isPositionInside(this.sw))
                return false;
            if (!this.gridZone.isPositionInside(this.se))
                return false;
            return true;
        }

        /**
         * Determines whether this square is fully outside its parent grid zone.
         * @return true is this square is totaly outside its parent grid zone.
         */
        public boolean isOutsideGridZone()
        {
            if (this.gridZone.isPositionInside(this.nw))
                return false;
            if (this.gridZone.isPositionInside(this.ne))
                return false;
            if (this.gridZone.isPositionInside(this.sw))
                return false;
            if (this.gridZone.isPositionInside(this.se))
                return false;
            return true;
        }

        public double getSizeInPixels(DrawContext dc)
        {
            View view = dc.getView();
            if(this.centerPoint == null || frameCount % 10 == 0)
                this.centerPoint = getSurfacePoint(dc, this.centroid.getLatitude(), this.centroid.getLongitude());
            Double distance = view.getEyePoint().distanceTo3(this.centerPoint);
            return this.size / view.computePixelSizeAtDistance(distance);
        }

        public boolean isUTMPositionInsideZone(int zone, char hemisphere, double easting, double northing)
        {
            return gridZone.isPositionInside(computePositionFromUTM(zone, hemisphere, easting, northing));
        }

        public void computeTruncatedSegment(Position p1, Position p2, ArrayList<Position> positions)
        {
            boolean p1In = this.gridZone.isPositionInside(p1);
            boolean p2In = this.gridZone.isPositionInside(p2);
            if (!p1In && !p2In)
                // whole segment is (likely) outside
                return;
            if (p1In && p2In)
            {
                // whole segment is (likely) inside
                positions.add(p1);
                positions.add(p2);
            }
            else
            {
                // segment does cross the boundary
                Position outPoint = !p1In ? p1 : p2;
                Position inPoint = p1In ? p1 : p2;
                for (int i = 1; i <= 2; i++)  // there may be two intersections
                {
                    LatLon intersection = null;
                    if (outPoint.getLongitude().degrees > this.gridZone.sector.getMaxLongitude().degrees
                            || (this.gridZone.sector.getMaxLongitude().degrees == 180 && outPoint.getLongitude().degrees < 0))
                    {
                        // intersect with east meridian
                        intersection = this.gridZone.greatCircleIntersectionAtLongitude(
                                inPoint.getLatLon(), outPoint.getLatLon(), this.gridZone.sector.getMaxLongitude());
                    }
                    else if (outPoint.getLongitude().degrees < this.gridZone.sector.getMinLongitude().degrees
                            || (this.gridZone.sector.getMinLongitude().degrees == -180 && outPoint.getLongitude().degrees > 0))
                    {
                        // intersect with west meridian
                        intersection = this.gridZone.greatCircleIntersectionAtLongitude(
                                inPoint.getLatLon(), outPoint.getLatLon(), this.gridZone.sector.getMinLongitude());
                    }
                    else if (outPoint.getLatitude().degrees > this.gridZone.sector.getMaxLatitude().degrees)
                    {
                        // intersect with top parallel
                        intersection = this.gridZone.greatCircleIntersectionAtLatitude(
                                inPoint.getLatLon(), outPoint.getLatLon(), this.gridZone.sector.getMaxLatitude());
                    }
                    else if (outPoint.getLatitude().degrees < this.gridZone.sector.getMinLatitude().degrees)
                    {
                        // intersect with bottom parallel
                        intersection = this.gridZone.greatCircleIntersectionAtLatitude(
                                inPoint.getLatLon(), outPoint.getLatLon(), this.gridZone.sector.getMinLatitude());
                    }
                    if (intersection != null)
                        outPoint = new Position(intersection, outPoint.getElevation());
                    else
                        break;
                }
                positions.add(inPoint);
                positions.add(outPoint);
            }
        }
    }

    /**
     * Represent a 100km square zone inside a <code>GridZone</code>.
     */
    private class SquareZone extends SquareSector
    {
        protected String name;
        protected SquareGrid squareGrid;
        protected ArrayList<GridElement> gridElements;

        private SquareZone northNeighbor, eastNeighbor;

        public SquareZone(GridZone parent, double SWEasting, double SWNorthing, double size)
        {
            super(parent, SWEasting, SWNorthing, size);

            // Find out name
            double tenMeterRadian = 10d / 6378137d;
            try
            {
                MGRSCoord MGRS = null;
                if (this.gridZone.isPositionInside(this.sw))
                    MGRS = MGRSCoord.fromLatLon(
                            Angle.fromRadiansLatitude(this.sw.getLatitude().radians + tenMeterRadian),
                            Angle.fromRadiansLongitude(this.sw.getLongitude().radians + tenMeterRadian), globe);
                else if (this.gridZone.isPositionInside(this.se))
                    MGRS = MGRSCoord.fromLatLon(
                            Angle.fromRadiansLatitude(this.se.getLatitude().radians + tenMeterRadian),
                            Angle.fromRadiansLongitude(this.se.getLongitude().radians - tenMeterRadian), globe);
                else if (this.gridZone.isPositionInside(this.nw))
                    MGRS = MGRSCoord.fromLatLon(
                            Angle.fromRadiansLatitude(this.nw.getLatitude().radians - tenMeterRadian),
                            Angle.fromRadiansLongitude(this.nw.getLongitude().radians + tenMeterRadian), globe);
                else if (this.gridZone.isPositionInside(this.ne))
                    MGRS = MGRSCoord.fromLatLon(
                            Angle.fromRadiansLatitude(this.ne.getLatitude().radians - tenMeterRadian),
                            Angle.fromRadiansLongitude(this.ne.getLongitude().radians - tenMeterRadian), globe);
                if (MGRS != null)
                    this.name = MGRS.toString().substring(3, 5);
            }
            catch (IllegalArgumentException e)
            {
                this.name = "";
            }
        }

        public void setNorthNeighbor(SquareZone sz)
        {
            this.northNeighbor = sz;
        }

        public void setEastNeighbor(SquareZone sz)
        {
            this.eastNeighbor = sz;
        }

        public void selectRenderables(DrawContext dc, Sector vs, MGRSGraticuleLayer layer)
        {
            // Select our renderables
            if (this.gridElements == null)
                createRenderables(dc);

            boolean drawMetricLabels = getSizeInPixels(dc) > MIN_CELL_SIZE_PIXELS * 4 * 1.7;
            for (GridElement ge : this.gridElements)
            {
                if (ge.isInView(dc, vs))
                {
                    if (ge.type.equals(GridElement.TYPE_LINE_NORTH) && this.isNorthNeighborInView(dc))
                        continue;
                    if (ge.type.equals(GridElement.TYPE_LINE_EAST) && this.isEastNeighborInView(dc))
                        continue;

                    if (drawMetricLabels)
                        metricScaleSupport.computeMetricScaleExtremes(this.gridZone, ge, this.size);
                    layer.addRenderable(ge.renderable, GRATICULE_100000M);
                    renderablesCount++;
                }
            }

            if (getSizeInPixels(dc) <= MIN_CELL_SIZE_PIXELS * 2)
                return;

            // Select grid renderables
            if (this.squareGrid == null)
                this.squareGrid = new SquareGrid(this.gridZone, this.SWEasting, this.SWNorthing, this.size);
            if (this.squareGrid.isInView(dc))
            {
                this.squareGrid.selectRenderables(dc, vs, layer);
                visibleCellsCount++;
            }
            else
                this.squareGrid.clearRenderables();
        }

        private boolean isNorthNeighborInView(DrawContext dc)
        {
            return this.northNeighbor != null ? this.northNeighbor.isInView(dc) : false;
        }

        private boolean isEastNeighborInView(DrawContext dc)
        {
            return this.eastNeighbor != null ? this.eastNeighbor.isInView(dc) : false;
        }

        public void clearRenderables()
        {
            if (this.gridElements != null)
            {
                this.gridElements.clear();
                this.gridElements = null;
            }
            if (this.squareGrid != null)
            {
                this.squareGrid.clearRenderables();
                this.squareGrid = null;
            }
        }

        public void createRenderables(DrawContext dc)
        {
            this.gridElements = new ArrayList<GridElement>();

            ArrayList<Position> positions = new ArrayList<Position>();
            Position p1, p2;
            Polyline polyline;
            Sector lineSector;
            Globe globe = dc.getGlobe();

            // left segment
            positions.clear();
            if (this.isTruncated)
            {
                this.computeTruncatedSegment(sw, nw, positions);
            }
            else
            {
                positions.add(sw);
                positions.add(nw);
            }
            if (positions.size() > 0)
            {
                p1 = positions.get(0);
                p2 = positions.get(1);
                polyline = new Polyline(positions);
                polyline.setPathType(Polyline.GREAT_CIRCLE);
                polyline.setFollowTerrain(true);
                lineSector = Sector.boundingSector(p1, p2);
                GridElement ge = new GridElement(lineSector, polyline, GridElement.TYPE_LINE_WEST);
                ge.setValue(this.SWEasting);
                this.gridElements.add(ge);
            }

            // right segment
            positions.clear();
            if (this.isTruncated)
            {
                this.computeTruncatedSegment(se, ne, positions);
            }
            else
            {
                positions.add(se);
                positions.add(ne);
            }
            if (positions.size() > 0)
            {
                p1 = positions.get(0);
                p2 = positions.get(1);
                polyline = new Polyline(positions);
                polyline.setPathType(Polyline.GREAT_CIRCLE);
                polyline.setFollowTerrain(true);
                lineSector = Sector.boundingSector(p1, p2);
                GridElement ge = new GridElement(lineSector, polyline, GridElement.TYPE_LINE_EAST);
                ge.setValue(this.SWEasting + this.size);
                this.gridElements.add(ge);
            }

            // bottom segment
            positions.clear();
            if (this.isTruncated)
            {
                this.computeTruncatedSegment(sw, se, positions);
            }
            else
            {
                positions.add(sw);
                positions.add(se);
            }
            if (positions.size() > 0)
            {
                p1 = positions.get(0);
                p2 = positions.get(1);
                polyline = new Polyline(positions);
                polyline.setPathType(Polyline.GREAT_CIRCLE);
                polyline.setFollowTerrain(true);
                lineSector = Sector.boundingSector(p1, p2);
                GridElement ge = new GridElement(lineSector, polyline, GridElement.TYPE_LINE_SOUTH);
                ge.setValue(this.SWNorthing);
                this.gridElements.add(ge);
            }

            // top segment
            positions.clear();
            if (this.isTruncated)
            {
                this.computeTruncatedSegment(nw, ne, positions);
            }
            else
            {
                positions.add(nw);
                positions.add(ne);
            }
            if (positions.size() > 0)
            {
                p1 = positions.get(0);
                p2 = positions.get(1);
                polyline = new Polyline(positions);
                polyline.setPathType(Polyline.GREAT_CIRCLE);
                polyline.setFollowTerrain(true);
                lineSector = Sector.boundingSector(p1, p2);
                GridElement ge = new GridElement(lineSector, polyline, GridElement.TYPE_LINE_NORTH);
                ge.setValue(this.SWNorthing + this.size);
                this.gridElements.add(ge);
            }

            // Label
            if (this.gridZone.isPositionInside(new Position(this.squareCenter, 0)))
            {
                GeographicText text = new UserFacingText(this.name, new Position(this.squareCenter, 0));
                this.gridElements.add(new GridElement(this.boundingSector, text, GridElement.TYPE_GRIDZONE_LABEL));
            }
            else if (this.squareCenter.getLatitude().degrees <= this.gridZone.sector.getMaxLatitude().degrees
                    && this.squareCenter.getLatitude().degrees >= this.gridZone.sector.getMinLatitude().degrees
                    && this.boundingSector.getDeltaLon().degrees * Math.cos(this.centroid.getLatitude().radians) > .3)
            {
                GeographicText text = new UserFacingText(this.name, new Position(this.centroid, 0));
                this.gridElements.add(new GridElement(this.boundingSector, text, GridElement.TYPE_GRIDZONE_LABEL));
            }
        }
    }

    /**
     * Represent a square 10x10 grid and recursive tree in TM coordinates
     */
    private class SquareGrid extends SquareSector
    {
        private ArrayList<GridElement> gridElements;
        private ArrayList<SquareGrid> subGrids;

        public SquareGrid(GridZone parent, double SWEasting, double SWNorthing, double size)
        {
            super(parent, SWEasting, SWNorthing, size);
        }


        public boolean isInView(DrawContext dc)
        {
            if (!viewFrustum.intersects(this.getExtent(dc.getGlobe(), dc.getVerticalExaggeration())))
                return false;

            // Check apparent size
            if (getSizeInPixels(dc) <= MIN_CELL_SIZE_PIXELS * 4)
                return false;

            return true;
        }

        public void selectRenderables(DrawContext dc, Sector vs, MGRSGraticuleLayer layer)
        {
            // Select our renderables
            if (this.gridElements == null)
                createRenderables(dc);

            int gridStep = (int) this.size / 10;
            boolean drawMetricLabels = getSizeInPixels(dc) > MIN_CELL_SIZE_PIXELS * 4 * 1.7;
            String graticuleType = getTypeFor(gridStep);

            for (GridElement ge : this.gridElements)
            {
                if (ge.isInView(dc, vs))
                {
                    if (drawMetricLabels)
                        metricScaleSupport.computeMetricScaleExtremes(this.gridZone, ge, this.size);

                    layer.addRenderable(ge.renderable, graticuleType);
                    renderablesCount++;
                }
            }

            if (getSizeInPixels(dc) <= MIN_CELL_SIZE_PIXELS * 4 * 2)
                return;

            // Select sub grids renderables
            if (this.subGrids == null)
                createSubGrids();
            for (SquareGrid sg : this.subGrids)
            {
                if (sg.isInView(dc))
                {
                    sg.selectRenderables(dc, vs, layer);
                    visibleCellsCount++;
                }
                else
                    sg.clearRenderables();
            }
        }

        public void clearRenderables()
        {
            if (this.gridElements != null)
            {
                this.gridElements.clear();
                this.gridElements = null;
            }
            if (this.subGrids != null)
            {
                for (SquareGrid sg : this.subGrids)
                    sg.clearRenderables();
                this.subGrids.clear();
                this.subGrids = null;
            }
        }

        public void createSubGrids()
        {
            this.subGrids = new ArrayList<SquareGrid>();
            // TODO: round value to int to avoid precision drift from double operations
            double gridStep = this.size / 10;
            for (int i = 0; i < 10; i++)
            {
                double easting = this.SWEasting + gridStep * i;
                for (int j = 0; j < 10; j++)
                {
                    double northing = this.SWNorthing + gridStep * j;
                    SquareGrid sg = new SquareGrid(this.gridZone, easting, northing, gridStep);
                    if (!sg.isOutsideGridZone())
                        this.subGrids.add(sg);
                }
            }
        }

        public void createRenderables(DrawContext dc)
        {
            this.gridElements = new ArrayList<GridElement>();
            double gridStep = this.size / 10;
            Position p1, p2;
            Globe globe = dc.getGlobe();
            ArrayList<Position> positions = new ArrayList<Position>();

            // South-North lines
            for (int i = 1; i <= 9; i++)
            {
                double easting = this.SWEasting + gridStep * i;
                positions.clear();
                p1 = this.computePositionFromUTM(this.gridZone.UTMZone,  this.gridZone.hemisphere,  easting, SWNorthing);
                p2 = this.computePositionFromUTM(this.gridZone.UTMZone,  this.gridZone.hemisphere,  easting, SWNorthing + this.size);
                if (this.isTruncated)
                {
                    this.computeTruncatedSegment(p1, p2, positions);
                }
                else
                {
                    positions.add(p1);
                    positions.add(p2);
                }
                if (positions.size() > 0)
                {
                    p1 = positions.get(0);
                    p2 = positions.get(1);
                    Polyline polyline = new Polyline(positions);
                    polyline.setPathType(Polyline.GREAT_CIRCLE);
                    polyline.setFollowTerrain(true);
                    Sector lineSector = Sector.boundingSector(p1, p2);
                    GridElement ge = new GridElement(lineSector, polyline, GridElement.TYPE_LINE_EASTING);
                    ge.setValue(easting);
                    this.gridElements.add(ge);
                }
            }
            // West-East lines
            for (int i = 1; i <= 9; i++)
            {
                double northing = this.SWNorthing + gridStep * i;
                positions.clear();
                p1 = this.computePositionFromUTM(this.gridZone.UTMZone,  this.gridZone.hemisphere,  SWEasting, northing);
                p2 = this.computePositionFromUTM(this.gridZone.UTMZone,  this.gridZone.hemisphere,  SWEasting + this.size, northing);
                if (this.isTruncated)
                {
                    this.computeTruncatedSegment(p1, p2, positions);
                }
                else
                {
                    positions.add(p1);
                    positions.add(p2);
                }
                if (positions.size() > 0)
                {
                    p1 = positions.get(0);
                    p2 = positions.get(1);
                    Polyline polyline = new Polyline(positions);
                    polyline.setPathType(Polyline.GREAT_CIRCLE);
                    polyline.setFollowTerrain(true);
                    Sector lineSector = Sector.boundingSector(p1, p2);
                    GridElement ge = new GridElement(lineSector, polyline, GridElement.TYPE_LINE_NORTHING);
                    ge.setValue(northing);
                    this.gridElements.add(ge);
                }
            }
        }
    }
}
