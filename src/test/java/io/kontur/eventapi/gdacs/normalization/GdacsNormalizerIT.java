package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GdacsNormalizerIT extends AbstractIntegrationTest {

    private final GdacsAlertNormalizer gdacsAlertNormalizer;
    private final GdacsGeometryNormalizer gdacsGeometryNormalizer;
    private final GdacsDataLakeConverter gdacsDataLakeConverter;
    private final DataLakeDao dataLakeDao;
    private final NormalizedObservationsDao normalizedObservationsDao;

    private final static String externalId = "GDACS_EQ_1239039_9997379";
    private static DataLake dataLakeAlert;
    private static DataLake dataLakeAlertGeometry;

    @Autowired
    public GdacsNormalizerIT(GdacsAlertNormalizer gdacsAlertNormalizer, GdacsGeometryNormalizer gdacsGeometryNormalizer, GdacsService gdacsService, GdacsDataLakeConverter gdacsDataLakeConverter, DataLakeDao dataLakeDao, NormalizedObservationsDao normalizedObservationsDao) {
        this.gdacsAlertNormalizer = gdacsAlertNormalizer;
        this.gdacsGeometryNormalizer = gdacsGeometryNormalizer;
        this.gdacsDataLakeConverter = gdacsDataLakeConverter;
        this.dataLakeDao = dataLakeDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
    }

    @Test
    @Order(1)
    public void testSavingDataInDb() throws IOException {
        var dataLakes = getDataLakeList();
        dataLakeDao.storeEventData(dataLakes.get(0));
        dataLakeDao.storeEventData(dataLakes.get(1));
        var dataLakeAlertOpt = dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, GDACS_ALERT_PROVIDER);
        var dataLakeGeometryOpt = dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, GDACS_ALERT_GEOMETRY_PROVIDER);

        assertFalse(dataLakeAlertOpt.isEmpty());
        assertFalse(dataLakeGeometryOpt.isEmpty());

        dataLakeAlert = dataLakeAlertOpt.get();
        dataLakeAlertGeometry = dataLakeGeometryOpt.get();
    }

    @Test
    @Order(2)
    public void isApplicableGdacsAlert() throws IOException {
        assertTrue(gdacsAlertNormalizer.isApplicable(getDataLakeList().get(0)));
        assertFalse(gdacsAlertNormalizer.isApplicable(getDataLakeList().get(1)));
    }

    @Test
    @Order(3)
    public void isApplicableGdacsGeometry() throws IOException {
        assertTrue(gdacsGeometryNormalizer.isApplicable(getDataLakeList().get(1)));
        assertFalse(gdacsGeometryNormalizer.isApplicable(getDataLakeList().get(0)));
    }

    @Test
    @Order(4)
    public void normalizeGdacsAlert() throws IOException {
        var observation = gdacsAlertNormalizer.normalize(dataLakeAlert);

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

        assertNotEquals(dataLakeAlert.getExternalId(), observation.getExternalEventId());
        assertEquals("EQ_1239039", observation.getExternalEventId());

        assertEquals(dataLakeAlert.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLakeAlert.getObservationId(), observation.getObservationId());
        assertEquals(dataLakeAlert.getLoadedAt(), observation.getLoadedAt());
        assertEquals(dataLakeAlert.getExternalId(), observation.getExternalEpisodeId());

        assertEquals(GDACS_ALERT_PROVIDER, observation.getProvider());
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

        assertNull(observation.getGeometries());

        normalizedObservationsDao.insert(observation);
    }

    @Test
    @Order(5)
    public void normalizeGdacsGeometry() {
        var observation = gdacsGeometryNormalizer.normalize(dataLakeAlertGeometry);

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

        assertNotEquals(dataLakeAlertGeometry.getExternalId(), observation.getExternalEventId());
        assertEquals("EQ_1239039", observation.getExternalEventId());

        assertEquals(dataLakeAlertGeometry.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLakeAlertGeometry.getObservationId(), observation.getObservationId());
        assertEquals(dataLakeAlertGeometry.getLoadedAt(), observation.getLoadedAt());
        assertEquals(dataLakeAlertGeometry.getExternalId(), observation.getExternalEpisodeId());

        assertEquals(GDACS_ALERT_GEOMETRY_PROVIDER, observation.getProvider());
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
        parsedAlert.setIdentifier(externalId);
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