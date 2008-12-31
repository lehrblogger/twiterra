/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom.coords;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;

/**
 * This immutable class holds a set of UTM coordinates along with it's
 * corresponding latitude and longitude.
 *
 * @author Patrick Murris
 * @version $Id: UTMCoord.java 5188 2008-04-27 02:05:53Z patrickmurris $
 */

public class UTMCoord
{
    private final Angle latitude;
    private final Angle longitude;
    private final char hemisphere;
    private final int zone;
    private final double easting;
    private final double northing;


    /**
     * Create a set of UTM coordinates from a pair of latitude and longitude
     * for a WGS84 globe.
     *
     * @param latitude the latitude <code>Angle</code>.
     * @param longitude the longitude <code>Angle</code>.
     * @return the corresponding <code>UTMCoord</code>.
     * @throws IllegalArgumentException if <code>latitude</code> or <code>longitude</code> is null,
     * or the conversion to UTM coordinates fails.
     */
    public static UTMCoord fromLatLon(Angle latitude, Angle longitude)
    {
        return fromLatLon(latitude, longitude, null);
    }

    /**
     * Create a set of UTM coordinates from a pair of latitude and longitude
     * for the given <code>Globe</code>.
     *
     * @param latitude the latitude <code>Angle</code>.
     * @param longitude the longitude <code>Angle</code>.
     * @param globe the <code>Globe</code> - can be null (will use WGS84).
     * @return the corresponding <code>UTMCoord</code>.
     * @throws IllegalArgumentException if <code>latitude</code> or <code>longitude</code> is null,
     * or the conversion to UTM coordinates fails.
     */
    public static UTMCoord fromLatLon(Angle latitude, Angle longitude, Globe globe)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final UTMCoordConverter converter = new UTMCoordConverter(globe);
        long err = converter.convertGeodeticToUTM(latitude.radians, longitude.radians);

        if (err != UTMCoordConverter.UTM_NO_ERROR)
        {
            String message = Logging.getMessage("Coord.UTMConversionError");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return new UTMCoord(latitude, longitude, converter.getZone(), converter.getHemisphere(),
                converter.getEasting(), converter.getNorthing());
    }

    /**
     * Create a set of UTM coordinates for a WGS84 globe.
     *
     * @param zone the UTM zone - 1 to 60.
     * @param hemisphere the hemisphere 'N' or 'S'.
     * @param easting the easting distance in meters
     * @param northing the northing distance in meters.
     * @return the corresponding <code>UTMCoord</code>.
     * @throws IllegalArgumentException if the conversion to UTM coordinates fails.
     */
    public static UTMCoord fromUTM(int zone, char hemisphere, double easting, double northing)
    {
        return fromUTM(zone, hemisphere, easting, northing, null);
    }

    /**
     * Create a set of UTM coordinates for the given <code>Globe</code>.
     *
     * @param zone the UTM zone - 1 to 60.
     * @param hemisphere the hemisphere 'N' or 'S'.
     * @param easting the easting distance in meters
     * @param northing the northing distance in meters.
     * @param globe the <code>Globe</code> - can be null (will use WGS84).
     * @return the corresponding <code>UTMCoord</code>.
     * @throws IllegalArgumentException if the conversion to UTM coordinates fails.
     */
    public static UTMCoord fromUTM(int zone, char hemisphere, double easting, double northing, Globe globe)
    {
        final UTMCoordConverter converter = new UTMCoordConverter(globe);
        long err = converter.convertUTMToGeodetic(zone, hemisphere, easting, northing);

        if (err != UTMCoordConverter.UTM_NO_ERROR)
        {
            String message = Logging.getMessage("Coord.UTMConversionError");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return new UTMCoord(Angle.fromRadians(converter.getLatitude()),
                Angle.fromRadians(converter.getLongitude()),
                zone, hemisphere, easting, northing);
    }

    /**
     * Create an arbitrary set of UTM coordinates with the given values.
     *
     * @param latitude the latitude <code>Angle</code>.
     * @param longitude the longitude <code>Angle</code>.
     * @param zone the UTM zone - 1 to 60.
     * @param hemisphere the hemisphere 'N' or 'S'.
     * @param easting the easting distance in meters
     * @param northing the northing distance in meters.
     * @throws IllegalArgumentException if <code>latitude</code> or <code>longitude</code> is null.
     */
    public UTMCoord(Angle latitude, Angle longitude, int zone, char hemisphere, double easting, double northing)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.latitude = latitude;
        this.longitude = longitude;
        this.hemisphere = hemisphere;
        this.zone = zone;
        this.easting = easting;
        this.northing = northing;
    }

    public Angle getLatitude()
    {
        return this.latitude;
    }

    public Angle getLongitude()
    {
        return this.longitude;
    }

    public int getZone()
    {
        return this.zone;
    }

    public char getHemisphere()
    {
        return this.hemisphere;
    }

    public double getEasting()
    {
        return this.easting;
    }

    public double getNorthing()
    {
        return this.northing;
    }

    public String toString()
    {
        return zone + " " + hemisphere + " " + (int)easting + "E" + " " + (int)northing + "N";
    }


}
