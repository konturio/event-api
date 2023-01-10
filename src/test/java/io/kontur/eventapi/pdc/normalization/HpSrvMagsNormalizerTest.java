package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.FeatureCollection;

import java.io.IOException;
import java.util.UUID;

import static io.kontur.eventapi.pdc.normalization.PdcHazardNormalizer.MAG_PROPERTIES;
import static org.junit.jupiter.api.Assertions.*;

class HpSrvMagsNormalizerTest {

    @Test
    public void testIsApplicable() throws IOException {
        DataLake dataLake = createDataLakeObject();

        assertTrue(new HpSrvMagsNormalizer().isApplicable(dataLake));
    }

    @Test
    public void testNormalization() throws IOException {
        DataLake dataLake = createDataLakeObject();
        dataLake.setExternalId(UUID.randomUUID().toString());

        NormalizedObservation obs = new HpSrvMagsNormalizer().runNormalization(dataLake);

        assertEquals(dataLake.getObservationId(), obs.getObservationId());
        assertEquals(dataLake.getProvider(), obs.getProvider());
        assertEquals(dataLake.getLoadedAt(), obs.getLoadedAt());
        assertEquals("bd6bfd50-a743-4959-88ee-72cf6809ae76", obs.getExternalEventId());
        assertEquals("9b2538fd-2b6b-498d-94a6-896fd55e3ca8", obs.getExternalEpisodeId());
        assertEquals(Severity.UNKNOWN, obs.getEventSeverity());
        assertEquals("Flood - New York--Newark, NY--NJ--CT Region, United States", obs.getName());
        assertNull(obs.getDescription());
        assertNull(obs.getEpisodeDescription());
        assertEquals(EventType.FLOOD, obs.getType());
        assertEquals(1595457720000L, obs.getStartedAt().toInstant().toEpochMilli());
        assertEquals(1595466000000L, obs.getEndedAt().toInstant().toEpochMilli());
        assertEquals(1595458283232L, obs.getSourceUpdatedAt().toInstant().toEpochMilli());
        FeatureCollection fc = obs.getGeometries();
        assertEquals(1, fc.getFeatures().length);
        assertEquals(MAG_PROPERTIES, fc.getFeatures()[0].getProperties());
    }

    private DataLake createDataLakeObject() throws IOException {
        String json = readMessageFromFile("HpSrvMagsNormalizerTest.json");
        return new PdcDataLakeConverter()
                .convertHpSrvMagData(new ObjectMapper().readTree(json), "bd6bfd50-a743-4959-88ee-72cf6809ae76")
                .get(0);
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}