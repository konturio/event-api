package io.kontur.eventapi.nifc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.kontur.eventapi.TestUtil.readFile;
import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.nifc.normalization.NifcNormalizer.LOCATIONS_PROPERTIES;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static org.junit.jupiter.api.Assertions.*;

class LocationsNifcNormalizerTest {

    private final LocationsNifcNormalizer normalizer = new LocationsNifcNormalizer();

    @Test
    void testNormalize() throws IOException {
        DataLake dataLake = createDataLake();

        NormalizedObservation observation = normalizer.normalize(dataLake).get();

        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getEndedAt());
        assertEquals(dataLake.getProvider(), observation.getProvider());
        assertEquals("2021-IDIPF-000504", observation.getExternalEventId());
        assertEquals("POINT(-115.354225 47.128143)", observation.getPoint());
        assertEquals(Severity.MINOR, observation.getEventSeverity());
        assertEquals("Wildfire Stateline Complex", observation.getName());
        assertEquals("Stateline Complex", observation.getProperName());
        assertEquals("6.5 miles SW of St. Regis, MT", observation.getDescription());
        assertEquals(EventType.WILDFIRE, observation.getType());
        assertEquals(getDateTimeFromMilli(1626488389000L), observation.getStartedAt());
        assertNull(observation.getEpisodeDescription());
        assertNull(observation.getActive());
        assertNull(observation.getCost());
        assertNull(observation.getRegion());
        assertTrue(observation.getUrls().isEmpty());
        assertNull(observation.getExternalEpisodeId());

        assertEquals(LOCATIONS_PROPERTIES, observation.getGeometries().getFeatures()[0].getProperties());
    }

    private DataLake createDataLake() throws IOException {
        String data = readFile(this, "LocationsNifcNormalizerTest.json");
        String externalId = "2021-IDIPF-000504";
        OffsetDateTime updatedAt = getDateTimeFromMilli(1636581972000L);
        DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, OffsetDateTime.now());
        dataLake.setData(data);
        dataLake.setProvider(NIFC_LOCATIONS_PROVIDER);
        return dataLake;
    }
}