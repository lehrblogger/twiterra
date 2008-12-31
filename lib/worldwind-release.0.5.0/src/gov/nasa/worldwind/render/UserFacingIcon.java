/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;

import java.awt.*;

/**
 * @author tag
 * @version $Id$
 */
public class UserFacingIcon extends AVListImpl implements WWIcon, Movable
{
    //    private final String iconPath;
    private Position iconPosition; // may be null because placement may be relative
    private Dimension iconSize; // may be null to indicate "use native image size"
    private boolean isHighlighted = false;
    private boolean isVisible = true;
    private double highlightScale = 1.2; // TODO: make configurable
    private String toolTipText;
    private Font toolTipFont;
    private boolean showToolTip = false;
    private boolean alwaysOnTop = false;
    private java.awt.Color textColor;
    private Object imageSource;
    private Object backgroundImage;
    private double backgroundScale;

    public UserFacingIcon(String iconPath, Position iconPosition)
    {
        if (iconPath == null)
        {
            String message = Logging.getMessage("nullValue.IconFilePath");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.imageSource = iconPath;
        this.iconPosition = iconPosition;
    }

    public UserFacingIcon(Object imageSource, Position iconPosition)
    {
        if (imageSource == null)
        {
            String message = Logging.getMessage("nullValue.IconFilePath");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.imageSource = imageSource;
        this.iconPosition = iconPosition;
    }

    public Object getImageSource()
    {
        return imageSource;
    }

    public void setImageSource(Object imageSource)
    {
        this.imageSource = imageSource;
    }

    public String getPath()
    {
        return this.imageSource instanceof String ? (String) this.imageSource : null;
    }

    public Position getPosition()
    {
        return this.iconPosition;
    }

    public void setPosition(Position iconPosition)
    {
        this.iconPosition = iconPosition;
    }

    public boolean isHighlighted()
    {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted)
    {
        isHighlighted = highlighted;
    }

    public double getHighlightScale()
    {
        return highlightScale;
    }

    public void setHighlightScale(double highlightScale)
    {
        this.highlightScale = highlightScale;
    }

    public Dimension getSize()
    {
        return this.iconSize;
    }

    public void setSize(Dimension size)
    {
        this.iconSize = size;
    }

    public boolean isVisible()
    {
        return isVisible;
    }

    public void setVisible(boolean visible)
    {
        isVisible = visible;
    }

    public String getToolTipText()
    {
        return toolTipText;
    }

    public void setToolTipText(String toolTipText)
    {
        this.toolTipText = toolTipText;
    }

    public Font getToolTipFont()
    {
        return toolTipFont;
    }

    public void setToolTipFont(Font toolTipFont)
    {
        this.toolTipFont = toolTipFont;
    }

    public boolean isShowToolTip()
    {
        return showToolTip;
    }

    public void setShowToolTip(boolean showToolTip)
    {
        this.showToolTip = showToolTip;
    }

    public Color getToolTipTextColor()
    {
        return textColor;
    }

    public void setToolTipTextColor(Color textColor)
    {
        this.textColor = textColor;
    }

    public boolean isAlwaysOnTop()
    {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop)
    {
        this.alwaysOnTop = alwaysOnTop;
    }

    public Object getBackgroundImage()
    {
        return backgroundImage;
    }

    public void setBackgroundImage(Object background)
    {
        this.backgroundImage = background;
    }

    public double getBackgroundScale()
    {
        return backgroundScale;
    }

    public void setBackgroundScale(double backgroundScale)
    {
        this.backgroundScale = backgroundScale;
    }

    public void move(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.iconPosition = this.iconPosition.add(position);
    }

    public void moveTo(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.iconPosition = position;
    }

    public Position getReferencePosition()
    {
        return this.iconPosition;
    }

    public String toString()
    {
        return this.imageSource != null ? this.imageSource.toString() : this.getClass().getName();
    }

    /**
     * Returns an XML state document String describing the public attributes of this UserFacingIcon.
     *
     * @return XML state document string describing this UserFacingIcon.
     */
    public String getRestorableState()
    {
        RestorableSupport restorableSupport = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (restorableSupport == null)
            return null;

        // Save the imagePath property only when the imageSource property is a simple String path. If the imageSource
        // property is a BufferedImage (or some other object), we make no effort to save that state. We save under
        // the name "imagePath" to denote that it is a special case of "imageSource".
        if (getPath() != null)
            restorableSupport.addStateValueAsString("imagePath", getPath(), true);

        // Save the iconPosition property only if all parts (latitude, longitude, and elevation) can be saved.
        // We will not save a partial iconPosition (for example, just the elevation).
        if (this.iconPosition != null
            && this.iconPosition.getLatitude() != null
            && this.iconPosition.getLongitude() != null)
        {
            RestorableSupport.StateObject positionStateObj = restorableSupport.addStateObject("position");
            if (positionStateObj != null)
            {
                restorableSupport.addStateValueAsDouble(positionStateObj, "latitude",
                        this.iconPosition.getLatitude().degrees);
                restorableSupport.addStateValueAsDouble(positionStateObj, "longitude",
                        this.iconPosition.getLongitude().degrees);
                restorableSupport.addStateValueAsDouble(positionStateObj, "elevation",
                        this.iconPosition.getElevation());
            }
        }

        if (this.iconSize != null)
        {
            RestorableSupport.StateObject sizeStateObj = restorableSupport.addStateObject("size");
            if (sizeStateObj != null)
            {
                restorableSupport.addStateValueAsDouble(sizeStateObj, "width", this.iconSize.getWidth());
                restorableSupport.addStateValueAsDouble(sizeStateObj, "height", this.iconSize.getHeight());
            }
        }

        if (this.toolTipText != null)
            restorableSupport.addStateValueAsString("toolTipText", this.toolTipText, true);

        // Save the name, style, and size of the font. These will be used to restore the font using the 
        // constructor: new Font(name, style, size).
        if (this.toolTipFont != null)
        {
            RestorableSupport.StateObject toolTipFontStateObj = restorableSupport.addStateObject("toolTipFont");
            if (toolTipFontStateObj != null)
            {
                restorableSupport.addStateValueAsString(toolTipFontStateObj, "name", this.toolTipFont.getName());
                restorableSupport.addStateValueAsInteger(toolTipFontStateObj, "style", this.toolTipFont.getStyle());
                restorableSupport.addStateValueAsInteger(toolTipFontStateObj, "size", this.toolTipFont.getSize());
            }
        }

        if (this.textColor != null)
        {
            String encodedColor = RestorableSupport.encodeColor(this.textColor);
            if (encodedColor != null)
                restorableSupport.addStateValueAsString("toolTipTextColor", encodedColor);
        }

        restorableSupport.addStateValueAsDouble("highlightScale", this.highlightScale);
        restorableSupport.addStateValueAsBoolean("highlighted", this.isHighlighted);
        restorableSupport.addStateValueAsBoolean("visible", this.isVisible);
        restorableSupport.addStateValueAsBoolean("showToolTip", this.showToolTip);
        restorableSupport.addStateValueAsBoolean("alwaysOnTop", this.alwaysOnTop);

        return restorableSupport.getStateAsXml();
    }

    /**
     * Restores publicly settable attribute values found in the specified XML state document String. The
     * document specified by <code>stateInXml</code> must be a well formed XML document String, or this will throw an
     * IllegalArgumentException. Unknown structures in <code>stateInXml</code> are benign, because they will
     * simply be ignored.
     *
     * @param stateInXml an XML document String describing a UserFacingIcon.
     * @throws IllegalArgumentException If <code>stateInXml</code> is null, or if <code>stateInXml</code> is not
     *                                  a well formed XML document String.
     */
    public void restoreState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport restorableSupport;
        try
        {
            restorableSupport = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        // The imagePath property should exist only if the imageSource property was a simple String path.
        // If the imageSource property was a BufferedImage (or some other object), it should not exist in the
        // state document. We save under the name "imagePath" to denote that it is a special case of "imageSource".
        String imagePathState = restorableSupport.getStateValueAsString("imagePath");
        if (imagePathState != null)
            setImageSource(imagePathState);

        // Restore the position property only if all parts are available.
        // We will not restore a partial position (for example, just the elevation).
        RestorableSupport.StateObject positionStateObj = restorableSupport.getStateObject("position");
        if (positionStateObj != null)
        {
            Double latitudeState = restorableSupport.getStateValueAsDouble(positionStateObj, "latitude");
            Double longitudeState = restorableSupport.getStateValueAsDouble(positionStateObj, "longitude");
            Double elevationState = restorableSupport.getStateValueAsDouble(positionStateObj, "elevation");
            if (latitudeState != null && longitudeState != null && elevationState != null)
                setPosition(Position.fromDegrees(latitudeState, longitudeState, elevationState));
        }

        // Restore the size property only if all parts are available.
        // We will not restore a partial size (for example, just the width).
        RestorableSupport.StateObject sizeStateObj = restorableSupport.getStateObject("size");
        if (sizeStateObj != null)
        {
            Double widthState = restorableSupport.getStateValueAsDouble(sizeStateObj, "width");
            Double heightState = restorableSupport.getStateValueAsDouble(sizeStateObj, "height");
            if (widthState != null && heightState != null)
                setSize(new Dimension(widthState.intValue(), heightState.intValue()));
        }

        String toolTipTextState = restorableSupport.getStateValueAsString("toolTipText");
        if (toolTipTextState != null)
            setToolTipText(toolTipTextState);

        // Restore the toolTipFont property only if all parts are available.
        // We will not restore a partial toolTipFont (for example, just the size).
        RestorableSupport.StateObject toolTipFontStateObj = restorableSupport.getStateObject("toolTipFont");
        if (toolTipFontStateObj != null)
        {
            // The "font name" of toolTipFont.
            String nameState = restorableSupport.getStateValueAsString(toolTipFontStateObj, "name");
            // The style attributes.
            Integer styleState = restorableSupport.getStateValueAsInteger(toolTipFontStateObj, "style");
            // The simple font size.
            Integer sizeState = restorableSupport.getStateValueAsInteger(toolTipFontStateObj, "size");
            if (nameState != null && styleState != null && sizeState != null)
                setToolTipFont(new Font(nameState, styleState, sizeState));
        }

        String toolTipTextColorState = restorableSupport.getStateValueAsString("toolTipTextColor");
        if (toolTipTextColorState != null)
        {
            Color color = RestorableSupport.decodeColor(toolTipTextColorState);
            if (color != null)
                setToolTipTextColor(color);
        }
        
        Double highlightScaleState = restorableSupport.getStateValueAsDouble("highlightScale");
        if (highlightScaleState != null)
            setHighlightScale(highlightScaleState);

        Boolean isHighlightedState = restorableSupport.getStateValueAsBoolean("highlighted");
        if (isHighlightedState != null)
            setHighlighted(isHighlightedState);

        Boolean isVisibleState = restorableSupport.getStateValueAsBoolean("visible");
        if (isVisibleState != null)
            setVisible(isVisibleState);

        Boolean showToolTipState = restorableSupport.getStateValueAsBoolean("showToolTip");
        if (showToolTipState != null)
            setShowToolTip(showToolTipState);

        Boolean alwaysOnTopState = restorableSupport.getStateValueAsBoolean("alwaysOnTop");
        if (alwaysOnTopState != null)
            setAlwaysOnTop(alwaysOnTopState);
    }
}
