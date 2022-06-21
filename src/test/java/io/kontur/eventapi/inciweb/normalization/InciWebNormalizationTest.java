package io.kontur.eventapi.inciweb.normalization;

import static io.kontur.eventapi.inciweb.normalization.InciWebNormalizer.INCIWEB_PROPERTIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.kontur.eventapi.cap.dto.CapParsedEvent;
import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.inciweb.converter.InciWebDataLakeConverter;
import io.kontur.eventapi.inciweb.converter.InciWebXmlParser;
import io.kontur.eventapi.inciweb.job.InciWebImportJob;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

class InciWebNormalizationTest {

    @Test
    public void testIsApplicable() throws Exception {
        DataLake dataLake = createDataLake();
        assertTrue(new InciWebNormalizer().isApplicable(dataLake));
    }

    @Test
    public void testNormalization() throws Exception {
        //given
        DataLake dataLake = createDataLake();

        //when
        NormalizedObservation observation = new InciWebNormalizer().normalize(dataLake);

        //then
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(InciWebImportJob.INCIWEB_PROVIDER, observation.getProvider());
        assertEquals("http://example.com/incident/1/", observation.getExternalEventId());
        assertNull(observation.getExternalEpisodeId());
        assertEquals(Severity.UNKNOWN, observation.getEventSeverity());
        assertEquals("Title 1", observation.getName());
        assertEquals("Description 1", observation.getDescription());
        assertEquals(EventType.WILDFIRE, observation.getType());
        assertEquals(dataLake.getUpdatedAt(), observation.getStartedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getEndedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(List.of("http://example.com/incident/1/"), observation.getUrls());
        assertEquals("POINT(-110.111 10.111)", observation.getPoint());
        checkGeometriesValue(observation.getGeometries());
    }

    private void checkGeometriesValue(FeatureCollection geom) {
        assertNotNull(geom);
        assertEquals(1, geom.getFeatures().length);
        Feature feature = geom.getFeatures()[0];
        assertTrue(feature.getGeometry() instanceof Point);
        Point point = (Point) feature.getGeometry();
        assertEquals(-110.111, point.getCoordinates()[0]);
        assertEquals(10.111, point.getCoordinates()[1]);
        assertEquals(INCIWEB_PROPERTIES, feature.getProperties());
    }

    private DataLake createDataLake() throws Exception {
        String data = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("data.xml")), "UTF-8");
        Optional<CapParsedEvent> parsedItem = new InciWebXmlParser().getParsedItemForDataLake(data, "provider");
        assertTrue(parsedItem.isPresent());
        return new InciWebDataLakeConverter().convertEvent((CapParsedItem) parsedItem.get(),
                InciWebImportJob.INCIWEB_PROVIDER);
    }
}