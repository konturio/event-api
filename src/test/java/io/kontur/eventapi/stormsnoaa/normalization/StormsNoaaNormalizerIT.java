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
        assertTrue(normalizer.isApplicable(dataLake),
                "StormsNoaaNormalizer should be applicable for provider 'storms.noaa'");
    }

    @Test
    public void testNormalize() {
        DataLake dataLake = createTestDataLake();
        NormalizedObservation normalizedObservation = normalizer.normalize(dataLake);

        assertEquals(dataLake.getObservationId(), normalizedObservation.getObservationId(),
                "Observation ID should match the original DataLake observation ID");
        assertEquals(dataLake.getLoadedAt(), normalizedObservation.getLoadedAt(),
                "Loaded timestamp should be carried over from DataLake");
        assertEquals(dataLake.getUpdatedAt(), normalizedObservation.getSourceUpdatedAt(),
                "Source update time should come from DataLake updatedAt");
        assertEquals(dataLake.getProvider(), normalizedObservation.getProvider(),
                "Provider should remain unchanged after normalization");
        assertEquals("7", normalizedObservation.getExternalEpisodeId(),
                "Episode ID should be taken from the EPISODE_ID column");
        assertEquals(dataLake.getExternalId(), normalizedObservation.getExternalEventId(),
                "Event ID should match DataLake externalId sourced from EVENT_ID column");
        assertFalse(normalizedObservation.getActive(),
                "Newly normalized observations from Storms NOAA should be inactive by default");
        assertEquals(EventType.TORNADO, normalizedObservation.getType(),
                "EVENT_TYPE 'Tornado' should map to EventType.TORNADO");
        assertNull(normalizedObservation.getEpisodeDescription(),
                "EPISODE_NARRATIVE is empty in the CSV and should result in null");
        assertNull(normalizedObservation.getDescription(),
                "EVENT_NARRATIVE is empty in the CSV and should result in null");
        assertEquals(BigDecimal.valueOf(250000), normalizedObservation.getCost(),
                "Property damage should equal 250K parsed from DAMAGE_PROPERTY");
        assertEquals(OffsetDateTime.parse("1950-04-28T13:20:00Z"), normalizedObservation.getStartedAt(),
                "BEGIN_DATE_TIME should be converted to startedAt");
        assertEquals(OffsetDateTime.parse("1950-04-29T14:45:00Z"), normalizedObservation.getEndedAt(),
                "END_DATE_TIME should be converted to endedAt");
        assertEquals(Severity.SEVERE, normalizedObservation.getEventSeverity(),
                "Fujita scale F3 should translate to SEVERE severity");
        assertEquals("Tornado - WASHITA, OKLAHOMA, USA", normalizedObservation.getName(),
                "Name should combine event type and location");
        assertNotNull(normalizedObservation.getGeometries(),
                "Geometry collection should be created from provided coordinates");
        assertEquals(1, normalizedObservation.getGeometries().getFeatures().length,
                "Track geometry should contain exactly one feature");
        assertEquals(STORMS_NOAA_TRACK_PROPERTIES, normalizedObservation.getGeometries().getFeatures()[0].getProperties(),
                "Feature properties should match STORMS_NOAA_TRACK_PROPERTIES");
    }

    private DataLake createTestDataLake() {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId("10096222");
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setUpdatedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider("test-provider");
        dataLake.setData("EVENT_TYPE,EPISODE_ID,EVENT_ID,STATE,CZ_NAME,BEGIN_DATE_TIME,END_DATE_TIME,DAMAGE_PROPERTY,TOR_F_SCALE,BEGIN_LAT,BEGIN_LON,END_LAT,END_LON,EPISODE_NARRATIVE,EVENT_NARRATIVE\n" +
                "Tornado,7,10096222,OKLAHOMA,WASHITA,28-APR-50 13:20:00,29-APR-50 14:45:00,250K,F3,35.12,-99.2,35.17,-99.2,,");
        return dataLake;
    }
}