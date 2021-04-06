package io.kontur.eventapi.staticdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;

@Configuration
public class StaticDataConfig {
    private final static String STATIC_FILES_FOLDER = "static/";

    @Bean
    public StaticFileData getTornadoCanadaGovFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "tornado-canada-gov.geojson",
                "tornado.canada-gov",
                OffsetDateTime.parse("2018-08-16T00:00:00Z"),
                "geojson");
    }

    @Bean
    public StaticFileData getTornadoAustralianBmFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "tornado-australian-bm.geojson",
                "tornado.australian-bm",
                OffsetDateTime.parse("2020-01-01T00:00:00Z"),
                "geojson");
    }

    @Bean
    public StaticFileData getTornadoOsmFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "tornado-osm-wiki.geojson",
                "tornado.osm-wiki",
                null,
                "geojson");
    }

    @Bean
    public StaticFileData getTornadoDesInventarSendaiFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "tornado-des-inventar-sendai.geojson",
                "tornado.des-inventar-sendai",
                null,
                "geojson");
    }

    @Bean
    public StaticFileData getWildfireFrapCalFileData() {
        return new StaticFileData (
                STATIC_FILES_FOLDER + "wildfire-frap-cal.geojson",
                "wildfire.frap.cal",
                OffsetDateTime.parse("2020-05-01T00:00:00Z"),
                "geojson"
        );
    }
}
