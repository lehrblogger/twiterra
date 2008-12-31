/********************************************************************/
/* RSC IDENTIFIER: UPS
 *
 *
 * ABSTRACT
 *
 *    This component provides conversions between geodetic (latitude
 *    and longitude) coordinates and Universal Polar Stereographic (UPS)
 *    projection (hemisphere, easting, and northing) coordinates.
 *
 *
 * ERROR HANDLING
 *
 *    This component checks parameters for valid values.  If an
 *    invalid value is found the error code is combined with the
 *    current error code using the bitwise or.  This combining allows
 *    multiple error codes to be returned. The possible error codes
 *    are:
 *
 *         UPS_NO_ERROR           : No errors occurred in function
 *         UPS_LAT_ERROR          : Latitude outside of valid range
 *                                   (North Pole: 83.5 to 90,
 *                                    South Pole: -79.5 to -90)
 *         UPS_LON_ERROR          : Longitude outside of valid range
 *                                   (-180 to 360 degrees)
 *         UPS_HEMISPHERE_ERROR   : Invalid hemisphere ('N' or 'S')
 *         UPS_EASTING_ERROR      : Easting outside of valid range,
 *                                   (0 to 4,000,000m)
 *         UPS_NORTHING_ERROR     : Northing outside of valid range,
 *                                   (0 to 4,000,000m)
 *         UPS_A_ERROR            : Semi-major axis less than or equal to zero
 *         UPS_INV_F_ERROR        : Inverse flattening outside of valid range
 *								  	               (250 to 350)
 *
 *
 * REUSE NOTES
 *
 *    UPS is intended for reuse by any application that performs a Universal
 *    Polar Stereographic (UPS) projection.
 *
 *
 * REFERENCES
 *
 *    Further information on UPS can be found in the Reuse Manual.
 *
 *    UPS originated from :  U.S. Army Topographic Engineering Center
 *                           Geospatial Information Division
 *                           7701 Telegraph Road
 *                           Alexandria, VA  22310-3864
 *
 *
 * LICENSES
 *
 *    None apply to this component.
 *
 *
 * RESTRICTIONS
 *
 *    UPS has no restrictions.
 *
 *
 * ENVIRONMENT
 *
 *    UPS was tested and certified in the following environments:
 *
 *    1. Solaris 2.5 with GCC version 2.8.1
 *    2. Windows 95 with MS Visual C++ version 6
 *
 *
 * MODIFICATIONS
 *
 *    Date              Description
 *    ----              -----------
 *    06-11-95          Original Code
 *    03-01-97          Original Code
 *
 *
 */

package gov.nasa.worldwind.geom.coords;

/**
 * Ported to Java from the NGA GeoTrans ups.c and ups.h code
 *
 * @author Garrett Headley
 * @version Feb 12, 2007 4:52:59 PM
 */
public class UPSConverter
{
    public static final int UPS_NO_ERROR = 0x0000;
    private static final int UPS_LAT_ERROR = 0x0001;
    private static final int UPS_LON_ERROR = 0x0002;
    public static final int UPS_HEMISPHERE_ERROR = 0x0004;
    public static final int UPS_EASTING_ERROR = 0x0008;
    public static final int UPS_NORTHING_ERROR = 0x0010;
    private static final int UPS_A_ERROR = 0x0020;
    private static final int UPS_INV_F_ERROR = 0x0040;

    private static final double PI = 3.14159265358979323;
    private static final double MAX_LAT = (PI * 90) / 180.0;             // 90 degrees in radians
    private static final double MIN_NORTH_LAT = 83.5 * PI / 180.0;
    private static final double MIN_SOUTH_LAT = -79.5 * PI / 180.0;
    private static final double MAX_ORIGIN_LAT = (81.114528 * PI) / 180.0;
    private static final double MIN_EAST_NORTH = 0;
    private static final double MAX_EAST_NORTH = 4000000;

    private static double UPS_Origin_Latitude = MAX_ORIGIN_LAT;  /*set default = North Hemisphere */
    private static double UPS_Origin_Longitude = 0.0;

    /* Ellipsoid Parameters, default to WGS 84  */
    private static double UPS_a = 6378137.0;          /* Semi-major axis of ellipsoid in meters   */
    private static double UPS_f = 1 / 298.257223563;  /* Flattening of ellipsoid  */
    private static double UPS_False_Easting = 2000000.0;
    private static double UPS_False_Northing = 2000000.0;
    private static double false_easting = 0.0;
    private static double false_northing = 0.0;
    private static double UPS_Easting = 0.0;
    private static double UPS_Northing = 0.0;

    private static double Easting = 0.0;
    private static double Northing = 0.0;
    private static char Hemisphere = 'N';
    private static double Latitude = 0.0;
    private static double Longitude = 0.0;

    // This constructor will never be invoked
    private UPSConverter()
    {
    }

