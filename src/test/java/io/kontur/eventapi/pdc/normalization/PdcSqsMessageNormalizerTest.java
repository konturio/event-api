package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static io.kontur.eventapi.entity.EventType.*;
import static io.kontur.eventapi.entity.Severity.EXTREME;
import static io.kontur.eventapi.entity.Severity.MINOR;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;
import static io.kontur.eventapi.pdc.normalization.PdcHazardNormalizer.*;
import static org.junit.jupiter.api.Assertions.*;

class PdcSqsMessageNormalizerTest {

    private final PdcSqsMessageNormalizer normalizer = new PdcSqsMessageNormalizer();
    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    @Test
    public void testNormalize_Hazard() throws IOException, ParseException {
        String filename = "PdcSqsMessageNormalizerTest_HAZARD.json";
        DataLake dataLake = createDataLake(filename);

        NormalizedObservation observation = normalizer.runNormalization(dataLake);

        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getProvider(), observation.getProvider());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());

        assertEquals("POINT(10.0 10.0)", observation.getPoint());

        assertEquals(1, observation.getGeometries().getFeatures().length);
        assertEquals(geoJSONWriter.write(wktReader.read("POINT(10.0 10.0)")).toString(),
                observation.getGeometries().getFeatures()[0].getGeometry().toString());
        assertEquals(HAZARD_PROPERTIES, observation.getGeometries().getFeatures()[0].getProperties());

        assertEquals("2", observation.getExternalEventId());
        assertEquals("2", observation.getExternalEpisodeId());
        assertEquals(MINOR, observation.getEventSeverity());
        assertEquals(WILDFIRE, observation.getType());
        assertEquals("hazard name", observation.getName());
        assertEquals("hazard description", observation.getDescription());
        assertEquals("hazard description", observation.getEpisodeDescription());
        assertEquals(OffsetDateTime.parse("2020-01-01T00:00:00.000Z"), observation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2020-01-01T01:00:00.000Z"), observation.getEndedAt());
        assertEquals(OffsetDateTime.parse("2020-01-01T01:00:00.000Z"), observation.getSourceUpdatedAt());

        assertNull(observation.getActive());
        assertTrue(observation.getUrls().isEmpty());
        assertNull(observation.getCost());
        assertNull(observation.getRegion());
        assertNull(observation.getProperName());
        assertNull(observation.getNormalizedAt());
        assertFalse(observation.getRecombined());
    }

    @Test
    public void testNormalize_HazardCyclone() throws IOException, ParseException {
        String filename = "PdcSqsMessageNormalizerTest_HAZARD_CYCLONE.json";
        DataLake dataLake = createDataLake(filename);

        NormalizedObservation observation = normalizer.runNormalization(dataLake);

        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getProvider(), observation.getProvider());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());

        assertEquals("POINT(10.0 10.0)", observation.getPoint());

        assertEquals(1, observation.getGeometries().getFeatures().length);
        assertEquals(geoJSONWriter.write(wktReader.read("POINT(10.0 10.0)")).toString(),
                observation.getGeometries().getFeatures()[0].getGeometry().toString());
        assertEquals(SQS_CYCLONE_PROPERTIES, observation.getGeometries().getFeatures()[0].getProperties());

        assertEquals("2", observation.getExternalEventId());
        assertEquals("2", observation.getExternalEpisodeId());
        assertEquals(MINOR, observation.getEventSeverity());
        assertEquals(CYCLONE, observation.getType());
        assertEquals("hazard name", observation.getName());
        assertEquals("hazard description", observation.getDescription());
        assertEquals("hazard description", observation.getEpisodeDescription());
        assertEquals(OffsetDateTime.parse("2020-01-01T00:00:00.000Z"), observation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2020-01-01T01:00:00.000Z"), observation.getEndedAt());
        assertEquals(OffsetDateTime.parse("2020-01-01T01:00:00.000Z"), observation.getSourceUpdatedAt());

        assertNull(observation.getActive());
        assertTrue(observation.getUrls().isEmpty());
        assertNull(observation.getCost());
        assertNull(observation.getRegion());
        assertNull(observation.getProperName());
        assertNull(observation.getNormalizedAt());
        assertFalse(observation.getRecombined());
    }

    @Test
    public void testNormalize_Mag() throws IOException, ParseException {
        String filename = "PdcSqsMessageNormalizerTest_MAG.json";
        DataLake dataLake = createDataLake(filename);

        NormalizedObservation observation = normalizer.runNormalization(dataLake);

        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getProvider(), observation.getProvider());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());

        assertEquals("POINT(10.0 10.0)", observation.getPoint());

        assertEquals(1, observation.getGeometries().getFeatures().length);
        assertEquals(geoJSONWriter.write(wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))")).toString(),
                observation.getGeometries().getFeatures()[0].getGeometry().toString());
        assertEquals(MAG_PROPERTIES, observation.getGeometries().getFeatures()[0].getProperties());

        assertEquals("3", observation.getExternalEventId());
        assertEquals("1", observation.getExternalEpisodeId());
        assertEquals(EXTREME, observation.getEventSeverity());
        assertEquals(FLOOD, observation.getType());
        assertEquals("hazard name", observation.getName());
        assertEquals("hazard description", observation.getDescription());
        assertEquals("hazard description", observation.getEpisodeDescription());
        assertEquals(OffsetDateTime.parse("2020-01-01T00:00:00.000Z"), observation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2020-01-01T01:00:00.000Z"), observation.getEndedAt());
        assertEquals(OffsetDateTime.parse("2020-01-01T01:00:00.000Z"), observation.getSourceUpdatedAt());
        assertEquals(observation.getUrls(), List.of("snc url"));
        assertTrue(observation.getActive());

        assertNull(observation.getCost());
        assertNull(observation.getRegion());
        assertNull(observation.getProperName());
        assertNull(observation.getNormalizedAt());
        assertFalse(observation.getRecombined());
    }

    private DataLake createDataLake(String filename) throws IOException {
        String json = readMessageFromFile(filename);
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setProvider(PDC_SQS_PROVIDER);
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setData(json);
        dataLake.setExternalId("1");
        return dataLake;
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }
}