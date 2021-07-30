package io.kontur.eventapi.enrichment;

import org.wololo.geojson.FeatureCollection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.kontur.eventapi.enrichment.EnrichmentConfig.*;
import static io.kontur.eventapi.util.JsonUtil.readJson;

public class InsightsApiResponseHandler {

    public static Map<String, Object> processResponse(InsightsApiResponse response, List<String> enrichmentParams) throws Exception {
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            String message = response.getErrors().stream().map(InsightsApiResponse.ResponseError::getMessage)
                    .collect(Collectors.joining("\n"));
            throw new Exception(message);
        }
        Map<String, Object> analytics = new HashMap<>();
        enrichmentParams.forEach(param -> analytics.put(param, getParamFromResponse(param, response.getData().getPolygonStatistic().getPopulationStatistic())));
        return analytics;
    }

    private static Object getParamFromResponse(String param, InsightsApiResponse.PopulationStatistic data) {
        switch (param) {
            case POPULATION: return data.getPopulation().getPopulation();
            case HUMANITARIAN_IMPACT: return readJson(data.getHumanitarianImpact(), FeatureCollection.class);
            case PEOPLE_WITHOUT_OSM_BUILDINGS: return data.getOsmQuality().getPeopleWithoutOsmBuildings();
            case AREA_WITHOUT_OSM_BUILDINGS_KM2: return data.getOsmQuality().getAreaWithoutOsmBuildingsKm2();
            case PEOPLE_WITHOUT_OSM_ROADS: return data.getOsmQuality().getPeopleWithoutOsmRoads();
            case AREA_WITHOUT_OSM_ROADS_KM2: return data.getOsmQuality().getAreaWithoutOsmRoadsKm2();
            case PEOPLE_WITHOUT_OSM_OBJECTS: return data.getOsmQuality().getPeopleWithoutOsmObjects();
            case AREA_WITHOUT_OSM_OBJECTS_KM2: return data.getOsmQuality().getAreaWithoutOsmObjectsKm2();
            case OSM_GAPS_PERCENTAGE: return data.getOsmQuality().getOsmGapsPercentage();
            default: return null;
        }
    }
}
