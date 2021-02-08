package io.kontur.eventapi.emdat.normalization;

import io.kontur.eventapi.emdat.normalization.converter.CycloneSeverityConverter;
import io.kontur.eventapi.entity.Severity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CycloneSeverityConverterTest {

    @Test
    public void defineSeverityTest() {
        CycloneSeverityConverter converter = new CycloneSeverityConverter();

        assertEquals(Severity.EXTREME, converter.defineSeverity(Map.of("Dis Mag Value", "209")));
        assertEquals(Severity.EXTREME, converter.defineSeverity(Map.of("Dis Mag Value", "250")));
        assertEquals(Severity.SEVERE, converter.defineSeverity(Map.of("Dis Mag Value", "208")));
        assertEquals(Severity.SEVERE, converter.defineSeverity(Map.of("Dis Mag Value", "170")));
        assertEquals(Severity.SEVERE, converter.defineSeverity(Map.of("Dis Mag Value", "154")));
        assertEquals(Severity.MODERATE, converter.defineSeverity(Map.of("Dis Mag Value", "153")));
        assertEquals(Severity.MODERATE, converter.defineSeverity(Map.of("Dis Mag Value", "100")));
        assertEquals(Severity.UNKNOWN, converter.defineSeverity(Map.of("Dis Mag Value", "")));
        assertEquals(Severity.UNKNOWN, converter.defineSeverity(emptyMap()));
    }

}