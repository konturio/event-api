package io.kontur.eventapi.stormsnoaa.normalization;

import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.util.DateTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class StormsNoaaFloodSeverityTest {

    private StormsNoaaNormalizer normalizer;

    @BeforeEach
    void setUp() {
        NormalizedObservationsDao dao = Mockito.mock(NormalizedObservationsDao.class);
        Mockito.when(dao.getTimestampAtTimezone(any(), any()))
                .thenAnswer(invocation -> OffsetDateTime.of(invocation.getArgument(0), ZoneOffset.UTC));
        normalizer = new StormsNoaaNormalizer(dao);
    }

    @Test
    void normalizeFloodSeverityUnknown() {
        DataLake dataLake = createFloodDataLake(null, null, null, null, null, null);
        NormalizedObservation observation = normalizer.normalize(dataLake);
        assertEquals(Severity.UNKNOWN, observation.getEventSeverity(), "All loss fields missing should result in UNKNOWN severity");
    }

    @Test
    void normalizeFloodSeverityMinor() {
        DataLake dataLake = createFloodDataLake(null, null, null, null, "1", null);
        NormalizedObservation observation = normalizer.normalize(dataLake);
        assertEquals(Severity.MINOR, observation.getEventSeverity(), "Single injury should result in MINOR severity");
    }

    @Test
    void normalizeFloodSeverityModerate() {
        DataLake dataLake = createFloodDataLake(null, null, "1", null, null, null);
        NormalizedObservation observation = normalizer.normalize(dataLake);
        assertEquals(Severity.MODERATE, observation.getEventSeverity(), "One death should result in MODERATE severity");
    }

    @Test
    void normalizeFloodSeveritySevere() {
        DataLake dataLake = createFloodDataLake("1M", null, null, null, null, null);
        NormalizedObservation observation = normalizer.normalize(dataLake);
        assertEquals(Severity.SEVERE, observation.getEventSeverity(), "Property damage >= 1M should result in SEVERE severity");
    }

    @Test
    void normalizeFloodSeverityExtreme() {
        DataLake dataLake = createFloodDataLake(null, "10M", null, null, null, null);
        NormalizedObservation observation = normalizer.normalize(dataLake);
        assertEquals(Severity.EXTREME, observation.getEventSeverity(), "Crop damage >= 10M should result in EXTREME severity");
    }

    private DataLake createFloodDataLake(String damageProperty, String damageCrops,
                                         String deathsDirect, String deathsIndirect,
                                         String injuriesDirect, String injuriesIndirect) {
        String header = "EVENT_TYPE,EPISODE_ID,EVENT_ID,STATE,CZ_NAME,BEGIN_DATE_TIME,END_DATE_TIME,DAMAGE_PROPERTY,DAMAGE_CROPS,DEATHS_DIRECT,DEATHS_INDIRECT,INJURIES_DIRECT,INJURIES_INDIRECT";
        String row = String.join(",",
                "Flood",
                "",
                "",
                "STATE",
                "ZONE",
                "01-JAN-20 00:00:00",
                "01-JAN-20 01:00:00",
                damageProperty == null ? "" : damageProperty,
                damageCrops == null ? "" : damageCrops,
                deathsDirect == null ? "" : deathsDirect,
                deathsIndirect == null ? "" : deathsIndirect,
                injuriesDirect == null ? "" : injuriesDirect,
                injuriesIndirect == null ? "" : injuriesIndirect
        );
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setUpdatedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider("storms.noaa");
        dataLake.setData(header + "\n" + row);
        return dataLake;
    }
}
