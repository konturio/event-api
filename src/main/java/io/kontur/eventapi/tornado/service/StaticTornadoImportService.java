package io.kontur.eventapi.tornado.service;

import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;

import static io.kontur.eventapi.tornado.job.StaticTornadoImportJob.*;
import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

@Component
public class StaticTornadoImportService {
    private static final Map<String, String> PROVIDER_FILE_NAMES = Map.of(
            TORNADO_CANADA_GOV_PROVIDER, "static/kontur_tornado_pt_canada.json",
            TORNADO_AUSTRALIAN_BM_PROVIDER, "static/kontur_tornado_pt_australia.json",
            TORNADO_OSM_PROVIDER, "static/kontur_tornado_pt_osm.json"
    );

    private final static Map<String, OffsetDateTime> PROVIDER_UPDATE_DATES = Map.of(
            TORNADO_CANADA_GOV_PROVIDER, parseDateTimeFromString("16 Aug 2018 00:00:00 GMT"),
            TORNADO_AUSTRALIAN_BM_PROVIDER, parseDateTimeFromString("1 Jan 2020 00:00:00 GMT")
    );

    public Map<String, String> getProviderFilenames() {
        return PROVIDER_FILE_NAMES;
    }

    public OffsetDateTime getProviderUpdateDate(String provider) {
        return PROVIDER_UPDATE_DATES.getOrDefault(provider, null);
    }

    public String readFile(String fileName) throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new Exception("File not found: " + fileName);
        }
        return new String(inputStream.readAllBytes());
    }

    public Feature[] getFeatures(String json) {
        FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(json);
        return featureCollection.getFeatures();
    }
}
