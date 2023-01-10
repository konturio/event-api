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

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.kontur.eventapi.staticdata.normalization.StaticNormalizer.WILDFIRE_PROPERTIES;
import static org.junit.jupiter.api.Assertions.*;

class FrapCalStaticNormalizerIT extends AbstractCleanableIntegrationTest {

    private final FrapCalStaticNormalizer normalizer;

    @Autowired
    public FrapCalStaticNormalizerIT(JdbcTemplate jdbcTemplate, FrapCalStaticNormalizer normalizer, FeedDao feedDao) {
        super(jdbcTemplate, feedDao);
        this.normalizer = normalizer;
    }

    @Test
    public void testIsApplicable() {
        DataLake dataLake = new DataLake();
        dataLake.setProvider("wildfire.frap.cal");
        assertTrue(normalizer.isApplicable(dataLake));
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
        assertEquals(EventType.WILDFIRE, normalizedObservation.getType());
        assertNotNull(normalizedObservation.getPoint());
        assertNotNull(normalizedObservation.getGeometries());
        assertEquals(WILDFIRE_PROPERTIES, normalizedObservation.getGeometries().getFeatures()[0].getProperties());
        assertEquals("Wildfire - Los Angeles County, California, USA", normalizedObservation.getName());
        assertEquals("CalFire", normalizedObservation.getProperName());
        assertEquals(Severity.UNKNOWN, normalizedObservation.getEventSeverity());
        assertEquals(OffsetDateTime.parse("2007-10-21T00:00:00Z"), normalizedObservation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2007-10-23T00:00:00Z"), normalizedObservation.getEndedAt());

    }

    private DataLake createTestDataLake() {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId("123");
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setUpdatedAt(OffsetDateTime.parse("2020-07-12T10:37:00Z"));
        dataLake.setProvider("test-provider");
        dataLake.setData("{\"type\":\"Feature\",\"properties\":{\"YEAR_\":\"2007\",\"STATE\":\"California\", \"FIRE_NAME\":\"CalFire\"," +
                "\"UNIT_ID\":\"Los Angeles County\",\"ALARM_DATE\":\"2007-10-21\",\"CONT_DATE\":\"2007-10-23\"}," +
                "\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":" +
                "[[[[-118.49,34.38],[-118.49,34.38],[-118.49,34.38],[-118.49,34.38],[-118.49,34.38]]]]}}");
        return dataLake;
    }

}