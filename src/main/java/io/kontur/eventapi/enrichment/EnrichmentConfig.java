package io.kontur.eventapi.enrichment;

import java.util.HashMap;
import java.util.Map;

public class EnrichmentConfig {
    public static final String POPULATION = "population";
    public static final String PEOPLE_WITHOUT_OSM_BUILDINGS = "peopleWithoutOsmBuildings";
    public static final String AREA_WITHOUT_OSM_BUILDINGS_KM2 = "areaWithoutOsmBuildingsKm2";
    public static final String PEOPLE_WITHOUT_OSM_ROADS = "peopleWithoutOsmRoads";
    public static final String AREA_WITHOUT_OSM_ROADS_KM2 = "areaWithoutOsmRoadsKm2";
    public static final String PEOPLE_WITHOUT_OSM_OBJECTS = "peopleWithoutOsmObjects";
    public static final String AREA_WITHOUT_OSM_OBJECTS_KM2 = "areaWithoutOsmObjectsKm2";
    public static final String OSM_GAPS_PERCENTAGE = "osmGapsPercentage";
    public static final String INDUSTRIAL_AREA_KM2 = "industrialAreaKm2";
    public static final String FOREST_AREA_KM2 = "forestAreaKm2";
    public static final String VOLCANOES_COUNT = "volcanoesCount";
    public static final String HOTSPOT_DAYS_PER_YEAR_MAX = "hotspotDaysPerYearMax";
    public static final String HUMANITARIAN_IMPACT = "humanitarianImpact";

    public static final String OSM_QUALITY_GROUP = "osmQuality";
    public static final String POPULATION_GROUP = "population";
    public static final String THERMAL_SPOT_STATISTIC_GROUP = "thermalSpotStatistic";
    public static final String NO_GROUP = "no group";

    public static Map<String, String> groups = new HashMap<>();
    static {
        groups.put(POPULATION, POPULATION_GROUP);

        groups.put(PEOPLE_WITHOUT_OSM_BUILDINGS, OSM_QUALITY_GROUP);
        groups.put(AREA_WITHOUT_OSM_BUILDINGS_KM2, OSM_QUALITY_GROUP);
        groups.put(PEOPLE_WITHOUT_OSM_OBJECTS, OSM_QUALITY_GROUP);
        groups.put(AREA_WITHOUT_OSM_OBJECTS_KM2, OSM_QUALITY_GROUP);
        groups.put(PEOPLE_WITHOUT_OSM_ROADS, OSM_QUALITY_GROUP);
        groups.put(AREA_WITHOUT_OSM_ROADS_KM2, OSM_QUALITY_GROUP);
        groups.put(OSM_GAPS_PERCENTAGE, OSM_QUALITY_GROUP);

        groups.put(INDUSTRIAL_AREA_KM2, THERMAL_SPOT_STATISTIC_GROUP);
        groups.put(FOREST_AREA_KM2, THERMAL_SPOT_STATISTIC_GROUP);
        groups.put(VOLCANOES_COUNT, THERMAL_SPOT_STATISTIC_GROUP);
        groups.put(HOTSPOT_DAYS_PER_YEAR_MAX, THERMAL_SPOT_STATISTIC_GROUP);

        groups.put(HUMANITARIAN_IMPACT, NO_GROUP);
    }
}
