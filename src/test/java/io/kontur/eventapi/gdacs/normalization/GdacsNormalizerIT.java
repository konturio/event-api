package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

public class GdacsNormalizerIT extends AbstractIntegrationTest {

    private final GdacsNormalizer gdacsNormalizer;
    private final GdacsService gdacsService;
    private final GdacsDataLakeConverter gdacsDataLakeConverter;

    @Autowired
    public GdacsNormalizerIT(GdacsNormalizer gdacsNormalizer, GdacsService gdacsService, GdacsDataLakeConverter gdacsDataLakeConverter) {
        this.gdacsNormalizer = gdacsNormalizer;
        this.gdacsService = gdacsService;
        this.gdacsDataLakeConverter = gdacsDataLakeConverter;
    }

    @Test
    public void isApplicable() throws IOException {
        assertTrue(gdacsNormalizer.isApplicable(getDataLakeList().get(0)));
        assertFalse(gdacsNormalizer.isApplicable(getDataLakeList().get(1)));
    }

    @Test
    public void normalize() throws IOException {
        var dataLakes = getDataLakeList();
        gdacsService.saveGdacs(dataLakes);
        var dataLake = dataLakes.get(0);
        var observation = gdacsNormalizer.normalize(dataLake);

        String description = "On 10/12/2020 7:03:07 AM, an earthquake occurred in Mexico potentially affecting About 13000 people within 100km. The earthquake had Magnitude 4.9M, Depth:28.99km.";
        String name = "Green earthquake alert (Magnitude 4.9M, Depth:28.99km) in Mexico 12/10/2020 07:03 UTC, About 13000 people within 100km.";

        var fromDate = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 12, 7, 3, 7),
                ZoneOffset.UTC
        );
        var toDate = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 12, 7, 3, 7),
                ZoneOffset.UTC
        );

        assertNotEquals(dataLake.getExternalId(), observation.getExternalEventId());
        assertEquals("EQ_1239039", observation.getExternalEventId());

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

    private List<DataLake> getDataLakeList() throws IOException {
        var parsedAlert = new ParsedAlert();
        parsedAlert.setIdentifier("GDACS_EQ_1239039_1337379");
        parsedAlert.setDateModified(OffsetDateTime.of(LocalDateTime.of(2020, 10, 12, 9, 33, 22), ZoneOffset.UTC));
        parsedAlert.setSent(OffsetDateTime.parse("2020-10-12T05:03:07-00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        parsedAlert.setData(readMessageFromFile());

        return List.of(
                gdacsDataLakeConverter.convertGdacs(parsedAlert),
                gdacsDataLakeConverter.convertGdacsWithGeometry(parsedAlert, "{}")
        );
    }

    private String readMessageFromFile() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("alert.xml"), "UTF-8");
    }
}