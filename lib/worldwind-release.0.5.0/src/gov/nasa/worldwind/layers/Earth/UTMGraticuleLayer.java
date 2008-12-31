/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GeographicText;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.UserFacingText;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.view.OrbitView;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * Displays the UTM graticule.
 * @author Patrick Murris
 * @version $Id: UTMGraticuleLayer.java 4710 2008-03-16 22:02:20Z patrickmurris $
 */
public class UTMGraticuleLayer extends AbstractLayer
{
    public static final String GRATICULE_UTM = "Graticule.UTM";
    /**
     * Solid line rendering style. This style specifies that a line will be drawn without any breaks.
     * <br>
     * <pre><code>_________</code></pre>
     * <br>
     * is an example of a solid line.
     */
    public static final String LINE_STYLE_SOLID = GraticuleRenderingParams.VALUE_LINE_STYLE_SOLID;
    /**
     * Dashed line rendering style. This style specifies that a line will be drawn as a series of
     * long strokes, with space in between.
     * <br>
     * <pre><code>- - - - -</code></pre>
     * <br>
     * is an example of a dashed line.
     */
    public static final String LINE_STYLE_DASHED = GraticuleRenderingParams.VALUE_LINE_STYLE_DASHED;
    /**
     * Dotted line rendering style. This style specifies that a line will be drawn as a series of
     * evenly spaced "square" dots.
     * <br>
     * <pre><code>. . . . .</code></pre>
     * is an example of a dotted line.
     */
    public static final String LINE_STYLE_DOTTED = GraticuleRenderingParams.VALUE_LINE_STYLE_DOTTED;

    // Exceptions for some meridians. Values: longitude, min latitude, max latitude
    private static final int[][] specialMeridians = {{3, 56, 64}, {6, 64, 72}, {9, 72, 84}, {21, 72, 84}, {33, 72, 84}};
    // Latitude bands letters - from south to north
    private static final String latBands = "CDEFGHJKLMNPQRSTUVWX";

    private ArrayList<GridElement> gridElements;
    private GraticuleSupport graticuleSupport = new GraticuleSupport();

    public UTMGraticuleLayer()
    {
        createUTMRenderables();
        initRenderingParams();
        this.setPickEnabled(false);
        this.setName(Logging.getMessage("layers.Earth.UTMGraticule.Name"));
    }

    /**
     * Returns whether or not graticule lines will be rendered.
     *
     * @return true if graticule lines will be rendered; false otherwise.
     */
    public boolean isDrawGraticule()
    {
        return getUTMRenderingParams().isDrawLines();
    }

    /**
     * Sets whether or not graticule lines will be rendered.
     *
     * @param drawGraticule true to render graticule lines; false to disable rendering.
     */
    public void setDrawGraticule(boolean drawGraticule)
    {
        getUTMRenderingParams().setDrawLines(drawGraticule);
    }

    /**
     * Returns the graticule line Color.
     *
     * @return Color used to render graticule lines.
     */
    public Color getGraticuleLineColor()
    {
        return getUTMRenderingParams().getLineColor();
    }

    /**
     * Sets the graticule line Color.
     *
     * @param color Color that will be used to render graticule lines.
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

        getUTMRenderingParams().setLineColor(color);
    }

    /**
     * Returns the graticule line width.
     *
     * @return width of the graticule lines.
     */
    public double getGraticuleLineWidth()
    {
        return getUTMRenderingParams().getLineWidth();
    }

    /**
     * Sets the graticule line width.
     *
     * @param lineWidth width of the graticule lines.
     */
    public void setGraticuleLineWidth(double lineWidth)
    {
        getUTMRenderingParams().setLineWidth(lineWidth);
    }

    /**
     * Returns the graticule line rendering style.
     *
     * @return rendering style of the graticule lines.
     */
    public String getGraticuleLineStyle()
    {
        return getUTMRenderingParams().getLineStyle();
    }

    /**
     * Sets the graticule line rendering style.
     *
     * @param lineStyle rendering style of the graticule lines.
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

        getUTMRenderingParams().setLineStyle(lineStyle);
    }

    /**
     * Returns whether or not graticule labels will be rendered.
     *
     * @return true if graticule labels will be rendered; false otherwise.
     */
    public boolean isDrawLabels()
    {
        return getUTMRenderingParams().isDrawLabels();
    }

