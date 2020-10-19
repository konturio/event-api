package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

public class GdacsNormalizerIT extends AbstractIntegrationTest {

    private final GdacsNormalizer gdacsNormalizer;

    @Autowired
    public GdacsNormalizerIT(GdacsNormalizer gdacsNormalizer) {
        this.gdacsNormalizer = gdacsNormalizer;
    }

    @Test
    public void isApplicable() throws IOException {
        DataLake dataLake = createDataLakeObject();
        assertTrue(gdacsNormalizer.isApplicable(dataLake));
    }

    @Test
    public void normalize() throws IOException {
        var dataLake = createDataLakeObject();
        var observation = gdacsNormalizer.normalize(dataLake);

        String description = "On 10/12/2020 7:03:07 AM, an earthquake occurred in Mexico potentially affecting About 13000 people within 100km. The earthquake had Magnitude 4.9M, Depth:28.99km.";
        String name = "Earthquake in Mexico";

        var fromDate = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 12, 7, 3, 7),
                ZoneOffset.UTC
        );
        var toDate = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 12, 7, 3, 7),
                ZoneOffset.UTC
        );

        assertEquals(dataLake.getExternalId(), observation.getExternalEventId());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());

        assertEquals(GDACS_PROVIDER, observation.getProvider());
        assertEquals(EventType.EARTHQUAKE, observation.getType());
        assertEquals(Severity.MINOR, observation.getEventSeverity());

        assertEquals(name, observation.getName());
        assertEquals(description, observation.getDescription());
        assertEquals(description, observation.getEpisodeDescription());
        assertEquals(fromDate, observation.getStartedAt());
        assertEquals(toDate, observation.getEndedAt());

        assertTrue(observation.getActive());
        assertNull(observation.getPoint());
        assertNull(observation.getCost());
        assertNull(observation.getRegion());

        assertNotNull(observation.getGeometries());
    }

    private DataLake createDataLakeObject() throws IOException {
        var alert = new AlertForInsertDataLake(
                OffsetDateTime.of(LocalDateTime.of(2020, 10, 12, 9, 33, 22), ZoneOffset.UTC),
                "GDACS_EQ_1239039_1337379",
                readMessageFromFile()
        );
        return new GdacsDataLakeConverter().convertGdacs(alert);
    }

    private String readMessageFromFile() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("alert.xml"), "UTF-8");
    }
}