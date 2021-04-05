package io.kontur.eventapi.noaatornado.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.Map;

import static io.kontur.eventapi.noaatornado.job.NoaaTornadoImportJob.NOAA_TORNADO_PROVIDER;
import static io.kontur.eventapi.util.CsvUtil.parseRow;
import static io.kontur.eventapi.util.SeverityUtil.convertFujitaScale;

@Component
public class NoaaTornadoNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(NoaaTornadoNormalizer.class);
    private static final GeoJSONWriter geoJsonWriter = new GeoJSONWriter();
    private static final WKTReader wktReader = new WKTReader();

    private static final Map<String, BigDecimal> COST_UNITS = Map.of(
            "K", BigDecimal.valueOf(1000),
            "M", BigDecimal.valueOf(1000000));

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 1949)
            .appendPattern(" HH:mm:ss")
            .toFormatter();

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return dataLakeDto.getProvider().equals(NOAA_TORNADO_PROVIDER);
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setActive(false);
        normalizedObservation.setType(EventType.TORNADO);

        String[] csvHeaderAndRow = dataLakeDto.getData().split("\n");
        Map<String, String> data = parseRow(csvHeaderAndRow[0], csvHeaderAndRow[1]);

        normalizedObservation.setExternalEventId(parseString(data, "EPISODE_ID"));
        normalizedObservation.setExternalEpisodeId(parseString(data, "EVENT_ID"));
        normalizedObservation.setDescription(parseString(data, "EPISODE_NARRATIVE"));
        normalizedObservation.setEpisodeDescription(parseString(data, "EVENT_NARRATIVE"));
        normalizedObservation.setCost(getCost(parseString(data, "DAMAGE_PROPERTY")));
        normalizedObservation.setStartedAt(parseDate(data, "BEGIN_DATE_TIME"));
        normalizedObservation.setEndedAt(parseDate(data, "END_DATE_TIME"));

        String fujitaScale = StringUtils.getDigits(parseString(data, "TOR_F_SCALE"));
        normalizedObservation.setEventSeverity(convertFujitaScale(fujitaScale));

        Double startLon = parseDouble(data, "BEGIN_LON");
        Double startLat = parseDouble(data,"BEGIN_LAT");
        Double endLon = parseDouble(data, "END_LON");
        Double endLat = parseDouble(data, "END_LAT");
        setGeometry(startLon, startLat, endLon, endLat, normalizedObservation);

        String zone = parseString(data, "CZ_NAME");
        String state = parseString(data, "STATE");
        String name = "Tornado - " + (zone == null ? "" : zone + ", ") + (state == null ? "" : state + ", ") + "USA";
        normalizedObservation.setName(name);

        return normalizedObservation;
    }

    private void setGeometry(Double x1, Double y1, Double x2, Double y2, NormalizedObservation normalizedObservation) {
        boolean startPointPresent = x1 != null && y1 != null;
        boolean endPointPresent = x2 != null && y2 != null;
        if (!startPointPresent && !endPointPresent) return;
        String point = startPointPresent ? makeWktPoint(x1, y1) : makeWktPoint(x2, y2);
        String geom = startPointPresent && endPointPresent ? makeWktLine(x1, y1, x2, y2) : point;
        normalizedObservation.setPoint(point);
        try {
            Geometry geometry = geoJsonWriter.write(wktReader.read(geom));
            Feature feature = new Feature(geometry, Collections.emptyMap());
            normalizedObservation.setGeometries(new FeatureCollection(new Feature[] {feature}).toString());
        } catch (ParseException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private BigDecimal getCost(String damageProperty) {
        if (StringUtils.isBlank(damageProperty)) return null;
        String unitStr = RegExUtils.removePattern(damageProperty, "[0-9\\.]");
        String costStr = RegExUtils.removePattern(damageProperty, "[a-zA-Z]");
        BigDecimal unitValue = COST_UNITS.getOrDefault(unitStr, BigDecimal.ONE);
        BigDecimal costValue = costStr.isBlank() ? BigDecimal.ONE : NumberUtils.createBigDecimal(costStr);
        return costValue.multiply(unitValue);
    }

    private String parseString(Map<String, String> map, String key) {
        String value = map.get(key);
        return StringUtils.isBlank(value) ? null : value;
    }

    private Double parseDouble(Map<String, String> map, String key) {
        String value = parseString(map, key);
        return value == null ? null : Double.valueOf(value);
    }

    private OffsetDateTime parseDate(Map<String, String> map, String key) {
        String value = parseString(map, key);
        return value == null ? null : OffsetDateTime.of(LocalDateTime.parse(value, formatter), ZoneOffset.UTC);
    }

    private String makeWktLine(Double lon1, Double lat1, Double lon2, Double lat2) {
        return String.format("LINESTRING(%s %s, %s %s)", lon1, lat1, lon2, lat2);
    }
}
