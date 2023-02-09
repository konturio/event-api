package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;
import static io.kontur.eventapi.pdc.normalization.PdcHazardNormalizer.EXPOSURE_PROPERTIES;
import static org.junit.jupiter.api.Assertions.*;

class PdcMapSrvNormalizerTest {

    private static final String HAZARD_ID = "e069a69d-2907-44ae-8d73-d506634fa171";
    private static final OffsetDateTime LOADED_AT = OffsetDateTime.parse("2021-05-20T10:15:30Z");

    @Test
    public void testIsApplicable() throws IOException {
        assertTrue(new PdcMapSrvNormalizer().isApplicable(generateDataLake()));
    }

    @Test
    public void testNormalize() throws IOException {
        DataLake dataLake = generateDataLake();
        NormalizedObservation observation = new PdcMapSrvNormalizer().normalize(dataLake);

        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getExternalId(), observation.getExternalEventId());
        assertEquals(dataLake.getProvider(), observation.getProvider());
        assertNotNull(observation.getPoint());
        assertEquals(getGeometries(), observation.getGeometries().getFeatures()[0].getGeometry().toString());
        assertEquals(EXPOSURE_PROPERTIES, observation.getGeometries().getFeatures()[0].getProperties());
        assertEquals(Severity.UNKNOWN, observation.getEventSeverity());
        assertNull(observation.getName());
        assertNull(observation.getDescription());
        assertNull(observation.getEpisodeDescription());
        assertEquals(EventType.CYCLONE, observation.getType());
        assertTrue(observation.getActive());
        assertNull(observation.getCost());
        assertNull(observation.getRegion());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertNull(observation.getStartedAt());
        assertNull(observation.getEndedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertTrue(observation.getUrls().isEmpty());
        assertNull(observation.getExternalEpisodeId());
    }

    private DataLake generateDataLake() throws IOException {
        String json = readMessageFromFile("PdcMapSrvNormalizerTest.json");
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setLoadedAt(LOADED_AT);
        dataLake.setUpdatedAt(LOADED_AT);
        dataLake.setProvider(PDC_MAP_SRV_PROVIDER);
        dataLake.setExternalId(HAZARD_ID);
        dataLake.setData(json);
        return dataLake;
    }

    private String getGeometries() throws IOException {
        return readMessageFromFile("PdcMapSrvNormalizerTest.Geometries.json");
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }
}