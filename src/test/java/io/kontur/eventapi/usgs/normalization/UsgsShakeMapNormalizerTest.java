package io.kontur.eventapi.usgs.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.kontur.eventapi.usgs.converter.UsgsShakeMapDataLakeConverter.USGS_SHAKEMAP_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

class UsgsShakeMapNormalizerTest {

    private final UsgsShakeMapNormalizer normalizer = new UsgsShakeMapNormalizer();

    @Test
    void testNormalize() throws IOException {
        DataLake dl = createDataLake();
        NormalizedObservation o = normalizer.normalize(dl);

        assertEquals(dl.getObservationId(), o.getObservationId());
        assertEquals(EventType.EARTHQUAKE, o.getType());
        assertEquals("test1", o.getExternalEventId());
        assertEquals("M 5.5 - Test Place", o.getName());
        assertEquals(Severity.MODERATE, o.getEventSeverity());
        assertEquals(5.5, o.getSeverityData().get("magnitude"));
        assertEquals(5.0, o.getSeverityData().get("depthKm"));
        assertNotNull(o.getGeometries());
    }

    private DataLake createDataLake() throws IOException {
        String data = IOUtils.toString(this.getClass().getResourceAsStream("UsgsShakeMapNormalizerTest.json"), StandardCharsets.UTF_8);
        DataLake dl = new DataLake(UUID.randomUUID(), "test1", OffsetDateTime.now(), OffsetDateTime.now());
        dl.setProvider(USGS_SHAKEMAP_PROVIDER);
        dl.setData(data);
        return dl;
    }
}
