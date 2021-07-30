package io.kontur.eventapi.enrichment;

import java.util.Set;

public class EnrichmentConfig {
    public static final String POPULATION = "population";
    public static final String HUMANITARIAN_IMPACT = "humanitarianImpact";
    public static final String PEOPLE_WITHOUT_OSM_BUILDINGS = "peopleWithoutOsmBuildings";
    public static final String AREA_WITHOUT_OSM_BUILDINGS_KM2 = "areaWithoutOsmBuildingsKm2";
    public static final String PEOPLE_WITHOUT_OSM_ROADS = "peopleWithoutOsmRoads";
    public static final String AREA_WITHOUT_OSM_ROADS_KM2 = "areaWithoutOsmRoadsKm2";
    public static final String PEOPLE_WITHOUT_OSM_OBJECTS = "peopleWithoutOsmObjects";
    public static final String AREA_WITHOUT_OSM_OBJECTS_KM2 = "areaWithoutOsmObjectsKm2";
    public static final String OSM_GAPS_PERCENTAGE = "osmGapsPercentage";

    public static final Set<String> osmQuality = Set.of(
            OSM_GAPS_PERCENTAGE,
            PEOPLE_WITHOUT_OSM_BUILDINGS,
            AREA_WITHOUT_OSM_BUILDINGS_KM2,
            PEOPLE_WITHOUT_OSM_ROADS,
            AREA_WITHOUT_OSM_ROADS_KM2,
            PEOPLE_WITHOUT_OSM_OBJECTS,
            AREA_WITHOUT_OSM_OBJECTS_KM2);

    public static final Set<String> population = Set.of(POPULATION);

    public static final Set<String> humanitarianImpact = Set.of(HUMANITARIAN_IMPACT);
}
