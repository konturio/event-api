package io.kontur.eventapi.enrichment;

import lombok.Data;

import java.util.List;

@Data
public class InsightsApiResponse {
    private ResponseData data;
    private List<ResponseError> errors;

    @Data
    public static class ResponseData {
        private PolygonStatistic polygonStatistic;
    }

    @Data
    public static class PolygonStatistic {
        private PopulationStatistic populationStatistic;
    }

    @Data
    public static class PopulationStatistic {
        private OsmQuality osmQuality;
        private Population population;
        private String humanitarianImpact;
    }

    @Data
    public static class OsmQuality {
        private Double osmGapsPercentage;
        private Long peopleWithoutOsmBuildings;
        private Double areaWithoutOsmBuildingsKm2;
        private Long peopleWithoutOsmRoads;
        private Double areaWithoutOsmRoadsKm2;
        private Long peopleWithoutOsmObjects;
        private Double areaWithoutOsmObjectsKm2;
    }

    @Data
    public static class Population {
        private Long population;
    }

    @Data
    public static class ResponseError {
        private String message;
    }
}
