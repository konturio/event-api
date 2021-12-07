package io.kontur.eventapi.emdat.service;

import io.kontur.eventapi.client.KonturApiClient;
import io.kontur.eventapi.emdat.dto.GeocoderDto;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EmDatNormalizationService {

    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final KonturApiClient konturApiClient;

    public EmDatNormalizationService(KonturApiClient konturApiClient) {
        this.konturApiClient = konturApiClient;
    }

    public Optional<Geometry> obtainGeometries(String country, String location) {
        List<String> regions = splitLocation(location);

        if (regions.isEmpty()) {
            Optional<GeocoderDto> countryDto = callGeocoder(country);
            if (countryDto.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(geoJSONWriter.write(convertBbox(countryDto.get().getBounds())));
        }

        Polygon[] polygons = regions.stream()
                .map(region -> callGeocoder(country + " " + region))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(GeocoderDto::getBounds)
                .map(this::convertBbox)
                .toArray(Polygon[]::new);

        MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(polygons);

        return Optional.of(geoJSONWriter.write(multiPolygon));
    }

    public Optional<? extends Geometry> convertWktPointIntoGeometry(String point) {
        if (StringUtils.isEmpty(point)) {
            return Optional.empty();
        }

        try {
            org.locationtech.jts.geom.Geometry geometry = wktReader.read(point);
            return Optional.of(geoJSONWriter.write(geometry));
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    private Optional<GeocoderDto> callGeocoder(String search) {
        List<GeocoderDto> dtos = konturApiClient.geocoder(search);
        if (CollectionUtils.isEmpty(dtos)) {
            return Optional.empty();
        }
        return Optional.of(dtos.get(0));
    }

    private List<String> splitLocation(String location) {
        if (location == null || location.isBlank()) {
            return Collections.emptyList();
        }
        return Stream
                .of(location.split("(?:^|(( ?; ?)|( ?, ?)))| ?\\(.*?\\) ?"))
                .filter(s -> !StringUtils.isEmpty(s))
                .collect(Collectors.toList());
    }

    private Polygon convertBbox(List<BigDecimal> bbox) {
        CoordinateXY coord1 = new CoordinateXY(bbox.get(0).doubleValue(), bbox.get(1).doubleValue());
        CoordinateXY coord2 = new CoordinateXY(bbox.get(2).doubleValue(), bbox.get(1).doubleValue());
        CoordinateXY coord3 = new CoordinateXY(bbox.get(2).doubleValue(), bbox.get(3).doubleValue());
        CoordinateXY coord4 = new CoordinateXY(bbox.get(0).doubleValue(), bbox.get(3).doubleValue());
        return geometryFactory.createPolygon(new CoordinateXY[]{coord1, coord2, coord3, coord4, coord1});
    }

}
