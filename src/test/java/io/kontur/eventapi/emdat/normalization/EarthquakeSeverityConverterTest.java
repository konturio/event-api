package io.kontur.eventapi.emdat.normalization;

import io.kontur.eventapi.emdat.normalization.converter.EarthquakeSeverityConverter;
import io.kontur.eventapi.entity.Severity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EarthquakeSeverityConverterTest {

    @Test
    public void defineSeverityTest() {
        EarthquakeSeverityConverter converter = new EarthquakeSeverityConverter();

        assertEquals(Severity.EXTREME, converter.defineSeverity(Map.of("Dis Mag Value", "9")));
        assertEquals(Severity.EXTREME, converter.defineSeverity(Map.of("Dis Mag Value", "7")));
        assertEquals(Severity.SEVERE, converter.defineSeverity(Map.of("Dis Mag Value", "6")));
        assertEquals(Severity.MODERATE, converter.defineSeverity(Map.of("Dis Mag Value", "5")));
        assertEquals(Severity.MODERATE, converter.defineSeverity(Map.of("Dis Mag Value", "4")));
        assertEquals(Severity.MINOR, converter.defineSeverity(Map.of("Dis Mag Value", "3")));
        assertEquals(Severity.MINOR, converter.defineSeverity(Map.of("Dis Mag Value", "2")));
        assertEquals(Severity.UNKNOWN, converter.defineSeverity(Map.of("Dis Mag Value", "")));
        assertEquals(Severity.UNKNOWN, converter.defineSeverity(emptyMap()));
    }
}