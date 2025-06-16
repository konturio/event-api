package io.kontur.eventapi.jtwc.normalization;

import static org.junit.jupiter.api.Assertions.*;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.jtwc.JtwcUtil;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

import java.util.Objects;
import java.util.UUID;

public class JtwcNormalizerTest {

    @Test
    public void testIsApplicable() {
        DataLake dl = new DataLake(UUID.randomUUID(), "id", DateTimeUtil.uniqueOffsetDateTime(), DateTimeUtil.uniqueOffsetDateTime());
        dl.setProvider(JtwcUtil.JTWC_PROVIDER);
        assertTrue(new JtwcNormalizer().isApplicable(dl));
    }

    @Test
    public void testNormalization() throws Exception {
        String data = IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream("jtwc_norm_test1.txt")), "UTF-8");
        DataLake dl = new DataLake(UUID.randomUUID(), "28W", DateTimeUtil.uniqueOffsetDateTime(), DateTimeUtil.uniqueOffsetDateTime());
        dl.setProvider(JtwcUtil.JTWC_PROVIDER);
        dl.setData(data);

        JtwcNormalizer normalizer = new JtwcNormalizer();
        NormalizedObservation obs = normalizer.normalize(dl);

        assertNotNull(obs);
        assertEquals(dl.getObservationId(), obs.getObservationId());
        assertEquals(JtwcUtil.JTWC_PROVIDER, obs.getProvider());
        assertEquals("28W", obs.getExternalEventId());
        assertEquals("28W_032", obs.getExternalEpisodeId());
        assertEquals("TROPICAL STORM RAI", obs.getName());
        assertEquals(EventType.CYCLONE, obs.getType());
        assertEquals(Severity.SEVERE, obs.getEventSeverity());
        assertNotNull(obs.getStartedAt());
        FeatureCollection geom = obs.getGeometries();
        assertNotNull(geom);
        assertTrue(geom.getFeatures()[0].getGeometry() instanceof Point);
    }
}
