package io.kontur.eventapi.gdacs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GdacsDescriptionCleanerTest {

    @Test
    void testCleanDroughtAlert() {
        String src = "The  Drought alert level is Green.";
        String expected = "";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Should remove drought alert line entirely");
    }

    @Test
    void testCleanFloodDescription() {
        String src = "On 16/05/2025, a flood started in China, lasting until 30/07/2025 (last update). The flood caused 49 deaths and 550342 displaced .";
        String expected = "The flood caused 49 deaths and 550342 displaced.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Flood description should strip dates and keep stats");
    }

    @Test
    void testCleanCyclone() {
        String src = "From 10/06/2025 to 14/06/2025, a Tropical Storm (maximum wind speed of 120 km/h) WUTIP-25 was active in NWPacific. The cyclone affects these countries: China, Viet Nam (vulnerability Medium). Estimated population affected by category 1 (120 km/h) wind speeds or higher is 0.968 million .";
        String expected = "Tropical Storm (maximum wind speed of 120 km/h) WUTIP-25 was active in NWPacific. The cyclone affects these countries: China, Viet Nam (vulnerability Medium). Estimated population affected by category 1 (120 km/h) wind speeds or higher is 0.968 million.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Cyclone description should keep useful info and drop date range");
    }

    @Test
    void testZeroLossFlood() {
        String src = "On 01/06/2025, a flood started in Indonesia, lasting until 08/06/2025 (last update). The flood caused 0 deaths and 0 displaced .";
        String expected = "";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Flood with no impact should result in empty description");
    }

    @Test
    void testVulnerabilityUnknown() {
        String src = "From 12/07/2025 to 14/07/2025, a Tropical Depression (maximum wind speed of 56 km/h) SEVEN-25 was active in NWPacific. The cyclone affects these countries: Japan (vulnerability [unknown]). Estimated population affected by category 1 (120 km/h) wind speeds or higher is 0  (0 in tropical storm).";
        String expected = "Tropical Depression (maximum wind speed of 56 km/h) SEVEN-25 was active in NWPacific. The cyclone affects these countries: Japan.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Should drop unknown vulnerability and zero population");
    }

    @Test
    void testEarthquakeNoPeople() {
        String src = "On 7/31/2025 1:01:09 PM, an earthquake occurred in [unknown] potentially affecting No people affected in 100km. The earthquake had Magnitude 5M, Depth:10km.";
        String expected = "The earthquake had Magnitude 5M, Depth:10km.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Earthquake description should remove useless prefix");
    }

    @Test
    void testCycloneZeroPopulation() {
        String src = "From 11/07/2025 to 14/07/2025, a Tropical Storm (maximum wind speed of 93 km/h) NARI-25 was active in NWPacific. The cyclone affects these countries: Japan, Russian Federation (vulnerability Low). Estimated population affected by category 1 (120 km/h) wind speeds or higher is 0  (0 in tropical storm).";
        String expected = "Tropical Storm (maximum wind speed of 93 km/h) NARI-25 was active in NWPacific. The cyclone affects these countries: Japan, Russian Federation (vulnerability Low).";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Cyclone with zero affected population should drop that part");
    }

    @Test
    void testFloodZeroDisplaced() {
        String src = "On 16/06/2025, a flood started in India, lasting until 26/06/2025 (last update). The flood caused 13 deaths and 0 displaced .";
        String expected = "The flood caused 13 deaths.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Flood description should omit zero displaced numbers");
    }

    @Test
    void testForestFireRemoval() {
        String src = "On 21/07/2025, a forest fire started in Australia,  until 26/07/2025.";
        String expected = "";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Forest fire description with no details should be dropped");
    }

    @Test
    void testFloodDisplacedOnly() {
        String src = "On 06/06/2025, a flood started in Philippines, lasting until 09/06/2025 (last update). The flood caused 0 deaths and 1799 displaced .";
        String expected = "The flood caused 1799 displaced.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Flood description should keep displaced count only");
    }

    @Test
    void testDroughtOrange() {
        String src = "The Drought alert level is Orange.";
        String expected = "";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Orange drought alert lines should be removed");
    }

    @Test
    void testFloodStatsPreserved() {
        String src = "On 14/07/2025, a flood started in Pakistan, lasting until 17/07/2025 (last update). The flood caused 196 deaths and 176 displaced .";
        String expected = "The flood caused 196 deaths and 176 displaced.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Flood stats should be kept when not zero");
    }

    @Test
    void testFloodOnlyDeaths() {
        String src = "On 27/06/2025, a flood started in Afghanistan, lasting until 07/07/2025 (last update). The flood caused 8 deaths and 0 displaced .";
        String expected = "The flood caused 8 deaths.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Flood description should keep deaths count only");
    }

    @Test
    void testFloodAllZero() {
        String src = "On 30/06/2025, a flood started in Algeria, lasting until 02/07/2025 (last update). The flood caused 0 deaths and 0 displaced .";
        String expected = "";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Zero impact flood should yield empty description");
    }

    @Test
    void testDepressionUnknown() {
        String src = "From 25/06/2025 to 26/06/2025, a Tropical Depression (maximum wind speed of 46 km/h) THREE-25 was active in NWPacific. The cyclone affects these countries: China (vulnerability [unknown]). Estimated population affected by category 1 (120 km/h) wind speeds or higher is 0 (0 in tropical storm).";
        String expected = "Tropical Depression (maximum wind speed of 46 km/h) THREE-25 was active in NWPacific. The cyclone affects these countries: China.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Unknown vulnerability and zero population should be removed");
    }

    @Test
    void testEarthquakeWithPopulation() {
        String src = "On 7/21/2025 9:54:00 AM, an earthquake occurred in Taiwan potentially affecting 650 thousand in MMI VI. The earthquake had Magnitude 5.6M, Depth:10 km.";
        String expected = "The earthquake had Magnitude 5.6M, Depth:10 km.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Earthquake prefix with population should be removed");
    }

    @Test
    void testEarthquakeNoPeople2() {
        String src = "On 6/28/2025 8:32:21 AM, an earthquake occurred in [unknown] potentially affecting No people affected in 100 km. The earthquake had Magnitude 6.6M, Depth:10 km.";
        String expected = "The earthquake had Magnitude 6.6M, Depth:10 km.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Earthquake description should drop useless population part");
    }

    @Test
    void testForestFireShort() {
        String src = "On 22/06/2025, a forest fire started in Greece, until 25/06/2025.";
        String expected = "";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Short forest fire records should be removed");
    }

    @Test
    void testVolcanoPreserved() {
        String src = "Volcano Fuego is emitting ash clouds according to the regional VAAC. The aviation alert level is Green.";
        String expected = "Volcano Fuego is emitting ash clouds according to the regional VAAC. The aviation alert level is Green.";
        assertEquals(expected, GdacsDescriptionCleaner.clean(src),
                "Volcano messages should be left untouched");
    }
}
