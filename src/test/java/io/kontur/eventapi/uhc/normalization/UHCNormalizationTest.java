package io.kontur.eventapi.uhc.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Objects;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.uhc.converter.UHCDataLakeConverter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSONFactory;

@ExtendWith(MockitoExtension.class)
class UHCNormalizationTest {

    private final static String testFile = "event.geojson";

    @Mock
    private DataLakeDao dataLakeDao;

    @AfterEach
    public void resetMocks() {
        reset(dataLakeDao);
    }

    @Test
    public void testIsApplicable() throws Exception {
        DataLake dataLake = createDataLake();
        assertTrue(new HumanitarianCrisisNormalizer().isApplicable(dataLake));
    }

    @Test
    public void testNormalization() throws Exception {
        //given
        DataLake dataLake = createDataLake();

        //when
        NormalizedObservation observation = new HumanitarianCrisisNormalizer().normalize(dataLake).get();

        //then
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(UHCDataLakeConverter.UHC_PROVIDER, observation.getProvider());
        assertEquals("ed2daf7d-01a9-42ef-8a30-4b99d40a6f50", observation.getExternalEventId());
        assertEquals(Severity.EXTREME, observation.getEventSeverity());
        assertEquals("Humanitarian crisis name", observation.getName());
        assertEquals("Humanitarian crisis name", observation.getProperName());
        assertEquals("Location", observation.getRegion());
        assertEquals(EventType.SITUATION, observation.getType());
        assertEquals(1645671600000L, observation.getStartedAt().toInstant().toEpochMilli());
        assertEquals(1645671600000L, observation.getEndedAt().toInstant().toEpochMilli());
        assertEquals(1647820800000L, observation.getSourceUpdatedAt().toInstant().toEpochMilli());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals("POINT(1.0 1.0)", observation.getPoint());
    }

    private DataLake createDataLake() throws Exception {
        when(dataLakeDao.isNewEvent(isA(String.class), isA(String.class), isA(String.class))).thenReturn(true);
        String geoJson = IOUtils.toString(Objects.requireNonNull(
                this.getClass().getResourceAsStream(testFile)), "UTF-8");
        Feature feature = (Feature) GeoJSONFactory.create(geoJson);
        return new UHCDataLakeConverter().convertEvent(feature, dataLakeDao);
    }

}