    /**
     * The function SetUPSParameters receives the ellipsoid parameters and sets the corresponding state variables. If
     * any errors occur, the error code(s) are returned by the function, otherwise UPS_NO_ERROR is returned.
     *
     * @param a Semi-major axis of ellipsoid in meters
     * @param f Flattening of ellipsoid
     * @return error code
     */
    public static long SetUPSParameters(double a, double f)
    {
        double inv_f = 1 / f;

        if (a <= 0.0)
        { /* Semi-major axis must be greater than zero */
            return UPS_A_ERROR;
        }
        if ((inv_f < 250) || (inv_f > 350))
        { /* Inverse flattening must be between 250 and 350 */
            return UPS_INV_F_ERROR;
        }

        UPS_a = a;
        UPS_f = f;

        return (UPS_NO_ERROR);
    }

    /**
     * The function convertGeodeticToUPS converts geodetic (latitude and longitude) coordinates to UPS (hemisphere,
     * easting, and northing) coordinates, according to the current ellipsoid parameters. If any errors occur, the error
     * code(s) are returned by the function, otherwide UPS_NO_ERROR is returned.
     *
     * @param latitude   Latitude in radians
     * @param longitude  Longitude in radians
     * @return error code
     */
    public static long convertGeodeticToUPS(double latitude, double longitude)
    {
        if ((latitude < -MAX_LAT) || (latitude > MAX_LAT))
        {   /* latitude out of range */
            return UPS_LAT_ERROR;
        }
        if ((latitude < 0) && (latitude > MIN_SOUTH_LAT))
            return UPS_LAT_ERROR;
        if ((latitude >= 0) && (latitude < MIN_NORTH_LAT))
            return UPS_LAT_ERROR;

        if ((longitude < -PI) || (longitude > (2 * PI)))
        {  /* slam out of range */
            return UPS_LON_ERROR;
        }

        if (latitude < 0)
        {
            UPS_Origin_Latitude = -MAX_ORIGIN_LAT;
            Hemisphere = 'S';
        } else
        {
            UPS_Origin_Latitude = MAX_ORIGIN_LAT;
            Hemisphere = 'N';
        }

        PolarConverter.setPolarStereographicParameters(UPS_a, UPS_f,
                UPS_Origin_Latitude, UPS_Origin_Longitude,
                false_easting, false_northing);

        PolarConverter.Convert_Geodetic_To_Polar_Stereographic(latitude, longitude);

        UPS_Easting = UPS_False_Easting + PolarConverter.getEasting();
        UPS_Northing = UPS_False_Northing + PolarConverter.getNorthing();

        Easting = UPS_Easting;
        Northing = UPS_Northing;

        return UPS_NO_ERROR;
    }

    /**
     * @return Easting/X in meters
     */
    public static double getEasting()
    {
        return Easting;
    }

    /**
     * @return Northing/Y in meters
     */
    public static double getNorthing()
    {
        return Northing;
    }

    /**
     * @return Hemisphere 'N' or 'S'
     */
    public static char getHemisphere()
    {
        return Hemisphere;
    }

    /**
     *  The function Convert_UPS_To_Geodetic converts UPS (hemisphere, easting,
     *  and northing) coordinates to geodetic (latitude and longitude) coordinates
     *  according to the current ellipsoid parameters.  If any errors occur, the
     *  error code(s) are returned by the function, otherwise UPS_NO_ERROR is
     *  returned.
     *
     *  @param Hemisphere Hemisphere either 'N' or 'S'
     *  @param  Easting Easting/X in meters
     *  @param  Northing Northing/Y in meters
     *  @return error code
     */
    public static long convertUPSToGeodetic(char Hemisphere, double Easting, double Northing)
    {
        long Error_Code = UPS_NO_ERROR;

        if ((Hemisphere != 'N') && (Hemisphere != 'S'))
            Error_Code |= UPS_HEMISPHERE_ERROR;
        if ((Easting < MIN_EAST_NORTH) || (Easting > MAX_EAST_NORTH))
            Error_Code |= UPS_EASTING_ERROR;
        if ((Northing < MIN_EAST_NORTH) || (Northing > MAX_EAST_NORTH))
            Error_Code |= UPS_NORTHING_ERROR;

        if (Hemisphere =='N')
            UPS_Origin_Latitude = MAX_ORIGIN_LAT;
        if (Hemisphere =='S')
            UPS_Origin_Latitude = -MAX_ORIGIN_LAT;

        if (Error_Code == UPS_NO_ERROR)
        {   /*  no errors   */
            PolarConverter.setPolarStereographicParameters(UPS_a,
                    UPS_f,
                    UPS_Origin_Latitude,
                    UPS_Origin_Longitude,
                    UPS_False_Easting,
                    UPS_False_Northing);

            PolarConverter.Convert_Polar_Stereographic_To_Geodetic( Easting, Northing);
            Latitude = PolarConverter.getLatitude();
            Longitude = PolarConverter.getLongitude();

            if ((Latitude < 0) && (Latitude > MIN_SOUTH_LAT))
                Error_Code |= UPS_LAT_ERROR;
            if ((Latitude >= 0) && (Latitude < MIN_NORTH_LAT))
                Error_Code |= UPS_LAT_ERROR;
        }
        return Error_Code;
    }

    /**
     * @return Latitude in radians.
     */
    public static double getLatitude()
    {
        return Latitude;
    }

    /**
     * @return Longitude in radians.
     */
    public static double getLongitude()
    {
        return Longitude;
    }

}


