package io.kontur.eventapi.staticdata.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AustraliaWildfireNormalizer extends StaticNormalizer {

    private final Map<String, HandleFunction<Map<String, Object>, NormalizedObservation>> handlers = Map.of(
            "wildfire.sa-gov", this::handleSaGov,
            "wildfire.qld-des-gov", this::handleQldDesGov,
            "wildfire.victoria-gov", this::handleVictoriaGov,
            "wildfire.nsw-gov", this::handleNswGov
    );

    private final static GeoJSONReader reader = new GeoJSONReader();

    @Override
    protected List<String> getProviders() {
        return new ArrayList<>(handlers.keySet());
    }

    @Override
    protected void setExtraFields(DataLake dataLake, NormalizedObservation normalizedObservation) {
        Feature feature = (Feature) GeoJSONFactory.create(dataLake.getData());

        Point point = reader.read(feature.getGeometry()).getCentroid();
        normalizedObservation.setPoint(makeWktPoint(point.getX(), point.getY()));

        Feature geomFeature = new Feature(feature.getGeometry(), Collections.emptyMap());
        normalizedObservation.setGeometries(new FeatureCollection(new Feature[] {geomFeature}).toString());

        normalizedObservation.setEventSeverity(Severity.UNKNOWN);
        normalizedObservation.setType(EventType.WILDFIRE);

        handlers.get(normalizedObservation.getProvider()).apply(feature.getProperties(), normalizedObservation);
    }

    private void handleSaGov(Map<String, Object> properties, NormalizedObservation normalizedObservation) {
        OffsetDateTime date = parseLocalDate(readString(properties, "FIREDATE"));
        normalizedObservation.setStartedAt(date);
        normalizedObservation.setEndedAt(date);
        normalizedObservation.setName(getName("Wildfire in South Australia", readString(properties, "INCIDENTNA")));
    }

    private void handleQldDesGov(Map<String, Object> properties, NormalizedObservation normalizedObservation) {
        OffsetDateTime startedAt = parseLocalDate(readString(properties, "IGNITIONDA"));
        OffsetDateTime endedAt = parseLocalDate(readString(properties, "OUTDATE"));
        normalizedObservation.setStartedAt(startedAt != null ? startedAt : endedAt);
        normalizedObservation.setEndedAt(endedAt != null ? endedAt : startedAt);
        normalizedObservation.setName(getName("Wildfire in Queensland, Australia", readString(properties, "LABEL")));
    }

    private void handleVictoriaGov(Map<String, Object> properties, NormalizedObservation normalizedObservation) {
        OffsetDateTime date = parseISOBasicDate(readString(properties, "STRTDATIT"));
        normalizedObservation.setStartedAt(date);
        normalizedObservation.setEndedAt(date);
        normalizedObservation.setName(getName("Wildfire in Victoria, Australia", readString(properties, "NAME")));
    }

    private void handleNswGov(Map<String, Object> properties, NormalizedObservation normalizedObservation) {
        OffsetDateTime startedAt = parseLocalDate(readString(properties, "StartDate"));
        OffsetDateTime endedAt = parseLocalDate(readString(properties, "EndDate"));
        normalizedObservation.setStartedAt(startedAt != null ? startedAt : endedAt);
        normalizedObservation.setEndedAt(endedAt != null ? endedAt : startedAt);
        normalizedObservation.setName(getName("Wildfire in New South Wales, Australia", readString(properties, "FireName")));
    }

    private String getName(String name, String atu) {
        return name + (atu != null ? ", " + atu : "");
    }

    interface HandleFunction<Props, Obs> {
        void apply(Props props, Obs obs);
    }
}
