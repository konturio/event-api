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
import java.util.UUID;

import static io.kontur.eventapi.TestUtil.readFile;
import static io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter.USGS_EARTHQUAKE_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

class UsgsEarthquakeNormalizerTest {

    private final ShakemapDao shakemapDao = mock(ShakemapDao.class);
    private final UsgsEarthquakeNormalizer normalizer = new UsgsEarthquakeNormalizer(shakemapDao);

    @Test
    void testNormalize() throws IOException {
        DataLake dl = createDataLake();
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

    private DataLake createDataLake() throws IOException {
        String data = readFile(this, "/usgs/sample.json");
        DataLake dl = new DataLake(UUID.randomUUID(), "nc75206757",
                DateTimeUtil.getDateTimeFromMilli(1751906694946L), OffsetDateTime.now());
        dl.setData(data);
        dl.setProvider(USGS_EARTHQUAKE_PROVIDER);
        return dl;
    }
}
