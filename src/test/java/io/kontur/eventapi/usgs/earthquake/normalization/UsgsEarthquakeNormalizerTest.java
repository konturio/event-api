package io.kontur.eventapi.usgs.earthquake.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.util.DateTimeUtil;
import org.junit.jupiter.api.Test;
import io.kontur.eventapi.dao.ShakemapDao;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static io.kontur.eventapi.TestUtil.readFile;
import static io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter.USGS_EARTHQUAKE_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

class UsgsEarthquakeNormalizerTest {

    private final ShakemapDao shakemapDao = mock(ShakemapDao.class);
    private final UsgsEarthquakeNormalizer normalizer = new UsgsEarthquakeNormalizer(shakemapDao);

    @Test
    void testNormalize() throws IOException {
        DataLake dl = createDataLake("/usgs/sample.json");
        NormalizedObservation obs = normalizer.normalize(dl);

        assertEquals(dl.getObservationId(), obs.getObservationId());
        assertEquals(dl.getProvider(), obs.getProvider());
        assertEquals("nc75206757", obs.getExternalEventId());
        assertEquals(Severity.MINOR, obs.getEventSeverity());
        assertEquals(EventType.EARTHQUAKE, obs.getType());
        assertEquals("M 1.6 - 2 km E of Aromas, CA", obs.getName());
        assertEquals("2 km E of Aromas, CA", obs.getDescription());
        assertEquals(dl.getUpdatedAt(), obs.getEndedAt());
        assertEquals(dl.getLoadedAt(), obs.getLoadedAt());
        assertTrue(obs.getUrls().contains("https://earthquake.usgs.gov/earthquakes/eventpage/nc75206757"));
        assertEquals(1, obs.getGeometries().getFeatures().length);
    }

    @Test
    void testNormalizeWithShakemap() throws Exception {
        when(shakemapDao.buildShakemapPolygons(any())).thenReturn("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[1,1]},\"properties\":{}}]}");

        DataLake dl = createDataLake("/usgs/sample_with_shakemap.json");
        NormalizedObservation obs = normalizer.normalize(dl);

        verify(shakemapDao).buildShakemapPolygons(any());
        assertEquals(2, obs.getGeometries().getFeatures().length);
    }

    @Test
    void testNormalizeWithPgaMask() throws Exception {
        when(shakemapDao.buildPgaMask(any())).thenReturn("{\"type\":\"Polygon\"}");

        DataLake dl = createDataLake("/usgs/sample_with_pga.json");
        NormalizedObservation obs = normalizer.normalize(dl);

        verify(shakemapDao).buildPgaMask(any());
        assertEquals(Map.of("type", "Polygon"), obs.getSeverityData().get("pga40Mask"));

        Object cov = obs.getSeverityData().get("coverage_pga_high_res");
        assertTrue(cov instanceof Map);
        assertEquals("Coverage", ((Map<?, ?>) cov).get("type"));
    }

    private DataLake createDataLake(String file) throws IOException {
        String data = readFile(this, file);
        DataLake dl = new DataLake(UUID.randomUUID(), "nc75206757",
                DateTimeUtil.getDateTimeFromMilli(1751906694946L), OffsetDateTime.now());
        dl.setData(data);
        dl.setProvider(USGS_EARTHQUAKE_PROVIDER);
        return dl;
    }
}
