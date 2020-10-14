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

import static io.kontur.eventapi.util.JsonUtil.readJson;
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

        NormalizedObservation obs = new HpSrvMagsNormalizer().normalize(dataLake);

        assertEquals(dataLake.getObservationId(), obs.getObservationId());
        assertEquals(dataLake.getProvider(), obs.getProvider());
        assertEquals(dataLake.getLoadedAt(), obs.getLoadedAt());
        assertEquals("bd6bfd50-a743-4959-88ee-72cf6809ae76", obs.getExternalEventId());
        assertEquals(Severity.UNKNOWN, obs.getEventSeverity());
        assertEquals("Flood - New York--Newark, NY--NJ--CT Region, United States", obs.getName());
        assertNull(obs.getDescription());
        assertEquals(
                "Automated Flood SmartAlert Area [ 2020-07-22 22:51:20 GMT ] | FLOODIPAWS-WARNING-2020-New York--Newark, NY--NJ--CT",
                obs.getEpisodeDescription());
        assertEquals(EventType.FLOOD, obs.getType());
        assertEquals(1595457720000L, obs.getStartedAt().toInstant().toEpochMilli());
        assertEquals(1595466000000L, obs.getEndedAt().toInstant().toEpochMilli());
        assertEquals(1595458283232L, obs.getSourceUpdatedAt().toInstant().toEpochMilli());
        FeatureCollection fc = readJson(obs.getGeometries(), FeatureCollection.class);
        assertEquals(1, fc.getFeatures().length);
        assertEquals(
                "Automated Flood SmartAlert Area [ 2020-07-22 22:51:20 GMT ] | FLOODIPAWS-WARNING-2020-New York--Newark, NY--NJ--CT",
                fc.getFeatures()[0].getProperties().get("description"));
        assertEquals("2020-07-22T22:51:23.232Z", fc.getFeatures()[0].getProperties().get("updatedAt"));
        assertEquals(false, fc.getFeatures()[0].getProperties().get("active"));
    }

    private DataLake createDataLakeObject() throws IOException {
        String json = readMessageFromFile("HpSrvMagsNormalizerTest.json");
        return new PdcDataLakeConverter()
                .convertHpSrvMagData(new ObjectMapper().readTree(json), "bd6bfd50-a743-4959-88ee-72cf6809ae76");
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}