    /**
     * Sets whether or not graticule labels will be rendered.
     *
     * @param drawLabels true to render graticule labels; false to disable rendering.
     */
    public void setDrawLabels(boolean drawLabels)
    {
        getUTMRenderingParams().setDrawLabels(drawLabels);
    }

    /**
     * Returns the graticule label Color.
     *
     * @return Color used to render graticule labels.
     */
    public Color getLabelColor()
    {
        return getUTMRenderingParams().getLabelColor();
    }

    /**
     * Sets the graticule label Color.
     *
     * @param color Color that will be used to render graticule labels.
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

        getUTMRenderingParams().setLabelColor(color);
    }

    /**
     * Returns the Font used for graticule labels.
     *
     * @return Font used to render graticule labels.
     */
    public Font getLabelFont()
    {
        return getUTMRenderingParams().getLabelFont();
    }

    /**
     * Sets the Font used for graticule labels.
     *
     * @param font Font that will be used to render graticule labels.
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

        getUTMRenderingParams().setLabelFont(font);
    }

    public String getRestorableState()
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (rs == null)
            return null;

        RestorableSupport.StateObject so = rs.addStateObject("renderingParams");
        for (Map.Entry<String, GraticuleRenderingParams> entry : this.graticuleSupport.getAllRenderingParams())
        {
            if (entry.getKey() != null && entry.getValue() != null)
            {
                RestorableSupport.StateObject eso = rs.addStateObject(so, entry.getKey());
                makeRestorableState(entry.getValue(), rs, eso);
            }
        }

        return rs.getStateAsXml();
    }

    private static void makeRestorableState(AVList params, RestorableSupport rs, RestorableSupport.StateObject context)
    {
        if (params != null && rs != null)
        {
            for (Map.Entry<String, Object> p : params.getEntries())
            {
                if (p.getValue() instanceof Color)
                {
                    rs.addStateValueAsInteger(context, p.getKey() + ".Red", ((Color) p.getValue()).getRed());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Green", ((Color) p.getValue()).getGreen());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Blue", ((Color) p.getValue()).getBlue());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Alpha", ((Color) p.getValue()).getAlpha());
                }
                else if (p.getValue() instanceof Font)
                {
                    rs.addStateValueAsString(context, p.getKey() + ".Name", ((Font) p.getValue()).getName());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Style", ((Font) p.getValue()).getStyle());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Size", ((Font) p.getValue()).getSize());
                }
                else
                {
                    rs.addStateValueAsString(context, p.getKey(), p.getValue().toString());
                }
            }
        }
    }

    public void restoreState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        RestorableSupport.StateObject so = rs.getStateObject("renderingParams");
        if (so != null)
        {
            RestorableSupport.StateObject[] renderParams = rs.getAllStateObjects(so);
            for (RestorableSupport.StateObject rp : renderParams)
            {
                if (rp != null)
                {
                    GraticuleRenderingParams params = getRenderingParams(rp.getName());
                    if (params == null)
                        params = new GraticuleRenderingParams();
                    restorableStateToParams(params, rs, rp);
                    setRenderingParams(rp.getName(), params);
                }
            }
        }
    }

    private static void restorableStateToParams(AVList params, RestorableSupport rs, RestorableSupport.StateObject context)
    {
        if (params != null && rs != null)
        {
            Boolean b = rs.getStateValueAsBoolean(context, GraticuleRenderingParams.KEY_DRAW_LINES);
            if (b != null)
                params.setValue(GraticuleRenderingParams.KEY_DRAW_LINES, b);

            Integer red = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LINE_COLOR + ".Red");
            Integer green = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LINE_COLOR + ".Green");
            Integer blue = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LINE_COLOR + ".Blue");
            Integer alpha = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LINE_COLOR + ".Alpha");
            if (red != null && green != null && blue != null && alpha != null)
                params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, new Color(red, green, blue, alpha));

            Double d = rs.getStateValueAsDouble(context, GraticuleRenderingParams.KEY_LINE_WIDTH);
            if (d != null)
                params.setValue(GraticuleRenderingParams.KEY_LINE_WIDTH, d);

            String s = rs.getStateValueAsString(context, GraticuleRenderingParams.KEY_LINE_STYLE);
            if (s != null)
                params.setValue(GraticuleRenderingParams.KEY_LINE_STYLE, s);

            b = rs.getStateValueAsBoolean(context, GraticuleRenderingParams.KEY_DRAW_LABELS);
            if (b != null)
                params.setValue(GraticuleRenderingParams.KEY_DRAW_LABELS, b);

            red = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_COLOR + ".Red");
            green = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_COLOR + ".Green");
            blue = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_COLOR + ".Blue");
            alpha = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_COLOR + ".Alpha");
            if (red != null && green != null && blue != null && alpha != null)
                params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, new Color(red, green, blue, alpha));

            String name = rs.getStateValueAsString(context, GraticuleRenderingParams.KEY_LABEL_FONT + ".Name");
            Integer style = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_FONT + ".Style");
            Integer size = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_FONT + ".Size");
            if (name != null && style != null && size != null)
                params.setValue(GraticuleRenderingParams.KEY_LABEL_FONT, new Font(name, style, size));
        }
    }

    // --- Graticule Rendering --------------------------------------------------------------

    private void initRenderingParams()
    {
        GraticuleRenderingParams params = new GraticuleRenderingParams();
        params.setValue(GraticuleRenderingParams.KEY_LINE_COLOR, new Color(.8f, .8f, .8f, .5f));
        params.setValue(GraticuleRenderingParams.KEY_LABEL_COLOR, new Color(1f, 1f, 1f, .8f));
        params.setValue(GraticuleRenderingParams.KEY_LABEL_FONT, Font.decode("Arial-Bold-14"));
        params.setValue(GraticuleRenderingParams.KEY_DRAW_LABELS, Boolean.TRUE);
        setRenderingParams(GRATICULE_UTM, params);
    }

    private GraticuleRenderingParams getUTMRenderingParams()
    {
        return this.graticuleSupport.getRenderingParams(GRATICULE_UTM);
    }

    protected GraticuleRenderingParams getRenderingParams(String key)
    {
        if (key == null)
        {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.graticuleSupport.getRenderingParams(key);
    }

    protected void setRenderingParams(String key, GraticuleRenderingParams renderingParams)
    {
        if (key == null)
        {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.graticuleSupport.setRenderingParams(key, renderingParams);
    }

    protected void addRenderable(Object renderable, String paramsKey)
    {
        if (renderable == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.graticuleSupport.addRenderable(renderable, paramsKey);
    }

    protected void removeAllRenderables()
    {
        this.graticuleSupport.removeAllRenderables();
    }

    public void doRender(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        removeAllRenderables();
        selectUTMRenderables(dc);
        renderGraticule(dc);
    }

    protected void renderGraticule(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.graticuleSupport.render(dc);
    }

    /**
     * Select the visible grid elements
     * @param dc the current <code>DrawContext</code>.
     */
    protected void selectUTMRenderables(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        Sector vs = dc.getVisibleSector();
        OrbitView view = (OrbitView)dc.getView();
        // Compute labels offset from view center
        Position centerPos = view.getCenterPosition();
        Double pixelSizeDegrees = Angle.fromRadians(view.computePixelSizeAtDistance(view.getZoom())
                / dc.getGlobe().getEquatorialRadius()).degrees;
        Double labelOffsetDegrees = pixelSizeDegrees * view.getViewport().getWidth() / 4;
        Position labelPos = Position.fromDegrees(centerPos.getLatitude().degrees - labelOffsetDegrees,
                centerPos.getLongitude().degrees - labelOffsetDegrees, 0);
        Double labelLatDegrees = labelPos.getLatitude().normalizedLatitude().degrees;
        labelLatDegrees = Math.min(Math.max(labelLatDegrees, -76), 78);
        labelPos = new Position(Angle.fromDegrees(labelLatDegrees), labelPos.getLongitude().normalizedLongitude(), 0);

        int count = 0;
        if (vs != null)
        {
            for (GridElement ge : this.gridElements)
            {
                if (ge.isInView(dc))
                {
                    if (ge.renderable instanceof GeographicText)
                    {
                        GeographicText gt = (GeographicText) ge.renderable;
                        if (labelPos.getLatitude().degrees < 72 || "*32*34*36*".indexOf("*" + gt.getText() + "*") == -1)
                        {
                            // Adjust label position according to eye position
                            Position pos = gt.getPosition();
                            if (ge.type.equals(GridElement.TYPE_LATITUDE_LABEL))
                                pos = Position.fromDegrees(pos.getLatitude().degrees,
                                        labelPos.getLongitude().degrees, pos.getElevation());
                            else if (ge.type.equals(GridElement.TYPE_LONGITUDE_LABEL))
                                pos = Position.fromDegrees(labelPos.getLatitude().degrees,
                                        pos.getLongitude().degrees, pos.getElevation());

                            gt.setPosition(pos);
                        }
                    }

                    this.graticuleSupport.addRenderable(ge.renderable, GRATICULE_UTM);
                    count++;
                }
            }
            //System.out.println("Total elements: " + count + " visible sector: " + vs);
        }
    }

