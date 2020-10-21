package io.kontur.eventapi.pdc.converter;

import io.kontur.eventapi.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.FeatureCollection;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdcDataLakeConverterTest {

    @Test
    public void testMagConversion() throws IOException {
        //given
        String magJson = readMessageFromFile("PdcDataLakeConverterTest.testMagConversion.json");
        String dataLakeData1 = readMessageFromFile("PdcDataLakeConverterTest.testMagConversion.result1.json");
        String dataLakeData2 = readMessageFromFile("PdcDataLakeConverterTest.testMagConversion.result2.json");

        //when
        var dataLakes = new PdcDataLakeConverter()
                .convertHpSrvMagData(JsonUtil.readTree(magJson), "testEventId");

        //then
        compareFeatureCollections(JsonUtil.readJson(dataLakeData1, FeatureCollection.class),
                JsonUtil.readJson(dataLakes.get(0).getData(), FeatureCollection.class));
    }

    private void compareFeatureCollections(FeatureCollection expected, FeatureCollection actual) {
        assertEquals(expected.getFeatures().length, actual.getFeatures().length);
        //TODO check whether both FC are equal
//        expected.getFeatures().
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}