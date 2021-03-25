package io.kontur.eventapi.tornado.normalization;

import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import static io.kontur.eventapi.tornado.job.StaticTornadoImportJob.*;
import static io.kontur.eventapi.tornado.service.TornadoService.parseDateWithFormatter;

@Component
public class StaticTornadoNormalizer extends TornadoNormalizer {

    private final Map<String, String> COUNTRY_NAMES = Map.of(TORNADO_CANADA_GOV_PROVIDER, "Canada");

    @Override
    protected void setDataFields(String data, NormalizedObservation normalizedObservation) {
        Feature feature = (Feature) GeoJSONFactory.create(data);
        Map<String, Object> properties = feature.getProperties();

        normalizedObservation.setDescription(readString(properties, "comments"));

        DateTimeFormatter formatter = FORMATTERS.get(normalizedObservation.getProvider());
        OffsetDateTime date = parseDateWithFormatter(readString(properties, "date"), formatter);
        normalizedObservation.setStartedAt(date);
        normalizedObservation.setEndedAt(date);

        Severity severity = convertSeverity(readString(properties, "fujita_scale"));
        normalizedObservation.setEventSeverity(severity);

        BigDecimal cost = parseCost(readString(properties, "damage_property"));
        normalizedObservation.setCost(cost);

        String name = readString(properties, "name");
        String nearestCity = readString(properties, "nearest_city");
        String admin0 = readString(properties, "admin0");
        String country = COUNTRY_NAMES.getOrDefault(normalizedObservation.getProvider(), StringUtils.EMPTY);
        normalizedObservation.setName(StringUtils.isBlank(name) ? createName(nearestCity, admin0, country) : name);
    }

    @Override
    protected void setGeometry(String data, NormalizedObservation normalizedObservation) {
        Feature feature = (Feature) GeoJSONFactory.create(data);
        Map<String, Object> properties = feature.getProperties();

        Double latitude = parseDouble(readString(properties, "latitude"));
        Double longitude = parseDouble(readString(properties, "longitude"));

        normalizedObservation.setPoint(makeWktPoint(longitude, latitude));
        normalizedObservation.setGeometries(new FeatureCollection(new Feature[] {feature}).toString());
    }


    @Override
    protected List<String> getProviders() {
        return List.of(TORNADO_CANADA_GOV_PROVIDER, TORNADO_AUSTRALIAN_BM_PROVIDER, TORNADO_OSM_PROVIDER);
    }

    private BigDecimal parseCost(String costString) {
        return NumberUtils.isParsable(costString) ? NumberUtils.createBigDecimal(costString) : null;
    }
}
