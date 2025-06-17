package io.kontur.eventapi.tornadojapanma.normalizer;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.kontur.eventapi.tornadojapanma.service.TornadoJapanMaImportService.TORNADO_JAPAN_MA_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

class TornadoJapanMaNormalizerIT extends AbstractCleanableIntegrationTest {

    private final TornadoJapanMaNormalizer normalizer;

    @Autowired
    public TornadoJapanMaNormalizerIT(JdbcTemplate jdbcTemplate, TornadoJapanMaNormalizer normalizer, FeedDao feedDao) {
        super(jdbcTemplate, feedDao);
        this.normalizer = normalizer;
    }

    @Test
    public void testIsApplicable() {
        assertFalse(normalizer.isApplicable(createDataLakeWithProvider("test-provider")));
        assertTrue(normalizer.isApplicable(createDataLakeWithProvider(TORNADO_JAPAN_MA_PROVIDER)));
    }

    @Test
    public void testNormalize() {
        DataLake dataLake = createTestDataLake();
        NormalizedObservation normalizedObservation = normalizer.normalize(dataLake);
        assertEquals(dataLake.getObservationId(), normalizedObservation.getObservationId());
        assertEquals(dataLake.getExternalId(), normalizedObservation.getExternalEventId());
        assertEquals(dataLake.getUpdatedAt(), normalizedObservation.getSourceUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), normalizedObservation.getLoadedAt());
        assertEquals(dataLake.getProvider(), normalizedObservation.getProvider());

        assertNull(normalizedObservation.getGeometries());
        assertNull(normalizedObservation.getDescription());
        assertNull(normalizedObservation.getEpisodeDescription());
        assertNull(normalizedObservation.getCost());
        assertNull(normalizedObservation.getRegion());
        assertNull(normalizedObservation.getSourceUpdatedAt());
        assertNull(normalizedObservation.getExternalEpisodeId());
        assertFalse(normalizedObservation.getActive());
        assertEquals(Severity.MINOR, normalizedObservation.getEventSeverity());
        assertEquals("Tornado - Japan, 高知県 安芸市", normalizedObservation.getName());
        assertEquals(EventType.OTHER, normalizedObservation.getType());
        assertEquals(OffsetDateTime.parse("2019-06-27T11:00:00Z"), normalizedObservation.getStartedAt());
        assertEquals(OffsetDateTime.parse("2019-06-27T11:00:00Z"), normalizedObservation.getEndedAt());

    }

    @Test
    private DataLake createDataLakeWithProvider(String provider) {
        DataLake dataLake = new DataLake();
        dataLake.setProvider(provider);
        return dataLake;
    }

    private DataLake createTestDataLake() {
        String data = "{\"type\":\"不明\",\"occurrenceDateTime\":\"2019/06/27 11:00頃\"," +
                "\"occurrencePlace\":\"高知県 安芸市\",\"details\":null,\"jefScale\":{\"windSpeed\":\"約35m/s\"," +
                "\"fscale\":\"JEF0\"},\"damageWidth\":\"340\",\"damageLength\":\"3.9\"," +
                "\"mainDamageSituation\":{\"dead\":\"0\",\"injuredPerson\":\"0\",\"completelyDestroyedHouse\":\"0\"," +
                "\"halfDestroyedHouse\":\"0\"},\"viewingArea\":\"梅雨前線\",\"remarks\":\"\",\"fscale\":null}";
        DataLake dataLake = new DataLake(UUID.randomUUID(), DigestUtils.md5Hex(data), OffsetDateTime.now(), OffsetDateTime.now());
        dataLake.setProvider(TORNADO_JAPAN_MA_PROVIDER);
        dataLake.setData(data);
        return dataLake;
    }
}