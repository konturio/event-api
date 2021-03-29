package io.kontur.eventapi.tornado.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.util.DateTimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;


import java.io.IOException;
import java.util.UUID;

import static io.kontur.eventapi.tornado.job.StaticTornadoImportJob.*;
import static org.junit.jupiter.api.Assertions.*;

class StaticTornadoNormalizerIT extends AbstractCleanableIntegrationTest {

    private final static String GEOMETRY_FILE = "io/kontur/eventapi/tornado/normalization/static_tornado_geometry.json";
    private final StaticTornadoNormalizer staticTornadoNormalizer;

    @Autowired
    public StaticTornadoNormalizerIT(JdbcTemplate jdbcTemplate, StaticTornadoNormalizer staticTornadoNormalizer) {
        super(jdbcTemplate);
        this.staticTornadoNormalizer = staticTornadoNormalizer;
    }

    @Test
    public void testIsApplicable() {
        DataLake canadaGovDataLake = createDataLakeWithProvider(TORNADO_CANADA_GOV_PROVIDER);
        DataLake australianBmDataLake = createDataLakeWithProvider(TORNADO_AUSTRALIAN_BM_PROVIDER);
        DataLake osmDataLake = createDataLakeWithProvider(TORNADO_OSM_PROVIDER);
        assertTrue(staticTornadoNormalizer.isApplicable(canadaGovDataLake));
        assertTrue(staticTornadoNormalizer.isApplicable(australianBmDataLake));
        assertTrue(staticTornadoNormalizer.isApplicable(osmDataLake));
    }

    @Test
    public void testNormalize() throws IOException {

        DataLake dataLake = createTestDataLake();
        NormalizedObservation normalizedObservation = staticTornadoNormalizer.normalize(dataLake);

        assertEquals(dataLake.getObservationId(), normalizedObservation.getObservationId());
        assertEquals(dataLake.getExternalId(), normalizedObservation.getExternalEventId());
        assertEquals(dataLake.getLoadedAt(), normalizedObservation.getLoadedAt());
        assertEquals(dataLake.getUpdatedAt(), normalizedObservation.getSourceUpdatedAt());
        assertEquals(dataLake.getProvider(), normalizedObservation.getProvider());

        assertNull(normalizedObservation.getCost());
        assertNull(normalizedObservation.getDescription());
        assertNull(normalizedObservation.getEpisodeDescription());
        assertNull(normalizedObservation.getRegion());
        assertNull(normalizedObservation.getSourceUri());
        assertNull(normalizedObservation.getExternalEpisodeId());

        assertNotNull(normalizedObservation.getName());
        assertNotNull(normalizedObservation.getStartedAt());
        assertNotNull(normalizedObservation.getEndedAt());

        assertEquals(normalizedObservation.getStartedAt(), normalizedObservation.getEndedAt());
        assertEquals(Severity.MINOR, normalizedObservation.getEventSeverity());
        assertEquals(EventType.TORNADO, normalizedObservation.getType());
        assertEquals("POINT(-81.012735 43.385625)", normalizedObservation.getPoint());

        Feature feature = (Feature) GeoJSONFactory.create(dataLake.getData());
        String geometry = new FeatureCollection(new Feature[] {feature}).toString();
        assertEquals(geometry, normalizedObservation.getGeometries());


    }

    private DataLake createDataLakeWithProvider(String provider) {
        DataLake dataLake = new DataLake();
        dataLake.setProvider(provider);
        return dataLake;
    }

    private DataLake createTestDataLake() throws IOException {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId("242f183ffdf9177068d4dd482881294b");
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setUpdatedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(TORNADO_CANADA_GOV_PROVIDER);

        String json = new String(getClass().getClassLoader().getResourceAsStream(GEOMETRY_FILE).readAllBytes());
        Feature feature = (Feature) GeoJSONFactory.create(json);
        dataLake.setData(feature.toString());

        return dataLake;
    }
}