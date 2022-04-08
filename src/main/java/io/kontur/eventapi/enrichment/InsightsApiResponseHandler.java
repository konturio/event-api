package io.kontur.eventapi.enrichment;

import io.kontur.eventapi.enrichment.dto.InsightsApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.kontur.eventapi.enrichment.EnrichmentConfig.*;

public class InsightsApiResponseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(InsightsApiResponseHandler.class);

    public static Map<String, Object> processResponse(InsightsApiResponse response, List<String> enrichmentFields) throws Exception {
        checkResponseErrors(response.getErrors());
        Map<String, Object> analytics = new HashMap<>();
        enrichmentFields.forEach(field -> {
            try {
                Object value = getFieldFromResponse(field, response.getData().getPolygonStatistic().getAnalytics());
                analytics.put(field, value);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        });
        return analytics;
    }

    private static void checkResponseErrors(List<InsightsApiResponse.ResponseError> errors) throws Exception {
        if (errors != null && !errors.isEmpty()) {
            String message = errors.stream()
                    .map(InsightsApiResponse.ResponseError::getMessage)
                    .collect(Collectors.joining("\n"));
            throw new Exception(message);
        }
    }

    private static Object getFieldFromResponse(String field, InsightsApiResponse.Analytics data) throws Exception {
        return switch (field) {
            case POPULATION -> data.getPopulation().getPopulation();
            case GDP -> data.getPopulation().getGdp();
            default -> findFunctionResult(data.getFunctions(), field);
        };
    }

    private static Object findFunctionResult(List<InsightsApiResponse.AnalyticFunction> functions, String id) throws Exception {
        List<Object> results = functions.stream()
                .filter(function -> function.getId().equals(id))
                .map(InsightsApiResponse.AnalyticFunction::getResult)
                .toList();
        if (results.isEmpty()) {
            throw new Exception("Enrichment field is not present: id = " + id);
        }
        return results.get(0);
    }
}
