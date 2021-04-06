package io.kontur.eventapi.staticdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class StaticDataConfig {
    private final static String STATIC_FILES_FOLDER = "static/";

    @Bean
    public List<StaticFileData> getStaticFiles() {
        List<StaticFileData> staticFiles = new ArrayList<>();
        staticFiles.add(getTornadoCanadaGovFileData());
        staticFiles.add(getTornadoAustralianBmFileData());
        staticFiles.add(getTornadoOsmFileData());
        staticFiles.add(getTornadoDesInventarSendaiFileData());
        staticFiles.addAll(getWildfireFrapCalFilesData());

        return staticFiles;
    }

    private StaticFileData getTornadoCanadaGovFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "tornado-canada-gov.geojson",
                "tornado.canada-gov",
                OffsetDateTime.parse("2018-08-16T00:00:00Z"),
                "geojson");
    }

    private StaticFileData getTornadoAustralianBmFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "tornado-australian-bm.geojson",
                "tornado.australian-bm",
                OffsetDateTime.parse("2020-01-01T00:00:00Z"),
                "geojson");
    }

    private StaticFileData getTornadoOsmFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "tornado-osm-wiki.geojson",
                "tornado.osm-wiki",
                null,
                "geojson");
    }

    private StaticFileData getTornadoDesInventarSendaiFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "tornado-des-inventar-sendai.geojson",
                "tornado.des-inventar-sendai",
                null,
                "geojson");
    }

    private List<StaticFileData> getWildfireFrapCalFilesData() {
        return List.of("1870-1930", "1930-1960", "1960-1980", "1980-2000", "2000-2010", "2010-2015", "2015-2020")
                .stream()
                .map(year -> new StaticFileData(
                        String.format("%swildfire-frap-cal_%s.geojson", STATIC_FILES_FOLDER, year),
                        "wildfire.frap.cal",
                        OffsetDateTime.parse("2020-05-01T00:00:00Z"),
                        "geojson"))
                .collect(Collectors.toList());
    }
}
