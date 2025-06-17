package io.kontur.eventapi.stormsnoaa.normalization;

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

import static io.kontur.eventapi.stormsnoaa.normalization.StormsNoaaNormalizer.STORMS_NOAA_TRACK_PROPERTIES;
import static org.junit.jupiter.api.Assertions.*;

class StormsNoaaNormalizerIT extends AbstractCleanableIntegrationTest {

    private final StormsNoaaNormalizer normalizer;

    @Autowired
    public StormsNoaaNormalizerIT(JdbcTemplate jdbcTemplate, StormsNoaaNormalizer normalizer, FeedDao feedDao) {
        super(jdbcTemplate, feedDao);
        this.normalizer = normalizer;
    }

    @Test
    public void testIsApplicable() {
        DataLake dataLake = new DataLake();
        dataLake.setProvider("storms.noaa");
        assertTrue(normalizer.isApplicable(dataLake));
    }

    @Test
    public void testNormalize() {
        DataLake dataLake = createTestDataLake();
        NormalizedObservation normalizedObservation = normalizer.normalize(dataLake);

        assertEquals(dataLake.getObservationId(), normalizedObservation.getObservationId());
        assertEquals(dataLake.getLoadedAt(), normalizedObservation.getLoadedAt());
        assertEquals(dataLake.getUpdatedAt(), normalizedObservation.getSourceUpdatedAt());
        assertEquals(dataLake.getProvider(), normalizedObservation.getProvider());
        assertEquals(dataLake.getExternalId(), normalizedObservation.getExternalEpisodeId());
        assertFalse(normalizedObservation.getActive());
        assertEquals(EventType.TORNADO, normalizedObservation.getType());
        assertNull(normalizedObservation.getExternalEventId());
        assertNull(normalizedObservation.getEpisodeDescription());
        assertNull(normalizedObservation.getDescription());
        assertEquals(BigDecimal.valueOf(250000), normalizedObservation.getCost());
        assertEquals(OffsetDateTime.parse("1950-04-28T13:20:00Z"), normalizedObservation.getStartedAt());
        assertEquals(OffsetDateTime.parse("1950-04-29T14:45:00Z"), normalizedObservation.getEndedAt());
        assertEquals(Severity.SEVERE, normalizedObservation.getEventSeverity());
        assertEquals("Tornado - WASHITA, OKLAHOMA, USA", normalizedObservation.getName());
        assertNotNull(normalizedObservation.getGeometries());
        assertEquals(1, normalizedObservation.getGeometries().getFeatures().length);
        assertEquals(STORMS_NOAA_TRACK_PROPERTIES, normalizedObservation.getGeometries().getFeatures()[0].getProperties());
    }

    private DataLake createTestDataLake() {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId("10096222");
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setUpdatedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider("test-provider");
        dataLake.setData("EVENT_TYPE,EPISODE_ID,EVENT_ID,STATE,CZ_NAME,BEGIN_DATE_TIME,END_DATE_TIME,DAMAGE_PROPERTY,TOR_F_SCALE,BEGIN_LAT,BEGIN_LON,END_LAT,END_LON,EPISODE_NARRATIVE,EVENT_NARRATIVE\n" +
                "Tornado,,10096222,OKLAHOMA,WASHITA,28-APR-50 13:20:00,29-APR-50 14:45:00,250K,F3,35.12,-99.2,35.17,-99.2,,");
        return dataLake;
    }
}