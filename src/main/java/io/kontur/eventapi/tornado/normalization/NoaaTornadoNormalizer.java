package io.kontur.eventapi.tornado.normalization;

import io.kontur.eventapi.entity.NormalizedObservation;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.tornado.job.NoaaTornadoImportJob.TORNADO_NOAA_PROVIDER;
import static io.kontur.eventapi.tornado.converter.TornadoDateConverter.parseDateTime;
import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class NoaaTornadoNormalizer extends TornadoNormalizer {

    private final static Logger LOG = LoggerFactory.getLogger(NoaaTornadoNormalizer.class);

    private final static String COUNTRY = "USA";
    private final static Map<String, BigDecimal> COST_UNITS = Map.of(
            "K", BigDecimal.valueOf(1000),
            "M", BigDecimal.valueOf(1000000));

    private final static WKTReader wktReader = new WKTReader();
    private final static GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    @Override
    protected void setDataFields(String data, NormalizedObservation normalizedObservation) {
        String[] csvHeaderAndRow = data.split("\n");
        Map<String, String> dataMap = parseRow(csvHeaderAndRow[0], csvHeaderAndRow[1]);

        normalizedObservation.setExternalEventId(StringUtils.defaultIfBlank(dataMap.get("EPISODE_ID"), null));
        normalizedObservation.setExternalEpisodeId(StringUtils.defaultIfBlank(dataMap.get("EVENT_ID"), null));
        normalizedObservation.setDescription(dataMap.get("EPISODE_NARRATIVE"));
        normalizedObservation.setEpisodeDescription(dataMap.get("EVENT_NARRATIVE"));

        String fujitaScale = StringUtils.getDigits(dataMap.get("TOR_F_SCALE"));
        normalizedObservation.setEventSeverity(convertSeverity(fujitaScale));

        BigDecimal cost = parseCostWithUnit(dataMap.get("DAMAGE_PROPERTY"));
        normalizedObservation.setCost(cost);

        OffsetDateTime startedAt = parseDateTime(dataMap.get("BEGIN_DATE_TIME"), normalizedObservation.getProvider());
        normalizedObservation.setStartedAt(startedAt);
        OffsetDateTime endedAt = parseDateTime(dataMap.get("END_DATE_TIME"), normalizedObservation.getProvider());
        normalizedObservation.setEndedAt(endedAt);

        String cz_name = dataMap.get("CZ_NAME");
        String state = dataMap.get("STATE");
        normalizedObservation.setName(createName(cz_name, state, COUNTRY));
    }

    @Override
    protected void setGeometry(String data, NormalizedObservation normalizedObservation) {
        String[] csvHeaderAndRow = data.split("\n");
        Map<String, String> dataMap = parseRow(csvHeaderAndRow[0], csvHeaderAndRow[1]);

        Double startLatitude = parseDouble(dataMap.get("BEGIN_LAT"));
        Double startLongitude = parseDouble(dataMap.get("BEGIN_LON"));
        Double endLatitude = parseDouble(dataMap.get("END_LAT"));
        Double endLongitude = parseDouble(dataMap.get("END_LON"));

        boolean startPointPresent = checkPoint(startLongitude, startLatitude);
        boolean endPointPresent = checkPoint(endLongitude, endLatitude);

        if (!startPointPresent && !endPointPresent) return;

        String wktPoint = startPointPresent
                ? makeWktPoint(startLongitude, startLatitude)
                : makeWktPoint(endLongitude, endLatitude);
        String wktGeometry = startPointPresent && endPointPresent
                ? makeWktLineString(startLongitude, startLatitude, endLongitude, endLatitude)
                : wktPoint;

        normalizedObservation.setPoint(wktPoint);
        normalizedObservation.setGeometries(createGeometries(wktGeometry));
    }

    @Override
    protected List<String> getProviders() {
        return Collections.singletonList(TORNADO_NOAA_PROVIDER);
    }

    private BigDecimal parseCostWithUnit(String costString) {
        if (StringUtils.isBlank(costString)) {
            return null;
        }
        String unit = RegExUtils.removePattern(costString, "[0-9\\.]");
        BigDecimal multiplyValue = COST_UNITS.getOrDefault(unit, BigDecimal.ONE);
        String cost = StringUtils.defaultIfBlank(RegExUtils.removePattern(costString, "[a-zA-Z]"), "1");
        return NumberUtils.createBigDecimal(cost).multiply(multiplyValue);
    }

    private String createGeometries(String wkt) {
        try {
            Geometry geometry = geoJSONWriter.write(wktReader.read(wkt));
            Feature feature = new Feature(geometry, Collections.emptyMap());
            return new FeatureCollection(new Feature[] {feature}).toString();
        } catch (ParseException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    private boolean checkPoint(Double lon, Double lat) {
        return lon != null && lat != null;
    }
}