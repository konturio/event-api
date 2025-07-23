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
import java.util.HashMap;
import com.fasterxml.jackson.core.type.TypeReference;
import io.kontur.eventapi.util.JsonUtil;
import org.wololo.geojson.Feature;

import static io.kontur.eventapi.TestUtil.readFile;
import static io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter.USGS_EARTHQUAKE_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

class UsgsEarthquakeNormalizerTest {

    private final ShakemapDao shakemapDao = mock(ShakemapDao.class);
    private final UsgsEarthquakeNormalizer normalizer = new UsgsEarthquakeNormalizer(shakemapDao);

    @Test
    void testNormalize() throws IOException {
        when(shakemapDao.buildCentroidBuffer(anyDouble(), anyDouble())).thenReturn("{\"type\":\"Polygon\"}");
        DataLake dl = createDataLake("/usgs/sample.json");
        NormalizedObservation obs = normalizer.normalize(dl);

        assertEquals(dl.getObservationId(), obs.getObservationId());
        assertEquals(dl.getProvider(), obs.getProvider());
        assertEquals("nc75206757", obs.getExternalEventId());
        assertEquals(Severity.MINOR, obs.getEventSeverity());
        assertEquals(EventType.EARTHQUAKE, obs.getType());
        assertEquals("M 1.6 - 2 km E of Aromas, CA", obs.getName());
        assertNull(obs.getProperName());
        assertEquals("2 km E of Aromas, CA", obs.getDescription());
        assertEquals("CA", obs.getRegion());
        String descr = "On 7/7/2025 4:43:17 PM, an earthquake occurred 2 km E of Aromas, CA. The earthquake had Magnitude 1.6M, Depth:0.45km.";
        assertEquals(descr, obs.getEpisodeDescription());
        assertEquals(dl.getUpdatedAt(), obs.getEndedAt());
        assertEquals(dl.getLoadedAt(), obs.getLoadedAt());
        assertTrue(obs.getUrls().contains("https://earthquake.usgs.gov/earthquakes/eventpage/nc75206757"));
        assertEquals(2, obs.getGeometries().getFeatures().length);
        Feature circle = obs.getGeometries().getFeatures()[1];
        assertEquals("Polygon", circle.getGeometry().getType());
    }

    @Test
    void testNormalizeWithShakemap() throws Exception {
        when(shakemapDao.buildShakemapPolygons(any())).thenReturn(
                "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[1,1]},\"properties\":{\"value\":2.5}}]}");
        when(shakemapDao.buildCentroidBuffer(anyDouble(), anyDouble())).thenReturn("{\"type\":\"Polygon\"}");

        DataLake dl = createDataLake("/usgs/sample.json");
        NormalizedObservation obs = normalizer.normalize(dl);

        verify(shakemapDao).buildShakemapPolygons(any());
        assertEquals(3, obs.getGeometries().getFeatures().length);

        Feature polygon = obs.getGeometries().getFeatures()[0];
        Map<String, Object> props = polygon.getProperties();
        assertEquals("Poly_SMPInt_2.5", props.get("Class"));
        assertEquals(dl.getExternalId(), props.get("eventid"));
        assertEquals("EQ", props.get("eventtype"));
        assertEquals("Intensity 2.5", props.get("polygonlabel"));
        assertEquals(Severity.MINOR, obs.getEventSeverity());
    }

    @Test
    void testNormalizeWithPgaMask() throws Exception {
        when(shakemapDao.buildPgaMask(any())).thenReturn("{\"type\":\"Polygon\"}");
        when(shakemapDao.buildCentroidBuffer(anyDouble(), anyDouble())).thenReturn("{\"type\":\"Polygon\"}");

        DataLake dl = createDataLake("/usgs/sample.json");
        NormalizedObservation obs = normalizer.normalize(dl);

        verify(shakemapDao).buildPgaMask(any());
        assertEquals(Map.of("type", "Polygon"), obs.getSeverityData().get("pga40Mask"));
        assertEquals(2, obs.getGeometries().getFeatures().length);

        Object cov = obs.getSeverityData().get("coverage_pga_highres");
        assertTrue(cov instanceof Map);
        assertEquals("Coverage", ((Map<?, ?>) cov).get("type"));
        assertEquals(Severity.SEVERE, obs.getEventSeverity());
    }

    @Test
    void testMagnitudeUpgrade() throws Exception {
        when(shakemapDao.buildCentroidBuffer(anyDouble(), anyDouble())).thenReturn("{\"type\":\"Polygon\"}");

        DataLake dl = createDataLake("/usgs/sample.json", 7.6);
        NormalizedObservation obs = normalizer.normalize(dl);

        assertEquals(Severity.SEVERE, obs.getEventSeverity());
    }

    private DataLake createDataLake(String file) throws IOException {
        return createDataLake(file, null);
    }

    private DataLake createDataLake(String file, Double magOverride) throws IOException {
        String data = readFile(this, file);
        if (magOverride != null) {
            Map<String, Object> feature = JsonUtil.readJson(data, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> props = (Map<String, Object>) feature.get("properties");
            if (props == null) {
                props = new HashMap<>();
                feature.put("properties", props);
            }
            props.put("mag", magOverride);
            data = JsonUtil.writeJson(feature);
        }
        DataLake dl = new DataLake(UUID.randomUUID(), "nc75206757",
                DateTimeUtil.getDateTimeFromMilli(1751906694946L), OffsetDateTime.now());
        dl.setData(data);
        dl.setProvider(USGS_EARTHQUAKE_PROVIDER);
        return dl;
    }
}
