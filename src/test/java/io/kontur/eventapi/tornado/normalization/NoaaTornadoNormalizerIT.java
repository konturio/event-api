package io.kontur.eventapi.tornado.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.util.DateTimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static io.kontur.eventapi.tornado.job.NoaaTornadoImportJob.TORNADO_NOAA_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;
import static org.junit.jupiter.api.Assertions.*;

class NoaaTornadoNormalizerIT extends AbstractCleanableIntegrationTest {

    private final static String DATA_FILE = "io/kontur/eventapi/tornado/normalization/noaa_tornado_data.csv";

    private NoaaTornadoNormalizer noaaTornadoNormalizer;

    @Autowired
    public NoaaTornadoNormalizerIT(JdbcTemplate jdbcTemplate, NoaaTornadoNormalizer noaaTornadoNormalizer) {
        super(jdbcTemplate);
        this.noaaTornadoNormalizer = noaaTornadoNormalizer;
    }

    @Test
    public void testIsApplicable() {
        DataLake dataLake = new DataLake();
        dataLake.setProvider(TORNADO_NOAA_PROVIDER);
        assertTrue(noaaTornadoNormalizer.isApplicable(dataLake));
    }

    @Test
    public void testNormalize() throws IOException {
        DataLake dataLake = createTestDataLake();
        NormalizedObservation normalizedObservation = noaaTornadoNormalizer.normalize(dataLake);

        assertEquals(dataLake.getObservationId(), normalizedObservation.getObservationId());
        assertEquals(dataLake.getUpdatedAt(), normalizedObservation.getSourceUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), normalizedObservation.getLoadedAt());
        assertEquals(dataLake.getExternalId(), normalizedObservation.getExternalEpisodeId());
        assertEquals(dataLake.getProvider(), normalizedObservation.getProvider());

        assertNull(normalizedObservation.getExternalEventId());
        assertNull(normalizedObservation.getRegion());
        assertNull(normalizedObservation.getSourceUri());

        assertNotNull(normalizedObservation.getName());
        assertNotNull(normalizedObservation.getStartedAt());
        assertNotNull(normalizedObservation.getEndedAt());
        assertNotNull(normalizedObservation.getGeometries());

        assertEquals(Severity.UNKNOWN, normalizedObservation.getEventSeverity());
        assertEquals(EventType.TORNADO, normalizedObservation.getType());
        assertEquals(BigDecimal.ZERO, normalizedObservation.getCost());
        assertEquals("POINT(-80.9 34.28)", normalizedObservation.getPoint());
    }

    private DataLake createTestDataLake() throws IOException {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId("10117722");
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(TORNADO_NOAA_PROVIDER);
        dataLake.setUpdatedAt(parseDateTimeFromString("24 Feb 2016 10:07:00 GMT"));
        String data = new String(getClass().getClassLoader().getResourceAsStream(DATA_FILE).readAllBytes());
        dataLake.setData(data);
        return dataLake;
    }
}