package io.kontur.eventapi.staticdata.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.kontur.eventapi.TestUtil.readFile;
import static io.kontur.eventapi.entity.EventType.WILDFIRE;
import static io.kontur.eventapi.entity.Severity.UNKNOWN;
import static io.kontur.eventapi.staticdata.normalization.StaticNormalizer.WILDFIRE_PROPERTIES;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.junit.jupiter.api.Assertions.*;

class AustraliaWildfireNormalizerTest {

    private final AustraliaWildfireNormalizer normalizer = new AustraliaWildfireNormalizer();

    @Test
    public void testNormalize_WildfireSaGov() throws IOException {
        DataLake dataLake = createDataLake("AustraliaWildfireNormalizerTest_sa-gov.json", "wildfire.sa-gov");

        NormalizedObservation observation = normalizer.normalize(dataLake);

        assertEquals(OffsetDateTime.parse("2012-01-01T00:00:00Z"), observation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2012-01-01T00:00:00Z"), observation.getEndedAt());
        assertEquals("Wildfire in South Australia, name", observation.getName());
    }

    @Test
    public void testNormalize_WildfireQldDesGov() throws IOException {
        DataLake dataLake = createDataLake("AustraliaWildfireNormalizerTest_qld-des-gov.json", "wildfire.qld-des-gov");

        NormalizedObservation observation = normalizer.normalize(dataLake);

        assertEquals(OffsetDateTime.parse("2012-01-01T00:00:00Z"), observation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2012-01-05T00:00:00Z"), observation.getEndedAt());
        assertEquals("Wildfire in Queensland, Australia, label", observation.getName());
    }

    @Test
    public void testNormalize_WildfireVictoriaGov() throws IOException {
        DataLake dataLake = createDataLake("AustraliaWildfireNormalizerTest_victoria-gov.json", "wildfire.victoria-gov");

        NormalizedObservation observation = normalizer.normalize(dataLake);

        assertEquals(OffsetDateTime.parse("2012-01-01T00:00:00Z"), observation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2012-01-01T00:00:00Z"), observation.getEndedAt());
        assertEquals("Wildfire in Victoria, Australia, name", observation.getName());
    }

    @Test
    public void testNormalize_WildfireNswGov() throws IOException {
        DataLake dataLake = createDataLake("AustraliaWildfireNormalizerTest_nsw-gov.json", "wildfire.nsw-gov");

        NormalizedObservation observation = normalizer.normalize(dataLake);

        assertEquals(OffsetDateTime.parse("2012-01-01T00:00:00Z"), observation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2012-01-05T00:00:00Z"), observation.getEndedAt());
        assertEquals("Wildfire in New South Wales, Australia, fire name", observation.getName());


    }

    private void assertDefault(DataLake dataLake, NormalizedObservation observation) {
        assertEquals(1, observation.getGeometries().getFeatures().length);
        assertEquals(WILDFIRE_PROPERTIES, observation.getGeometries().getFeatures()[0].getProperties());
        assertEquals(UNKNOWN, observation.getEventSeverity());
        assertEquals(WILDFIRE, observation.getType());

        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getProvider(), observation.getProvider());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getExternalId(), observation.getExternalEventId());
        assertFalse(observation.getActive());

        assertNull(observation.getDescription());
        assertNull(observation.getEpisodeDescription());
        assertNull(observation.getCost());
        assertNull(observation.getRegion());
        assertNull(observation.getUrls());
        assertNull(observation.getExternalEpisodeId());
        assertNull(observation.getProperName());
    }

    private DataLake createDataLake(String filename, String provider) throws IOException {
        String data = readFile(this, filename);
        DataLake dataLake = new DataLake(UUID.randomUUID(), md5Hex(data), OffsetDateTime.now(), OffsetDateTime.now());
        dataLake.setProvider(provider);
        dataLake.setData(data);
        return dataLake;
    }
}