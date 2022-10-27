package io.kontur.eventapi.staticdata.normalization;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.util.DateTimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.kontur.eventapi.staticdata.normalization.StaticNormalizer.TORNADO_PROPERTIES;
import static org.junit.jupiter.api.Assertions.*;

class CommonStaticNormalizerIT extends AbstractCleanableIntegrationTest {

    private final CommonStaticNormalizer normalizer;

    @Autowired
    public CommonStaticNormalizerIT(JdbcTemplate jdbcTemplate, CommonStaticNormalizer normalizer, FeedDao feedDao) {
        super(jdbcTemplate, feedDao);
        this.normalizer = normalizer;
    }

    @Test
    public void testIsApplicable() {
        assertTrue(normalizer.isApplicable(createDataLakeWithProvider("tornado.canada-gov")));
        assertTrue(normalizer.isApplicable(createDataLakeWithProvider("tornado.australian-bm")));
        assertTrue(normalizer.isApplicable(createDataLakeWithProvider("tornado.osm-wiki")));
        assertTrue(normalizer.isApplicable(createDataLakeWithProvider("tornado.des-inventar-sendai")));
    }

    @Test
    public void testNormalize() {
        DataLake dataLake = createTestDataLake();
        NormalizedObservation normalizedObservation = normalizer.normalize(dataLake);

        assertEquals(dataLake.getObservationId(), normalizedObservation.getObservationId());
        assertEquals(dataLake.getExternalId(), normalizedObservation.getExternalEventId());
        assertEquals(dataLake.getLoadedAt(), normalizedObservation.getLoadedAt());
        assertEquals(dataLake.getUpdatedAt(), normalizedObservation.getSourceUpdatedAt());
        assertEquals(dataLake.getProvider(), normalizedObservation.getProvider());
        assertFalse(normalizedObservation.getActive());
        assertEquals("Tornado - Stratford, Canada", normalizedObservation.getName());
        assertEquals(BigDecimal.valueOf(100), normalizedObservation.getCost());
        assertEquals(Severity.MINOR, normalizedObservation.getEventSeverity());
        assertEquals("tornado", normalizedObservation.getDescription());
        assertEquals(EventType.TORNADO, normalizedObservation.getType());
        assertNotNull(normalizedObservation.getPoint());
        assertNotNull(normalizedObservation.getGeometries());

        OffsetDateTime date = OffsetDateTime.parse("1980-05-06T00:00:00Z");
        assertEquals(date, normalizedObservation.getStartedAt());
        assertEquals(date, normalizedObservation.getEndedAt());

        assertEquals(1, normalizedObservation.getGeometries().getFeatures().length);
        assertEquals(TORNADO_PROPERTIES, normalizedObservation.getGeometries().getFeatures()[0].getProperties());
    }

    private DataLake createDataLakeWithProvider(String provider) {
        DataLake dataLake = new DataLake();
        dataLake.setProvider(provider);
        return dataLake;
    }

    private DataLake createTestDataLake() {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId("123");
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setUpdatedAt(OffsetDateTime.parse("2020-07-12T10:37:00Z"));
        dataLake.setProvider("test-provider");
        dataLake.setData("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-81.01,43.38]},\n" +
                "\"properties\":{\"name\":\"\",\"fujita_scale\":\"0\",\"admin0\":\"Canada\",\"damage_property\":\"100\"," +
                "\"date\":\"19800506\",\"nearest_city\":\"Stratford\",\"longitude\":-81.01,\"latitude\":43.38,\"comments\":\"tornado\"}}");
        return dataLake;
    }
}