package io.kontur.eventapi.staticdata.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.util.SeverityUtil.convertFujitaScale;

@Component
public class CommonStaticNormalizer extends StaticNormalizer {

    @Override
    protected List<String> getProviders() {
        return List.of("tornado.canada-gov", "tornado.australian-bm", "tornado.osm-wiki", "tornado.des-inventar-sendai");
    }

    @Override
    protected void setExtraFields(DataLake dataLake, NormalizedObservation normalizedObservation) {
        Feature feature = (Feature) GeoJSONFactory.create(dataLake.getData());
        Map<String, Object> properties = feature.getProperties();

        Double lon = readDouble(properties, "longitude");
        Double lat = readDouble(properties, "latitude");
        normalizedObservation.setPoint(makeWktPoint(lon, lat));
        Feature geomFeature = new Feature(feature.getGeometry(), Collections.emptyMap());
        normalizedObservation.setGeometries(new FeatureCollection(new Feature[] {geomFeature}).toString());

        String name = readString(properties, "name");
        String admin0 = readString(properties, "admin0");
        String nearestCity = readString(properties, "nearest_city");
        normalizedObservation.setName(name.isEmpty() ? createName("Tornado", nearestCity, admin0) : name);

        OffsetDateTime date = parseISOBasicDate(readString(properties, "date"));
        normalizedObservation.setStartedAt(date);
        normalizedObservation.setEndedAt(date);

        normalizedObservation.setCost(NumberUtils.createBigDecimal(readString(properties, "damage_property")));
        normalizedObservation.setEventSeverity(convertFujitaScale(readString(properties, "fujita_scale")));
        normalizedObservation.setDescription(readString(properties, "comments"));
        normalizedObservation.setType(EventType.TORNADO);
    }

}
