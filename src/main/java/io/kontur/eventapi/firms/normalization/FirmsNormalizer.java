package io.kontur.eventapi.firms.normalization;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.kontur.eventapi.firms.FirmsUtil.*;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class FirmsNormalizer extends Normalizer {
    private final static List<String> FIRMS_PROVIDERS = List.of(MODIS_PROVIDER, SUOMI_PROVIDER, NOAA_PROVIDER);

    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();
    private final H3Core h3;

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
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        var normalizedObservation = new NormalizedObservation();

        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setType(EventType.WILDFIRE);
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setStartedAt(dataLakeDto.getUpdatedAt());

        String[] csvHeaderAndRow = dataLakeDto.getData().split("\n");
        Map<String, String> csvData = parseRow(csvHeaderAndRow[0], csvHeaderAndRow[1]);
        Double longitude = Double.valueOf(csvData.get("longitude"));
        Double latitude = Double.valueOf(csvData.get("latitude"));

        normalizedObservation.setPoint(makeWktPoint(longitude, latitude));

        String wrtPolygon = createWrtPolygon(longitude, latitude);
        String geometry = writeJson(createGeometry(wrtPolygon, dataLakeDto.getUpdatedAt()));
        normalizedObservation.setGeometries(geometry);

        return normalizedObservation;
    }

    private String createWrtPolygon(Double longitude, Double latitude) {
        List<GeoCoord> h3Polygon = h3.h3ToGeoBoundary(h3.geoToH3(latitude, longitude, 8));

        h3Polygon.add(h3Polygon.get(0));//wrt polygon must be closed
        String wrtPolygon = h3Polygon.stream()
                .map(geoCoord -> geoCoord.lng + " " + geoCoord.lat)
                .collect(Collectors.joining(",", "POLYGON ((", "))"));

        return wrtPolygon;
    }

    private FeatureCollection createGeometry(String wrtPolygon, OffsetDateTime updatedAt) {
        Geometry geometry;
        try {
            geometry = geoJSONWriter.write(wktReader.read(wrtPolygon));
        } catch (ParseException e) {
            throw new RuntimeException("can not create Geometry", e);
        }

        Feature feature = new Feature(geometry, Map.of("updatedAt", updatedAt));

        return new FeatureCollection(new Feature[]{feature});
    }

}
