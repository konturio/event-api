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
        DataLake dataLake = createDataLake("nhc_norm_test1.xml", NhcUtil.NHC_AT_PROVIDER);
        assertTrue(new NhcNormalizer().isApplicable(dataLake));
    }

    @Test
    public void testNormalization1() throws Exception {
        //given
        DataLake dataLake = createDataLake("nhc_norm_test1.xml", NhcUtil.NHC_EP_PROVIDER);

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
        checkGeometriesValue(observation.getGeometries(), 9);
    }

    @Test
    public void testNormalization2() throws Exception {
        //given
        DataLake dataLake = createDataLake("nhc_norm_test2.xml", NhcUtil.NHC_AT_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(NhcUtil.NHC_AT_PROVIDER, observation.getProvider());
        assertEquals("AL022022", observation.getExternalEventId());
        assertEquals("AL022022_1", observation.getExternalEpisodeId());
        assertEquals(Severity.MODERATE, observation.getEventSeverity());
        assertEquals("POTENTIAL TROPICAL CYCLONE TWO", observation.getName());
        assertEquals("A TROPICAL STORM WARNING IS IN EFFECT FOR.", observation.getDescription());
        assertEquals(EventType.CYCLONE, observation.getType());
        assertEquals(DateTimeUtil.parseDateTimeByPattern("2022-06-27T21:00:00Z", null), observation.getStartedAt());
        assertNull(observation.getEndedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(List.of("https://www.nhc.noaa.gov/text/refresh/MIATCMAT2+shtml/272045.shtml"), observation.getUrls());
        checkGeometriesValue(observation.getGeometries(), 9);
    }

    @Test
    public void testNormalization4() throws Exception {
        //given
        DataLake dataLake = createDataLake("nhc_norm_test4.xml", NhcUtil.NHC_AT_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(NhcUtil.NHC_AT_PROVIDER, observation.getProvider());
        assertEquals("AL022022", observation.getExternalEventId());
        assertEquals("AL022022_3", observation.getExternalEpisodeId());
        assertEquals(Severity.MODERATE, observation.getEventSeverity());
        assertEquals("POTENTIAL TROPICAL CYCLONE TWO", observation.getName());
        assertEquals("CHANGES IN WATCHES AND WARNINGS WITH THIS ADVISORY...", observation.getDescription());
        assertEquals(EventType.CYCLONE, observation.getType());
        assertEquals(DateTimeUtil.parseDateTimeByPattern("2022-06-28T09:00:00Z", null), observation.getStartedAt());
        assertNull(observation.getEndedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(List.of("https://www.nhc.noaa.gov/text/refresh/MIATCMAT2+shtml/DDHHMM.shtml"), observation.getUrls());
        checkGeometriesValue(observation.getGeometries(), 9);
    }

    @Test
    public void testNormalization5() throws Exception {
        //given
        DataLake dataLake = createDataLake("nhc_norm_test5.xml", NhcUtil.NHC_EP_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(NhcUtil.NHC_EP_PROVIDER, observation.getProvider());
        assertEquals("EP032022", observation.getExternalEventId());
        assertEquals("EP032022_19", observation.getExternalEpisodeId());
        assertEquals(Severity.MINOR, observation.getEventSeverity());
        assertEquals("TROPICAL DEPRESSION CELIA", observation.getName());
        assertEquals("THERE ARE NO COASTAL WATCHES OR WARNINGS IN EFFECT.", observation.getDescription());
        assertEquals(EventType.CYCLONE, observation.getType());
        assertEquals(DateTimeUtil.parseDateTimeByPattern("2022-06-21T09:00:00Z", null), observation.getStartedAt());
        assertNull(observation.getEndedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(List.of("https://www.nhc.noaa.gov/text/refresh/MIATCMEP3+shtml/DDHHMM.shtml"), observation.getUrls());
        checkGeometriesValue(observation.getGeometries(), 9);
    }

    @Test
    public void testNormalization6() throws Exception {
        //given
        DataLake dataLake = createDataLake("nhc_norm_test6.xml", NhcUtil.NHC_EP_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertEquals(dataLake.getObservationId(), observation.getObservationId());
        assertEquals(NhcUtil.NHC_EP_PROVIDER, observation.getProvider());
        assertEquals("EP032022", observation.getExternalEventId());
        assertEquals("EP032022_43", observation.getExternalEpisodeId());
        assertEquals(Severity.MODERATE, observation.getEventSeverity());
        assertEquals("TROPICAL STORM CELIA", observation.getName());
        assertEquals("THERE ARE NO COASTAL WATCHES OR WARNINGS IN EFFECT.", observation.getDescription());
        assertEquals(EventType.CYCLONE, observation.getType());
        assertEquals(DateTimeUtil.parseDateTimeByPattern("2022-06-27T09:00:00Z", null), observation.getStartedAt());
        assertNull(observation.getEndedAt());
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt());
        assertEquals(List.of("https://www.nhc.noaa.gov/text/refresh/MIATCMEP3+shtml/270852.shtml"), observation.getUrls());
        checkGeometriesValue(observation.getGeometries(), 6);
    }

    @Test
    public void testNormalization7() throws Exception {
        //given
        DataLake dataLake = createDataLake("nhc_norm_test7.xml", NhcUtil.NHC_EP_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertNotNull(observation, "Normalization returned null for remnants special advisory");
        assertEquals(dataLake.getObservationId(), observation.getObservationId(),
                "Observation ID should match input for remnants special advisory");
        assertEquals(NhcUtil.NHC_EP_PROVIDER, observation.getProvider(),
                "Provider should remain EP for remnants special advisory");
        assertEquals("EP102024", observation.getExternalEventId(),
                "External event ID not parsed from advisory");
        assertEquals("EP102024_10", observation.getExternalEpisodeId(),
                "External episode ID not constructed properly");
        assertEquals(Severity.MINOR, observation.getEventSeverity(),
                "Severity should be MINOR for 30kt winds");
        assertEquals("REMNANTS JOHN", observation.getName(),
                "Name should include storm type and proper name");
        assertEquals("THIS IS THE LAST FORECAST/ADVISORY ISSUED BY THE NATIONAL HURRICANE CENTER ON THIS SYSTEM",
                observation.getDescription(),
                "Description not parsed correctly");
        assertEquals(EventType.CYCLONE, observation.getType(),
                "Event type should be CYCLONE");
        assertEquals(DateTimeUtil.parseDateTimeByPattern("2024-09-24T18:00:00Z", null), observation.getStartedAt(),
                "Start time not parsed correctly");
        assertNull(observation.getEndedAt(), "End time should be null");
        assertEquals(dataLake.getUpdatedAt(), observation.getSourceUpdatedAt(),
                "Source update time mismatch");
        assertEquals(dataLake.getLoadedAt(), observation.getLoadedAt(),
                "LoadedAt time mismatch");
        assertEquals(List.of("https://www.nhc.noaa.gov/text/refresh/MIATCMEP5+shtml/241747.shtml"), observation.getUrls(),
                "Unexpected URLs parsed");
        checkGeometriesValue(observation.getGeometries(), 1);
    }

    @Test
    public void testNormalizationNegativeType() throws Exception {
        //given - type is absent
        DataLake dataLake = createDataLake("nhc_norm_test_neg1.xml", NhcUtil.NHC_AT_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertNull(observation);
    }

    @Test
    public void testNormalizationNegativeName() throws Exception {
        //given - name is absent
        DataLake dataLake = createDataLake("nhc_norm_test_neg2.xml", NhcUtil.NHC_AT_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertNull(observation);
    }

    @Test
    public void testNormalizationNegativeAdvisoryNumber() throws Exception {
        //given - advisory number is absent
        DataLake dataLake = createDataLake("nhc_norm_test_neg3.xml", NhcUtil.NHC_AT_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertNull(observation);
    }

    @Test
    public void testNormalizationNegativeId() throws Exception {
        //given - event id is absent
        DataLake dataLake = createDataLake("nhc_norm_test_neg4.xml", NhcUtil.NHC_AT_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertNull(observation);
    }

    @Test
    public void testNormalizationNegativeMaxWind() throws Exception {
        //given - event id is absent
        DataLake dataLake = createDataLake("nhc_norm_test_neg5.xml", NhcUtil.NHC_AT_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertNull(observation);
    }

    @Test
    public void testNormalizationNegativeCenterLocation() throws Exception {
        //given - event id is absent
        DataLake dataLake = createDataLake("nhc_norm_test_neg6.xml", NhcUtil.NHC_AT_PROVIDER);

        //when
        NormalizedObservation observation = new NhcNormalizer().normalize(dataLake);

        //then
        assertNull(observation);
    }

    private void checkGeometriesValue(FeatureCollection geom, Integer expectedCount) {
        assertNotNull(geom, "Geometries should not be null");
        assertEquals(expectedCount, geom.getFeatures().length,
                "Unexpected number of geometry features");
        Feature feature = geom.getFeatures()[0];
        assertTrue(feature.getGeometry() instanceof Point,
                "First geometry must be a Point instance");
        assertEquals(1, Arrays.stream(geom.getFeatures())
                        .map(Feature::getProperties)
                        .map(item -> item.get(IS_OBSERVED_PROPERTY))
                        .filter(Boolean.TRUE::equals).toList().size(),
                "Exactly one geometry should be marked as observed");
    }

    private DataLake createDataLake(String fileName, String provider) throws Exception {
        String data = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(fileName)), "UTF-8");
        Optional<CapParsedEvent> parsedItem = new NhcXmlParser().getParsedItemForDataLake(data, provider);
        assertTrue(parsedItem.isPresent(), "Parsed item should be present for " + fileName);
        return new NhcDataLakeConverter().convertEvent((CapParsedItem) parsedItem.get(),
                provider);
    }

}
