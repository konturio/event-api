package io.kontur.eventapi.stormsnoaa.normalization;

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
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob.STORMS_NOAA_PROVIDER;
import static io.kontur.eventapi.util.CsvUtil.parseRow;
import static io.kontur.eventapi.util.SeverityUtil.convertFujitaScale;

@Component
public class StormsNoaaNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(StormsNoaaNormalizer.class);
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
            .toFormatter()
            .withLocale(Locale.US);

    private static final Map<String, EventType> EVENT_TYPES_MAPPER = Map.ofEntries(
            Map.entry("Drought", EventType.DROUGHT),
            Map.entry("Flash Flood", EventType.FLOOD),
            Map.entry("Flood", EventType.FLOOD),
            Map.entry("HAIL FLOODING", EventType.FLOOD),
            Map.entry("Lakeshore Flood", EventType.FLOOD),
            Map.entry("THUNDERSTORM WINDS/ FLOOD", EventType.FLOOD),
            Map.entry("THUNDERSTORM WINDS/FLASH FLOOD", EventType.FLOOD),
            Map.entry("THUNDERSTORM WINDS/FLOODING", EventType.FLOOD),
            Map.entry("Marine Tropical Storm", EventType.STORM),
            Map.entry("Tropical Storm", EventType.STORM),
            Map.entry("Tornado", EventType.TORNADO),
            Map.entry("TORNADO/WATERSPOUT", EventType.TORNADO),
            Map.entry("TORNADOES, TSTM WIND, HAIL", EventType.TORNADO),
            Map.entry("Tsunami", EventType.TSUNAMI),
            Map.entry("Volcanic Ash", EventType.VOLCANO),
            Map.entry("Volcanic Ashfall", EventType.VOLCANO),
            Map.entry("Wildfires", EventType.WILDFIRE),
            Map.entry("Winter Storm", EventType.WINTER_STORM));

    private static final List<String> PROPERTY_FIELDS = List.of("STATE", "CZ_NAME", "MAGNITUDE",
            "TOR_WIDTH", "EVENT_TYPE", "TOR_LENGTH", "FLOOD_CAUSE", "TOR_F_SCALE", "DAMAGE_CROPS",
            "END_LOCATION", "DEATHS_DIRECT", "BEGIN_LOCATION", "MAGNITUDE_TYPE", "DAMAGE_PROPERTY",
            "DEATHS_INDIRECT", "EVENT_NARRATIVE", "INJURIES_DIRECT", "EPISODE_NARRATIVE", "INJURIES_INDIRECT");

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return dataLakeDto.getProvider().equals(STORMS_NOAA_PROVIDER);
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setActive(false);

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

        setGeometry(data, normalizedObservation);

        String eventType = parseString(data, "EVENT_TYPE");
        normalizedObservation.setType(EVENT_TYPES_MAPPER.getOrDefault(eventType, EventType.OTHER));

        String zone = parseString(data, "CZ_NAME");
        String state = parseString(data, "STATE");
        normalizedObservation.setName(createName(eventType, zone, state, "USA"));

        return normalizedObservation;
    }

    private void setGeometry(Map<String, String> data, NormalizedObservation normalizedObservation) {
        Double x1 = parseDouble(data, "BEGIN_LON");
        Double y1 = parseDouble(data,"BEGIN_LAT");
        Double x2 = parseDouble(data, "END_LON");
        Double y2 = parseDouble(data, "END_LAT");

        boolean startPointPresent = x1 != null && y1 != null;
        boolean endPointPresent = x2 != null && y2 != null;
        String point = startPointPresent ? makeWktPoint(x1, y1) : endPointPresent ? makeWktPoint(x2, y2) : null;
        String geom = startPointPresent && endPointPresent ? makeWktLine(x1, y1, x2, y2) : point;
        normalizedObservation.setPoint(point);
        try {
            Geometry geometry = geom == null ? null : geoJsonWriter.write(wktReader.read(geom));
            Feature feature = new Feature(geometry, createGeometryProperties(data));
            normalizedObservation.setGeometries(new FeatureCollection(new Feature[] {feature}).toString());
        } catch (ParseException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private Map<String, Object> createGeometryProperties(Map<String, String> data) {
        return data.entrySet()
                .stream()
                .filter(entry -> PROPERTY_FIELDS.contains(entry.getKey()))
                .map(entry -> Map.entry(entry.getKey().toLowerCase(), (Object) entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String createName(String eventType, String ... atu) {
        return eventType + " - " + Arrays.stream(atu)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(", "));
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
