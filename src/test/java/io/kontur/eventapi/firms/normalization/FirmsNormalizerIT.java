package io.kontur.eventapi.firms.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.firms.FirmsUtil;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static io.kontur.eventapi.TestUtil.readFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FirmsNormalizerIT extends AbstractCleanableIntegrationTest {

    private final FirmsNormalizer firmsNormalizer;

    @Autowired
    public FirmsNormalizerIT(FirmsNormalizer firmsNormalizer, JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
        this.firmsNormalizer = firmsNormalizer;
    }

    @Test
    public void isApplicable() throws IOException {
        //given
        DataLake dataLake = createDataLakeObject();

        //when
        boolean applicable = firmsNormalizer.isApplicable(dataLake);

        //then
        assertTrue(applicable);
    }

    @Test
    public void normalize() throws IOException {
        //given
        var dataLake = createDataLakeObject();

        //when
        var observation = firmsNormalizer.normalize(dataLake);

        //then
        assertEquals(dataLake.getExternalId(), observation.getExternalEventId());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getStartedAt());
        assertEquals(FirmsUtil.MODIS_PROVIDER, observation.getProvider());
        assertEquals(EventType.WILDFIRE, observation.getType());
        assertEquals("POINT(133.141 -2.443)", observation.getPoint());
        assertEquals(readFile(this, "firms.geometries.json"), observation.getGeometries());

        assertNull(observation.getEventSeverity());
        assertNull(observation.getName());
        assertNull(observation.getDescription());
        assertNull(observation.getEpisodeDescription());
        assertNull(observation.getEndedAt());
        assertNull(observation.getSourceUri());
        assertNull(observation.getActive());
        assertNull(observation.getCost());
        assertNull(observation.getRegion());

    }

    private DataLake createDataLakeObject() throws IOException {
        DataLake dataLake = new DataLake();

        dataLake.setUpdatedAt(OffsetDateTime.of(LocalDateTime.parse("2020-11-01T01:30"), ZoneOffset.UTC));
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setData("latitude,longitude,brightness,scan,track,acq_date,acq_time,satellite,confidence,version,bright_t31,frp,daynight\n" +
                "-2.443,133.141,327.1,1,1,2020-11-01,0130,T,61,6.0NRT,298.7,19.6,D");
        dataLake.setLoadedAt(OffsetDateTime.of(LocalDateTime.parse("2020-11-02T14:36:50.631"), ZoneOffset.UTC));
        dataLake.setExternalId("6c241f36746818e6c25c0d36e6f92d51");
        dataLake.setProvider(FirmsUtil.MODIS_PROVIDER);

        return dataLake;
    }
}