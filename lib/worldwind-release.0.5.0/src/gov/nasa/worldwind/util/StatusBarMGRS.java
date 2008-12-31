/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.MGRSCoord;

/**
 * @author Patrick Murris
 * @version $Id: StatusBarMGRS.java 5175 2008-04-25 21:12:21Z patrickmurris $
 */
public class StatusBarMGRS extends StatusBar
{
    public void moved(PositionEvent event)
    {
        this.handleCursorPositionChange(event);
    }

    private void handleCursorPositionChange(PositionEvent event)
    {
        Position newPos = event.getPosition();
        if (newPos != null)
        {
            String las = String.format("%7.4f\u00B0 %7.4f\u00B0", newPos.getLatitude().getDegrees(), newPos.getLongitude().getDegrees());
            String els = makeCursorElevationDescription(
                getEventSource().getModel().getGlobe().getElevation(newPos.getLatitude(), newPos.getLongitude()));
            String los = "";
            try
            {
                MGRSCoord MGRS = MGRSCoord.fromLatLon(newPos.getLatitude(), newPos.getLongitude(),
                        getEventSource().getModel().getGlobe());
                los = MGRS.toString();
            }
            catch (Exception e)
            {
                los = "";
            }
            latDisplay.setText(las);
            lonDisplay.setText(los);
            eleDisplay.setText(els);
        }
        else
        {
            latDisplay.setText("");
            lonDisplay.setText("Off globe");
            eleDisplay.setText("");
        }
    }

}
