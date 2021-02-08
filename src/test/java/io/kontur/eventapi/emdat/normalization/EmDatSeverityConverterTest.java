package io.kontur.eventapi.emdat.normalization;

import io.kontur.eventapi.emdat.normalization.converter.EmDatSeverityConverter;
import io.kontur.eventapi.entity.Severity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmDatSeverityConverterTest {

    @Test
    public void defineSeverityTest() {
        EmDatSeverityConverter converter = new EmDatSeverityConverter();

        assertEquals(Severity.EXTREME, converter.defineSeverity(Map.of("Total Deaths", "100", "Total Affected", "100")));
        assertEquals(Severity.EXTREME, converter.defineSeverity(Map.of("Total Deaths", "100", "Total Damages ('000 US$)", "100")));
        assertEquals(Severity.EXTREME, converter.defineSeverity(Map.of("Total Deaths", "15")));
        assertEquals(Severity.EXTREME, converter.defineSeverity(Map.of("Total Deaths", "10")));

        assertEquals(Severity.SEVERE, converter.defineSeverity(Map.of("Total Deaths", "9")));
        assertEquals(Severity.SEVERE, converter.defineSeverity(Map.of("Total Affected", "100")));
        assertEquals(Severity.SEVERE, converter.defineSeverity(Map.of("Total Deaths", "","Total Affected", "100")));
        assertEquals(Severity.SEVERE, converter.defineSeverity(Map.of("Total Affected", "100", "Total Damages ('000 US$)", "100")));

        assertEquals(Severity.MODERATE, converter.defineSeverity(Map.of("Total Damages ('000 US$)", "100")));
        assertEquals(Severity.MODERATE, converter.defineSeverity(Map.of("Total Damages ('000 US$)", "100", "Total Deaths", "", "Total Affected", "")));
        assertEquals(Severity.MODERATE, converter.defineSeverity(Map.of("Total Damages ('000 US$)", "100", "Total Affected", "")));
        assertEquals(Severity.MODERATE, converter.defineSeverity(Map.of("Total Damages ('000 US$)", "100", "Total Deaths", "")));

        assertEquals(Severity.UNKNOWN, converter.defineSeverity(Map.of("Total Damages ('000 US$)", "")));
        assertEquals(Severity.UNKNOWN, converter.defineSeverity(Map.of("Total Deaths", "")));
        assertEquals(Severity.UNKNOWN, converter.defineSeverity(Map.of("Total Affected", "")));
        assertEquals(Severity.UNKNOWN, converter.defineSeverity(Map.of("Total Damages ('000 US$)", "", "Total Deaths", "", "Total Affected", "")));
        assertEquals(Severity.UNKNOWN, converter.defineSeverity(emptyMap()));
    }
}