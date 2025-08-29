package io.kontur.eventapi.enrichment.postprocessor;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.*;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;

import java.util.*;
import java.util.stream.Collectors;

import static io.kontur.eventapi.enrichment.EnrichmentConfig.*;
import static io.kontur.eventapi.entity.EventType.*;
import static io.kontur.eventapi.util.GeometryUtil.*;
import static java.lang.Math.abs;
import static java.util.stream.Collectors.toMap;
import static org.jpmml.evaluator.EvaluatorUtil.decodeAll;
import static org.locationtech.jts.operation.buffer.BufferOp.bufferOp;

@Component
public class LossPostProcessor extends EnrichmentPostProcessor {

    private static final String TARGET = "cost";
    private static final double errorBoundPct = 1.2;
    private static final Set<EventType> allowedEventTypes = Set.of(FLOOD, WILDFIRE, TORNADO, WINTER_STORM, EARTHQUAKE,
            STORM, CYCLONE, DROUGHT, VOLCANO);
    private static final Set<String> allowedAreaTypes = Set.of(ALERT_AREA, EXPOSURE, POSITION);
    private static final Set<String> requiredAnalytics = Set.of(POPULATION, GDP, BUILDING_COUNT, HIGHWAY_LENGTH, INDUSTRIAL_AREA_KM2);
    private final Evaluator evaluator;
    private static final Logger LOG = LoggerFactory.getLogger(LossPostProcessor.class);


    public LossPostProcessor(Evaluator lossEvaluator) {
        this.evaluator = lossEvaluator;
    }

    @Override
    public void process(FeedData event) {
        if (!isApplicable(event)) return;
        Geometry geometry = unionGeometry(toGeometryCollection(event.getGeometries()));
        if (geometry == null) return;
        Double area = calculateAreaKm2(geometry);
        EventType type = getType(event);
        if (area > 0 && allowedEventTypes.contains(type)) {
            Map<String, Double> features = getFeatures(event.getEventDetails(), area, type);
            Map<FieldName, FieldValue> arguments = new HashMap<>();
            for (ModelField field : evaluator.getInputFields()) {
                String name = field.getName().getValue();
                Double value = features.get(name);
                if (value == null) {
                    LOG.warn("Missing feature {} for loss calculation", name);
                    event.getEventDetails().put(LOSS, 0.);
                    event.getEventDetails().put(LOSS_BOUND, 0.);
                    return;
                }
                arguments.put(field.getName(), field.prepare(value));
            }
            try {
                Map<String, ?> results = decodeAll(evaluator.evaluate(arguments));
                Object evaluation = results.get(TARGET);
                double loss = 0.;
                if (evaluation instanceof Number number) {
                    loss = number.doubleValue();
                    if (loss < 0) {
                        loss = 0.;
                    }
                }
                event.getEventDetails().put(LOSS, loss);
                event.getEventDetails().put(LOSS_BOUND, abs(loss * errorBoundPct));
            } catch (EvaluationException e) {
                LOG.error("Failed to evaluate loss model", e);
                event.getEventDetails().put(LOSS, 0.);
                event.getEventDetails().put(LOSS_BOUND, 0.);
            }
        }
    }

    @Override
    public boolean isApplicable(Feed feed) {
        return feed.getEnrichmentPostProcessors().contains(LOSS_POSTPROCESSOR);
    }

    public boolean isApplicable(FeedData event) {
        return event.getEventDetails() != null
                && event.getEventDetails().containsKey(POPULATION) && event.getEventDetails().get(POPULATION) != null
                && event.getEventDetails().containsKey(GDP) && event.getEventDetails().get(GDP) != null
                && event.getEventDetails().containsKey(BUILDING_COUNT) && event.getEventDetails().get(BUILDING_COUNT) != null
                && event.getEventDetails().containsKey(HIGHWAY_LENGTH) && event.getEventDetails().get(HIGHWAY_LENGTH) != null
                && event.getEventDetails().containsKey(INDUSTRIAL_AREA_KM2) && event.getEventDetails().get(INDUSTRIAL_AREA_KM2) != null
                && event.getGeometries().getFeatures() != null && event.getGeometries().getFeatures().length > 0;
    }

    @Override
    protected Set<Geometry> toGeometryCollection(FeatureCollection fc) {
        return Arrays.stream(fc.getFeatures())
                .filter(feature -> allowedAreaTypes.contains(String.valueOf(feature.getProperties().get(AREA_TYPE_PROPERTY))))
                .map(feature -> geoJSONReader.read(feature.getGeometry()))
                .map(geom -> bufferOp(geom, 0))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private EventType getType(FeedData event) {
        return event.getEpisodes()
                .stream()
                .map(FeedEpisode::getType)
                .findFirst()
                .orElseThrow();
    }

    private Map<String, Double> getFeatures(Map<String, Object> eventDetails, Double area, EventType type) {
        Map<String, Double> features = eventDetails.entrySet()
                .stream()
                .filter(field -> requiredAnalytics.contains(field.getKey()))
                .collect(toMap(Map.Entry::getKey, field -> toDouble(field.getValue())));
        features.replace(HIGHWAY_LENGTH, features.get(HIGHWAY_LENGTH) / 1000);
        features.put("area", area);
        allowedEventTypes.forEach(allowedType -> features.put(allowedType.name(), allowedType.equals(type) ? 1. : 0.));
        return features;
    }
}
