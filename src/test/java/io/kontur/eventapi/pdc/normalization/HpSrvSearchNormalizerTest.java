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
import org.wololo.geojson.Point;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HpSrvSearchNormalizerTest {

    @Test
    public void testIsApplicable() throws IOException {
        DataLake dataLake = createDataLakeObject();

        assertTrue(new HpSrvSearchNormalizer().isApplicable(dataLake));
    }

    @Test
    public void testNormalization() throws IOException {
        //given
        DataLake dataLake = createDataLakeObject();
        dataLake.setExternalId(UUID.randomUUID().toString());

        //when
        NormalizedObservation obs = new HpSrvSearchNormalizer().normalize(dataLake);

        //then
        assertEquals(dataLake.getObservationId(), obs.getObservationId());
        assertEquals(dataLake.getProvider(), obs.getProvider());
        assertEquals("d26f0681-70e2-48b2-83eb-c8b9d8ef69fe", obs.getExternalEventId());
        assertEquals("d26f0681-70e2-48b2-83eb-c8b9d8ef69fe", obs.getExternalEpisodeId());
        assertEquals(Severity.EXTREME, obs.getEventSeverity());
        assertEquals("Flood - Huron, SD Region, United States", obs.getName());
        assertEquals("The National Weather Service (NWS) ...", obs.getDescription());
        assertEquals(EventType.FLOOD, obs.getType());
        assertEquals(1590590340000L, obs.getStartedAt().toInstant().toEpochMilli());
        assertEquals(1590675541253L, obs.getEndedAt().toInstant().toEpochMilli());
        assertEquals(1590590813468L, obs.getSourceUpdatedAt().toInstant().toEpochMilli());
        assertEquals(dataLake.getLoadedAt(), obs.getLoadedAt());

        assertNotNull(obs.getGeometries());
        checkGeometriesValue(obs.getGeometries());
    }

    private void checkGeometriesValue(FeatureCollection fc) {
        assertEquals(1, fc.getFeatures().length);
        var feature = fc.getFeatures()[0];
        assertTrue(feature.getGeometry() instanceof Point);
        var point = (Point) feature.getGeometry();
        assertEquals(-98.1841, point.getCoordinates()[0]);
        assertEquals(44.4186, point.getCoordinates()[1]);

        assertEquals("centerPoint", feature.getProperties().get("areaType"));
    }

    private DataLake createDataLakeObject() throws IOException {
        String json = readMessageFromFile("HpSrvSearchNormalizerTest.json");
        return new PdcDataLakeConverter().convertHpSrvHazardData(new ObjectMapper().readTree(json));
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}