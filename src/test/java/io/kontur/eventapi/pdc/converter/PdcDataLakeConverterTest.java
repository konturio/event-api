package io.kontur.eventapi.pdc.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.io.IOException;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

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

        compareFeatureCollections(JsonUtil.readJson(dataLakeData2, FeatureCollection.class),
                JsonUtil.readJson(dataLakes.get(1).getData(), FeatureCollection.class));
    }

    @Test
    public void testConvertExposure() throws IOException {
        String data = readMessageFromFile("PdcDataLakeConverterTest.testConvertExposure.json");
        String externalId = "testExternalId";

        DataLake dataLake = new PdcDataLakeConverter().convertExposure(data, externalId);

        assertNotNull(dataLake);
        assertNotNull(dataLake.getObservationId());
        assertNotNull(dataLake.getLoadedAt());
        assertNotNull(dataLake.getUpdatedAt());
        assertEquals(dataLake.getLoadedAt(), dataLake.getUpdatedAt());
        assertEquals(PDC_MAP_SRV_PROVIDER, dataLake.getProvider());
        assertEquals(data, dataLake.getData());
        assertEquals(externalId, dataLake.getExternalId());
    }

    private void compareFeatureCollections(FeatureCollection expected, FeatureCollection actual) {
        assertEquals(expected.getFeatures().length, actual.getFeatures().length);

        Feature expectedFeatures = expected.getFeatures()[0];
        Feature actualFeatures = actual.getFeatures()[0];
        
        assertEquals(expectedFeatures.getGeometry().toString(), actualFeatures.getGeometry().toString());
        assertEquals(expectedFeatures.getProperties().size(), actualFeatures.getProperties().size());
        assertEquals(expectedFeatures.getProperties().keySet(), actualFeatures.getProperties().keySet());
        assertTrue(expectedFeatures.getProperties().values().containsAll(actualFeatures.getProperties().values()));
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}