    /**
     * Create the graticule grid elements
     */
    private void createUTMRenderables()
    {
        this.gridElements = new ArrayList<GridElement>();

        ArrayList<Position> positions = new ArrayList<Position>();

        // Generate meridians and zone labels
        int lon = -180;
        int zoneNumber = 1;
        int maxLat = 84;
        for (int i = 0; i < 60; i++)
        {
            Angle longitude = Angle.fromDegrees(lon);
            // Meridian
            positions.clear();
            positions.add(new Position(Angle.fromDegrees(-80), longitude, 10e3));
            positions.add(new Position(Angle.fromDegrees(-60), longitude, 10e3));
            positions.add(new Position(Angle.fromDegrees(-30), longitude, 10e3));
            positions.add(new Position(Angle.ZERO, longitude, 10e3));
            positions.add(new Position(Angle.fromDegrees(30), longitude, 10e3));
            if (lon < 6 || lon > 36)
            {
                // 'regular' UTM meridians
                maxLat = 84;
                positions.add(new Position(Angle.fromDegrees(60), longitude, 10e3));
                positions.add(new Position(Angle.fromDegrees(maxLat), longitude, 10e3));
            }
            else
            {
                // Exceptions: shorter meridians around and north-east of Norway
                if (lon == 6)
                {
                    maxLat = 56;
                    positions.add(new Position(Angle.fromDegrees(maxLat), longitude, 10e3));
                }
                else
                {
                    maxLat = 72;
                    positions.add(new Position(Angle.fromDegrees(60), longitude, 10e3));
                    positions.add(new Position(Angle.fromDegrees(maxLat), longitude, 10e3));
                }
            }
            Polyline polyline = new Polyline(positions);
            polyline.setPathType(Polyline.GREAT_CIRCLE);
            polyline.setFollowTerrain(true);
            polyline.setTerrainConformance(50);
            Sector sector = Sector.fromDegrees(-80, maxLat, lon, lon);
            this.gridElements.add(new GridElement(sector, polyline, GridElement.TYPE_LINE));

            // Zone label
            GeographicText text = new UserFacingText(zoneNumber + "",
                    Position.fromDegrees(0, lon + 3, 0));
            sector = Sector.fromDegrees(-90, 90, lon + 3, lon + 3);
            this.gridElements.add(new GridElement(sector, text, GridElement.TYPE_LONGITUDE_LABEL));

            // Increase longitude and zone number
            lon += 6;
            zoneNumber++;
        }

        // Generate special meridian segments for exceptions around and north-east of Norway
        for (int i = 0; i < 5; i++)
        {
            positions.clear();
            lon = this.specialMeridians[i][0];
            positions.add(new Position(Angle.fromDegrees(this.specialMeridians[i][1]), Angle.fromDegrees(lon), 10e3));
            positions.add(new Position(Angle.fromDegrees(this.specialMeridians[i][2]), Angle.fromDegrees(lon), 10e3));
            Polyline polyline = new Polyline(positions);
            polyline.setPathType(Polyline.GREAT_CIRCLE);
            polyline.setFollowTerrain(true);
            polyline.setTerrainConformance(50);
            Sector sector = Sector.fromDegrees(this.specialMeridians[i][1], this.specialMeridians[i][2], lon, lon);
            this.gridElements.add(new GridElement(sector, polyline, GridElement.TYPE_LINE));
        }

        // Generate parallels - no exceptions
        int lat = -80;
        for (int i = 0; i < 21; i++)
        {
            Angle latitude = Angle.fromDegrees(lat);
            positions.clear();
            positions.add(new Position(latitude, Angle.NEG180, 10e3));
            positions.add(new Position(latitude, Angle.fromDegrees(-150), 10e3));
            positions.add(new Position(latitude, Angle.fromDegrees(-120), 10e3));
            positions.add(new Position(latitude, Angle.NEG90, 10e3));
            positions.add(new Position(latitude, Angle.fromDegrees(-60), 10e3));
            positions.add(new Position(latitude, Angle.fromDegrees(-30), 10e3));
            positions.add(new Position(latitude, Angle.ZERO, 10e3));
            positions.add(new Position(latitude, Angle.fromDegrees(30), 10e3));
            positions.add(new Position(latitude, Angle.fromDegrees(60), 10e3));
            positions.add(new Position(latitude, Angle.POS90, 10e3));
            positions.add(new Position(latitude, Angle.fromDegrees(120), 10e3));
            positions.add(new Position(latitude, Angle.fromDegrees(150), 10e3));
            positions.add(new Position(latitude, Angle.POS180, 10e3));
            Polyline polyline = new Polyline(positions);
            polyline.setPathType(Polyline.LINEAR);
            polyline.setFollowTerrain(true);
            polyline.setTerrainConformance(20);
            Sector sector = Sector.fromDegrees(lat, lat, -180, 180);
            this.gridElements.add(new GridElement(sector, polyline, GridElement.TYPE_LINE));

            // Latitude band label
            if (i < 20)
            {
                GeographicText text = new UserFacingText(this.latBands.charAt(i) + "",
                        Position.fromDegrees(lat + 4, 0, 0));
                sector = Sector.fromDegrees(lat + 4, lat + 4, -180, 180);
                this.gridElements.add(new GridElement(sector, text, GridElement.TYPE_LATITUDE_LABEL));
            }

            // Increase latitude
            lat += lat < 72 ? 8 : 12;
        }
    }

