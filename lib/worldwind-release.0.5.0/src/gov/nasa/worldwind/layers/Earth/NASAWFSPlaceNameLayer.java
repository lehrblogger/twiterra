/*
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.layers.placename.PlaceNameServiceSet;
import gov.nasa.worldwind.layers.placename.PlaceNameService;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Arrays;


public class NASAWFSPlaceNameLayer extends PlaceNameLayer {
    private static final double LEVEL_A = 0x1 << 25;
    private static final double LEVEL_B = 0x1 << 24;
    private static final double LEVEL_C = 0x1 << 23;
    private static final double LEVEL_D = 0x1 << 22;
    //  private static final double LEVEL_E = 0x1 << 21;
    private static final double LEVEL_F = 0x1 << 20;
    private static final double LEVEL_G = 0x1 << 19;
    //  private static final double LEVEL_H = 0x1 << 18;
    private static final double LEVEL_I = 0x1 << 17;
    private static final double LEVEL_J = 0x1 << 16;
    private static final double LEVEL_K = 0x1 << 15;
    private static final double LEVEL_L = 0x1 << 14;
    //private static final double LEVEL_M = 0x1 << 13;
    private static final double LEVEL_N = 0x1 << 12;
    private static final double LEVEL_O = 0x1 << 11;
    private static final double LEVEL_P = 0x1 << 10;
    private static final LatLon GRID_1x1 = new LatLon(Angle.fromDegrees(180d), Angle.fromDegrees(360d));
    //  private static final LatLon GRID_2x4 = new LatLon(Angle.fromDegrees(90d), Angle.fromDegrees(90d));
    private static final LatLon GRID_5x10 = new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d));
    private static final LatLon GRID_10x20 = new LatLon(Angle.fromDegrees(18d), Angle.fromDegrees(18d));
    private static final LatLon GRID_20x40 = new LatLon(Angle.fromDegrees(9d), Angle.fromDegrees(9d));
    private static final LatLon GRID_40x80 = new LatLon(Angle.fromDegrees(4d), Angle.fromDegrees(4d));
    private static final LatLon GRID_80x160 = new LatLon(Angle.fromDegrees(2d), Angle.fromDegrees(2d));

    //String constants for name sets
    public static final String OCEANS="topp:wpl_oceans";
    public static final String CONTINENTS="topp:wpl_continents";
    public static final String WATERBODIES="topp:wpl_waterbodies";
    public static final String TRENCHESRIDGES="topp:wpl_trenchesridges";
    public static final String DESERTSPLAINS="topp:wpl_desertsplains";
    public static final String LAKESRIVERS="topp:wpl_lakesrivers";
    public static final String MOUNTAINSVALLEYS="topp:wpl_mountainsvalleys";
    public static final String COUNTRIES="topp:wpl_countries";
    public static final String GEONET_P_PPC="topp:wpl_geonet_p_pplc";
    public static final String USCITIESOVER500K="topp:wpl_uscitiesover500k";
    public static final String USCITIESOVER100K="topp:wpl_uscitiesover100k";
    public static final String USCITIESOVER50K="topp:wpl_uscitiesover50k";
    public static final String USCITIESOVER10K="topp:wpl_uscitiesover10k";
    public static final String USCITIESOVER1K="topp:wpl_uscitiesover1k";
    public static final String USCITIESOVER0="topp:wpl_uscitiesover0";
    public static final String USCITIES0="topp:wpl_uscities0";
    public static final String US_ANTHROPOGENIC="topp:wpl_us_anthropogenic";
    public static final String US_WATER="topp:wpl_us_water";
    public static final String US_TERRAIN="topp:wpl_us_terrain";
    public static final String GEONET_A_ADM1="topp:wpl_geonet_a_adm1";
    public static final String GEONET_A_ADM2="topp:wpl_geonet_a_adm2";
    public static final String GEONET_P_PPLA="topp:wpl_geonet_p_ppla";
    public static final String GEONET_P_PPL="topp:wpl_geonet_p_ppl";
    public static final String GEONET_P_PPLC="topp:wpl_geonet_p_pplC";


    private static final String[] allNameSets={OCEANS, CONTINENTS, WATERBODIES, TRENCHESRIDGES, DESERTSPLAINS, LAKESRIVERS,
                                    MOUNTAINSVALLEYS, COUNTRIES, GEONET_P_PPC, USCITIESOVER500K, USCITIESOVER100K,
                                    USCITIESOVER50K, USCITIESOVER10K, USCITIESOVER1K, USCITIESOVER0,USCITIES0,
                                    US_ANTHROPOGENIC, US_WATER, US_TERRAIN, GEONET_A_ADM1, GEONET_A_ADM2,
                                    GEONET_P_PPLA, GEONET_P_PPL};

    private static List activeNamesList = Arrays.asList(allNameSets);
    
    public NASAWFSPlaceNameLayer() {
        super(makePlaceNameServiceSet());
    }

    public void setPlaceNameSetsVisible(List names)
    {
        activeNamesList=names;
        makePlaceNameServiceSet();
    }

    private static PlaceNameServiceSet makePlaceNameServiceSet() {
        final String service = "http://builds.worldwind.arc.nasa.gov/geoserver/wfs";
        final String fileCachePath = "Earth/NASA WFS Place Names";
        PlaceNameServiceSet placeNameServiceSet = new PlaceNameServiceSet();
        placeNameServiceSet.setExpiryTime(new GregorianCalendar(2008, 1, 11).getTimeInMillis());
        PlaceNameService placeNameService;

        final Sector usSector= Sector.fromDegrees(0d,90d,-180,0);
        // Oceans
        if (activeNamesList.contains(OCEANS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_oceans", fileCachePath, Sector.FULL_SPHERE, GRID_1x1,
                    java.awt.Font.decode("Arial-BOLDITALIC-12"));
            placeNameService.setColor(new java.awt.Color(200, 200, 200));
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_A);
            placeNameServiceSet.addService(placeNameService, false);
        }

        // Continents
        if (activeNamesList.contains(CONTINENTS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_continents", fileCachePath, Sector.FULL_SPHERE,
                    GRID_1x1,
                    java.awt.Font.decode("Arial-BOLD-12"));
            placeNameService.setColor(new java.awt.Color(255, 255, 240));
            placeNameService.setMinDisplayDistance(LEVEL_G);
            placeNameService.setMaxDisplayDistance(LEVEL_A);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Water Bodies
        if (activeNamesList.contains(WATERBODIES)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_waterbodies", fileCachePath, Sector.FULL_SPHERE,
                    GRID_5x10,
                    java.awt.Font.decode("Arial-ITALIC-10"));
            placeNameService.setColor(java.awt.Color.cyan);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_B);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Trenches & Ridges
        if (activeNamesList.contains(TRENCHESRIDGES)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_trenchesridges", fileCachePath, Sector.FULL_SPHERE,
                    GRID_5x10,
                    java.awt.Font.decode("Arial-BOLDITALIC-10"));
            placeNameService.setColor(java.awt.Color.cyan);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_B);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Deserts & Plains
        if (activeNamesList.contains(DESERTSPLAINS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_desertsplains", fileCachePath, Sector.FULL_SPHERE,
                    GRID_5x10,
                    java.awt.Font.decode("Arial-BOLDITALIC-10"));
            placeNameService.setColor(java.awt.Color.orange);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_B);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Lakes & Rivers
        if (activeNamesList.contains(LAKESRIVERS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_lakesrivers", fileCachePath, Sector.FULL_SPHERE,
                    GRID_10x20,
                    java.awt.Font.decode("Arial-ITALIC-10"));
            placeNameService.setColor(java.awt.Color.cyan);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_C);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Mountains & Valleys
        if (activeNamesList.contains(MOUNTAINSVALLEYS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_mountainsvalleys", fileCachePath, Sector.FULL_SPHERE,
                    GRID_10x20,
                    java.awt.Font.decode("Arial-BOLDITALIC-10"));
            placeNameService.setColor(java.awt.Color.orange);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_C);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Countries
        if (activeNamesList.contains(COUNTRIES)) {
            placeNameService = new PlaceNameService(service, "topp:countries", fileCachePath, Sector.FULL_SPHERE, GRID_5x10,
                    java.awt.Font.decode("Arial-BOLD-10"));
            placeNameService.setColor(java.awt.Color.white);
            placeNameService.setMinDisplayDistance(LEVEL_G);
            placeNameService.setMaxDisplayDistance(LEVEL_D);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNet World Capitals
        if (activeNamesList.contains(GEONET_P_PPLC)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_p_pplc", fileCachePath, Sector.FULL_SPHERE,
                    GRID_10x20,
                    java.awt.Font.decode("Arial-BOLD-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_D);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (Population Over 500k)
        if (activeNamesList.contains(USCITIESOVER500K)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover500k", fileCachePath, usSector,
                    GRID_10x20,
                    java.awt.Font.decode("Arial-BOLD-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(LEVEL_N);
            placeNameService.setMaxDisplayDistance(LEVEL_D);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (Population Over 100k)
        if (activeNamesList.contains(USCITIESOVER100K)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover100k", fileCachePath, usSector,
                    GRID_10x20,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(LEVEL_N);
            placeNameService.setMaxDisplayDistance(LEVEL_F);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (Population Over 50k)
        if (activeNamesList.contains(USCITIESOVER50K)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover50k", fileCachePath, usSector,
                    GRID_10x20,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(LEVEL_N);
            placeNameService.setMaxDisplayDistance(LEVEL_I);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (Population Over 10k)
        if (activeNamesList.contains(USCITIESOVER10K)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover10k", fileCachePath, usSector,
                    GRID_10x20,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_J);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (Population Over 1k)
        if (activeNamesList.contains(USCITIESOVER1K)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover1k", fileCachePath, usSector,
                    GRID_20x40,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_K);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (Population Over 0)
        if (activeNamesList.contains(USCITIESOVER0)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover0", fileCachePath, usSector,
                    GRID_20x40,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_L);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (No Population)
        if (activeNamesList.contains(USCITIES0)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_uscities0", fileCachePath, usSector,
                    GRID_40x80,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.orange);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);//M);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Anthropogenic Features
        if (activeNamesList.contains(US_ANTHROPOGENIC)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_us_anthropogenic", fileCachePath, usSector, GRID_80x160,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.orange);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_P);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Water Features
        if (activeNamesList.contains(US_WATER)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_us_water", fileCachePath, usSector, GRID_20x40,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.cyan);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Terrain Features
        if (activeNamesList.contains(US_TERRAIN)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_us_terrain", fileCachePath, usSector, GRID_20x40,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.orange);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_O);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNET Administrative 1st Order
        if (activeNamesList.contains(GEONET_A_ADM1)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_a_adm1", fileCachePath, Sector.FULL_SPHERE, GRID_20x40,
                    java.awt.Font.decode("Arial-BOLD-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNET Administrative 2nd Order
        if (activeNamesList.contains(GEONET_A_ADM2)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_a_adm2", fileCachePath, Sector.FULL_SPHERE, GRID_20x40,
                    java.awt.Font.decode("Arial-BOLD-10"));
            placeNameService.setColor(java.awt.Color.yellow);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNET Populated Place Administrative
        if (activeNamesList.contains(GEONET_P_PPLA)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_p_ppla", fileCachePath, Sector.FULL_SPHERE, GRID_20x40,
                    java.awt.Font.decode("Arial-BOLD-10"));
            placeNameService.setColor(java.awt.Color.pink);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNET Populated Place
        if (activeNamesList.contains(GEONET_P_PPL)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_p_ppl", fileCachePath, Sector.FULL_SPHERE, GRID_20x40,
                    java.awt.Font.decode("Arial-PLAIN-10"));
            placeNameService.setColor(java.awt.Color.pink);
            placeNameService.setMinDisplayDistance(0d);
            placeNameService.setMaxDisplayDistance(LEVEL_O);
            placeNameServiceSet.addService(placeNameService, false);
        }
        return placeNameServiceSet;
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.Earth.PlaceName.Name");
    }
}
