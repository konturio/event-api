package io.kontur.eventapi.enrichment;

import org.apache.commons.lang3.RegExUtils;
import org.wololo.geojson.FeatureCollection;

import java.util.*;

import static io.kontur.eventapi.enrichment.EnrichmentConfig.*;

public class InsightsApiRequestBuilder {

    private static final String paramsPattern = "analytics { %s }";
    private static final String queryPattern = "{ polygonStatistic(polygonStatisticRequest: {polygon: \"%s\"}) { %s } }";

    public static InsightsApiRequest buildRequest(FeatureCollection geometry, String params) {
        String geometryString = RegExUtils.replaceAll(geometry.toString(), "\"", "\\\\\\\"");
        String query = String.format(queryPattern, geometryString, params);
        return new InsightsApiRequest(query);
    }

    public static String buildParams(List<String> params) {
        Map<String, String> paramsWithGroups = processParams(params);
        StringBuilder sb = new StringBuilder();
        for (var group : paramsWithGroups.entrySet()) {
            if (group.getKey().equals(NO_GROUP)) {
                sb.append(group.getValue());
            } else {
                sb.append(String.format("%s { %s }", group.getKey(), group.getValue()));
            }
            sb.append(" ");
        }
        return String.format(paramsPattern, sb);
    }

    private static Map<String, String> processParams(List<String> params) {
        Map<String, String> paramsWithGroups = new HashMap<>();
        for (String param : params) {
            String group = groups.getOrDefault(param, NO_GROUP);
            paramsWithGroups.putIfAbsent(group, "");
            paramsWithGroups.computeIfPresent(group, (key, val) -> val + " " + param);
        }
        return paramsWithGroups;
    }



}
