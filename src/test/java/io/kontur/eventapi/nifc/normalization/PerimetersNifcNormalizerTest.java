package io.kontur.eventapi.nifc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.service.LocationService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.kontur.eventapi.TestUtil.readFile;
import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;
import static io.kontur.eventapi.nifc.normalization.NifcNormalizer.PERIMETERS_PROPERTIES;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static org.junit.jupiter.api.Assertions.*;

class PerimetersNifcNormalizerTest {

    private final PerimetersNifcNormalizer normalizer = new PerimetersNifcNormalizer(mock(LocationService.class));

    @Test
    void testNormalize() throws IOException {
        DataLake dataLake = createDataLake();

        NormalizedObservation observation = normalizer.normalize(dataLake);

        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getEndedAt());
        assertEquals(dataLake.getProvider(), observation.getProvider());
        assertEquals("2021-ALALF-210222", observation.getExternalEventId());
        assertEquals("POINT(-85.82526 33.48939)", observation.getPoint());
        assertEquals(Severity.MINOR, observation.getEventSeverity());
        assertEquals("Wildfire TL DUCK NEST", observation.getName());
        assertEquals("TL DUCK NEST", observation.getProperName());
        assertEquals("9 MILES S/SE OF OXFORD, AL", observation.getDescription());
        assertEquals(EventType.WILDFIRE, observation.getType());
        assertEquals(getDateTimeFromMilli(1637450954000L), observation.getStartedAt());
        assertNull(observation.getEpisodeDescription());
        assertNull(observation.getActive());
        assertNull(observation.getCost());
        assertNotNull(observation.getRegion());
        assertTrue(observation.getUrls().isEmpty());
        assertNull(observation.getExternalEpisodeId());

        assertEquals(PERIMETERS_PROPERTIES, observation.getGeometries().getFeatures()[0].getProperties());
    }

    private DataLake createDataLake() throws IOException {
        String data = readFile(this, "PerimetersNifcNormalizerTest.json");
        String externalId = "2021-ALALF-210222";
        OffsetDateTime updatedAt = getDateTimeFromMilli(1637628408000L);
        DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, OffsetDateTime.now());
        dataLake.setData(data);
        dataLake.setProvider(NIFC_PERIMETERS_PROVIDER);
        return dataLake;
    }

}