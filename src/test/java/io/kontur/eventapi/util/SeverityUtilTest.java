package io.kontur.eventapi.util;

import io.kontur.eventapi.entity.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeverityUtilTest {

    @Test
    public void testConvertFujitaScale() {
        assertEquals(Severity.MINOR, SeverityUtil.convertFujitaScale("0"));
        assertEquals(Severity.MODERATE, SeverityUtil.convertFujitaScale("1"));
        assertEquals(Severity.MODERATE, SeverityUtil.convertFujitaScale("2"));
        assertEquals(Severity.SEVERE, SeverityUtil.convertFujitaScale("3"));
        assertEquals(Severity.EXTREME, SeverityUtil.convertFujitaScale("4"));
        assertEquals(Severity.EXTREME, SeverityUtil.convertFujitaScale("5"));

        assertEquals(Severity.UNKNOWN, SeverityUtil.convertFujitaScale("6"));
        assertEquals(Severity.UNKNOWN, SeverityUtil.convertFujitaScale(""));
        assertEquals(Severity.UNKNOWN, SeverityUtil.convertFujitaScale(null));
    }
}