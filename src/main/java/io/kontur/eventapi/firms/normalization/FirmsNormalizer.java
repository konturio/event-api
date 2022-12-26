package io.kontur.eventapi.firms.normalization;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.firms.dto.ParsedNormalizationItem;
import io.kontur.eventapi.normalization.Normalizer;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.kontur.eventapi.firms.FirmsUtil.FIRMS_PROVIDERS;

import static io.kontur.eventapi.util.CsvUtil.parseNormalizationRow;
import static io.kontur.eventapi.util.GeometryUtil.*;

@Component
public class FirmsNormalizer extends Normalizer {
    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();
    private final H3Core h3;

    public static final Map<String, Object> FIRMS_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, EXPOSURE, IS_OBSERVED_PROPERTY, true);

    public FirmsNormalizer() {
        try {
            h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException("failed to create h3 engine", e);
        }
    }

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return FIRMS_PROVIDERS.contains(dataLakeDto.getProvider());
    }

    @Override
    public Optional<NormalizedObservation> normalize(DataLake dataLakeDto) {
        var normalizedObservation = new NormalizedObservation();

        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setType(EventType.THERMAL_ANOMALY);
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setStartedAt(dataLakeDto.getUpdatedAt());

        String[] csvHeaderAndRow = dataLakeDto.getData().split("\n");
        ParsedNormalizationItem csvData =
                parseNormalizationRow(dataLakeDto.getProvider(), csvHeaderAndRow[0], csvHeaderAndRow[1]);
        if (csvData != null) {
            Double longitude = csvData.getLongitude();
            Double latitude = csvData.getLatitude();

            normalizedObservation.setPoint(makeWktPoint(longitude, latitude));

            String wktPolygon = createWktPolygon(longitude, latitude);
            normalizedObservation.setGeometries(createGeometry(wktPolygon));
        }
        return Optional.of(normalizedObservation);
    }

    private String createWktPolygon(Double longitude, Double latitude) {
        long h3Index = h3.geoToH3(latitude, longitude, 8);
        List<GeoCoord> h3Polygon = h3.h3ToGeoBoundary(h3Index);

        h3Polygon.add(h3Polygon.get(0));//wkt polygon must be closed

        return h3Polygon.stream()
                .map(geoCoord -> geoCoord.lng + " " + geoCoord.lat)
                .collect(Collectors.joining(",", "POLYGON ((", "))"));
    }

    private FeatureCollection createGeometry(String wktPolygon) {
        Geometry geometry;
        try {
            geometry = geoJSONWriter.write(wktReader.read(wktPolygon));
        } catch (ParseException e) {
            throw new RuntimeException("can not create Geometry", e);
        }

        return convertGeometryToFeatureCollection(geometry, FIRMS_PROPERTIES);
    }

}
