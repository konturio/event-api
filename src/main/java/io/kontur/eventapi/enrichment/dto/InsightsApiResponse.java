package io.kontur.eventapi.enrichment.dto;

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
        private Analytics analytics;
    }

    @Data
    public static class Analytics {
        private List<AnalyticFunction> functions;
        private Population population;
    }

    @Data
    public static class AnalyticFunction {
        private String id;
        private Object result;
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
