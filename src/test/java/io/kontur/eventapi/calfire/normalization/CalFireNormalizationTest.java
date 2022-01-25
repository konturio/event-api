package io.kontur.eventapi.calfire.normalization;

import io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.Point;
import java.util.Objects;

import static io.kontur.eventapi.calfire.normalization.CalFireNormalizer.CALFIRE_PROPERTIES;
import static org.junit.jupiter.api.Assertions.*;

class CalFireNormalizationTest {

    @Test
    public void testIsApplicable() throws Exception {
        DataLake dataLake = createDataLake();
        assertTrue(new CalFireNormalizer().isApplicable(dataLake));
    }

    @Test
    public void testNormalization() throws Exception {
        //given
        DataLake dataLake = createDataLake();

        //when
        NormalizedObservation observation = new CalFireNormalizer().normalize(dataLake);

        //then
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(CalFireDataLakeConverter.CALFIRE_PROVIDER, observation.getProvider());
        assertEquals("2347b6fb-8468-46bb-9917-d5cea3f1efdd", observation.getExternalEventId());
        assertNull(observation.getExternalEpisodeId());
        assertEquals(Severity.MINOR, observation.getEventSeverity());
        assertEquals("Wildfire Fire", observation.getName());
        assertEquals("Fire", observation.getProperName());
        assertEquals("Location", observation.getRegion());
        assertEquals(EventType.WILDFIRE, observation.getType());
        assertEquals(1610640000000L, observation.getStartedAt().toInstant().toEpochMilli());
        assertEquals(1610718305000L, observation.getEndedAt().toInstant().toEpochMilli());
        assertEquals(1611582312000L, observation.getSourceUpdatedAt().toInstant().toEpochMilli());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals("https://www.fire.ca.gov", observation.getSourceUri());
        assertEquals("POINT(-100.12345 30.12345)", observation.getPoint());
        checkGeometriesValue(observation.getGeometries());
    }

    private void checkGeometriesValue(FeatureCollection geom) {
        assertNotNull(geom);
        assertEquals(1, geom.getFeatures().length);
        Feature feature = geom.getFeatures()[0];
        assertTrue(feature.getGeometry() instanceof Point);
        Point point = (Point) feature.getGeometry();
        assertEquals(-100.12345, point.getCoordinates()[0]);
        assertEquals(30.12345, point.getCoordinates()[1]);
        assertEquals(CALFIRE_PROPERTIES, feature.getProperties());
    }

    private DataLake createDataLake() throws Exception {
        String geoJson = IOUtils.toString(Objects.requireNonNull(
                this.getClass().getResourceAsStream("CalFireFeature.json")), "UTF-8");
        Feature feature = (Feature) GeoJSONFactory.create(geoJson);
        String externalId = String.valueOf(feature.getProperties().get("UniqueId"));
        String updatedAt = (String) feature.getProperties().get("Updated");
        return new CalFireDataLakeConverter().convertEvent(geoJson, externalId,
                DateTimeUtil.parseDateTimeByPattern(updatedAt, null));
    }

}