    protected class GridElement
    {
        public final static String TYPE_LINE = "GridElement_Line";
        public final static String TYPE_LINE_NORTH = "GridElement_LineNorth";
        public final static String TYPE_LINE_SOUTH = "GridElement_LineSouth";
        public final static String TYPE_LINE_WEST = "GridElement_LineWest";
        public final static String TYPE_LINE_EAST = "GridElement_LineEast";
        public final static String TYPE_LINE_NORTHING = "GridElement_LineNorthing";
        public final static String TYPE_LINE_EASTING = "GridElement_LineEasting";
        public final static String TYPE_GRIDZONE_LABEL = "GridElement_GridZoneLabel";
        public final static String TYPE_LONGITUDE_LABEL = "GridElement_LongitudeLabel";
        public final static String TYPE_LATITUDE_LABEL = "GridElement_LatitudeLabel";

        protected final Sector sector;
        protected final Object renderable;
        protected final String type;
        protected double value;

        public GridElement(Sector sector, Object renderable, String type)
        {
            if (sector == null)
            {
                String message = Logging.getMessage("nullValue.SectorIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (renderable == null)
            {
                String message = Logging.getMessage("nullValue.ObjectIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (type == null)
            {
                String message = Logging.getMessage("nullValue.StringIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            this.sector = sector;
            this.renderable = renderable;
            this.type = type;
        }

        public void setValue(double value)
        {
            this.value = value;
        }
        
        public boolean isInView(DrawContext dc)
        {
            if (dc == null)
            {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            return isInView(dc, dc.getVisibleSector());
        }

        public boolean isInView(DrawContext dc, Sector vs)
        {
            if (dc == null)
            {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (vs == null)
            {
                String message = Logging.getMessage("nullValue.SectorIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (!this.sector.intersects(vs))
                return false;

            return true;
        }
    }
}
