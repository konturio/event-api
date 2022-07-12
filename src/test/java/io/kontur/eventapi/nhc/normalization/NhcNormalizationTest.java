package io.kontur.eventapi.nhc.normalization;

import static io.kontur.eventapi.util.GeometryUtil.IS_OBSERVED_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.kontur.eventapi.cap.dto.CapParsedEvent;
import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.nhc.NhcUtil;
import io.kontur.eventapi.nhc.converter.NhcDataLakeConverter;
import io.kontur.eventapi.nhc.converter.NhcXmlParser;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

@ExtendWith(MockitoExtension.class)
public class NhcNormalizationTest {

    @Test
    public void testIsApplicable() throws Exception {
        DataLake dataLake = createDataLake();
        assertTrue(new NhcNormalizer().isApplicable(dataLake));
    }

    @Test
    public void testNormalization() throws Exception {
        //given
        DataLake dataLake = createDataLake();

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(NhcUtil.NHC_EP_PROVIDER, observation.getProvider());
        assertEquals("EP052022", observation.getExternalEventId());
        assertEquals("EP052022_10", observation.getExternalEpisodeId());
        assertEquals(Severity.EXTREME, observation.getEventSeverity());
        assertEquals("HURRICANE DARBY", observation.getName());
        assertEquals("NO DESCRIPTION.", observation.getDescription());
        assertEquals(EventType.CYCLONE, observation.getType());
        assertEquals(DateTimeUtil.parseDateTimeByPattern("2022-07-11T21:00:00Z", null), observation.getStartedAt());
        assertNull(observation.getEndedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(List.of("https://www.nhc.noaa.gov/text/refresh/MIATCMEP5+shtml/112045.shtml"), observation.getUrls());
        assertNull(observation.getPoint());
        checkGeometriesValue(observation.getGeometries());
    }

    private void checkGeometriesValue(FeatureCollection geom) {
        assertNotNull(geom);
        assertEquals(9, geom.getFeatures().length);
        Feature feature = geom.getFeatures()[0];
        assertTrue(feature.getGeometry() instanceof Point);
        assertEquals(1, Arrays.stream(geom.getFeatures())
                .map(Feature::getProperties)
                .map(item -> item.get(IS_OBSERVED_PROPERTY))
                .filter(Boolean.TRUE::equals).toList().size());
    }

    private DataLake createDataLake() throws Exception {
        String data = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("nhc_norm_test1.xml")), "UTF-8");
        Optional<CapParsedEvent> parsedItem = new NhcXmlParser().getParsedItemForDataLake(data, NhcUtil.NHC_EP_PROVIDER);
        assertTrue(parsedItem.isPresent());
        return new NhcDataLakeConverter().convertEvent((CapParsedItem) parsedItem.get(),
                NhcUtil.NHC_EP_PROVIDER);
    }

}
