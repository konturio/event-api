package io.kontur.eventapi.stormsnoaa.normalization;

import io.kontur.eventapi.dao.NormalizedObservationsDao;
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
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;
import io.kontur.eventapi.util.LossUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob.STORMS_NOAA_PROVIDER;
import static io.kontur.eventapi.util.CsvUtil.parseRow;
import static io.kontur.eventapi.util.GeometryUtil.*;
import static io.kontur.eventapi.util.SeverityUtil.*;
import static io.kontur.eventapi.util.SeverityUtil.WIND_SPEED_KPH;

@Component
public class StormsNoaaNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(StormsNoaaNormalizer.class);
    private static final GeoJSONWriter geoJsonWriter = new GeoJSONWriter();
    private static final WKTReader wktReader = new WKTReader();
    private final NormalizedObservationsDao normalizedObservationsDao;

    public static final Map<String, Object> STORMS_NOAA_POSITION_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, POSITION, IS_OBSERVED_PROPERTY, true);
    public static final Map<String, Object> STORMS_NOAA_TRACK_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, TRACK, IS_OBSERVED_PROPERTY, true);

    private static final Map<String, BigDecimal> COST_UNITS = Map.of(
            "H", BigDecimal.valueOf(100),
            "h", BigDecimal.valueOf(100),
            "K", BigDecimal.valueOf(1000),
            "M", BigDecimal.valueOf(1000000),
            "B", BigDecimal.valueOf(1000000000));

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 1949)
            .appendPattern(" HH:mm:ss")
            .toFormatter()
            .withLocale(Locale.US);

    private static final Pattern zoneOffsetPattern = Pattern.compile("(-|\\+)?\\d+");

    private static final Map<String, EventType> EVENT_TYPES_MAPPER = Map.ofEntries(
            Map.entry("Thunderstorm Wind", EventType.STORM),
            Map.entry("Hail", EventType.STORM),
            Map.entry("Flash Flood", EventType.FLOOD),
            Map.entry("High Wind", EventType.STORM),
            Map.entry("Tornado", EventType.TORNADO),
            Map.entry("Winter Storm", EventType.WINTER_STORM),
            Map.entry("Drought", EventType.DROUGHT),
            Map.entry("Winter Weather", EventType.WINTER_STORM),
            Map.entry("Heavy Snow", EventType.WINTER_STORM),
            Map.entry("Flood", EventType.FLOOD),
            Map.entry("Marine Thunderstorm Wind", EventType.STORM),
            Map.entry("Heavy Rain", EventType.FLOOD),
            Map.entry("Strong Wind", EventType.STORM),
            Map.entry("Lightning", EventType.STORM),
            Map.entry("Blizzard", EventType.WINTER_STORM),
            Map.entry("Ice Storm", EventType.WINTER_STORM),
            Map.entry("High Surf", EventType.FLOOD),
            Map.entry("Funnel Cloud", EventType.TORNADO),
            Map.entry("Wildfire", EventType.WILDFIRE),
            Map.entry("Tropical Storm", EventType.CYCLONE),
            Map.entry("Waterspout", EventType.TORNADO),
            Map.entry("Coastal Flood", EventType.FLOOD),
            Map.entry("Lake-Effect Snow", EventType.WINTER_STORM),
            Map.entry("Hurricane (Typhoon)", EventType.CYCLONE),
            Map.entry("Dust Storm", EventType.STORM),
            Map.entry("Storm Surge/Tide", EventType.FLOOD),
            Map.entry("Marine Hail", EventType.STORM),
            Map.entry("Marine High Wind", EventType.STORM),
            Map.entry("Sleet", EventType.WINTER_STORM),
            Map.entry("Marine Tropical Storm", EventType.CYCLONE),
            Map.entry("Tropical Depression", EventType.CYCLONE),
            Map.entry("Freezing Fog", EventType.WINTER_STORM),
            Map.entry("Hurricane", EventType.CYCLONE),
            Map.entry("Lakeshore Flood", EventType.FLOOD),
            Map.entry("Marine Strong Wind", EventType.STORM),
            Map.entry("Dense Smoke", EventType.WILDFIRE),
            Map.entry("Marine Hurricane/Typhoon", EventType.CYCLONE),
            Map.entry("Volcanic Ashfall", EventType.VOLCANO),
            Map.entry("Seiche", EventType.FLOOD),
            Map.entry("Volcanic Ash", EventType.VOLCANO),
            Map.entry("Sneakerwave", EventType.FLOOD),
            Map.entry("Tsunami", EventType.TSUNAMI),
            Map.entry("Marine Tropical Depression", EventType.CYCLONE));

    public StormsNoaaNormalizer(NormalizedObservationsDao normalizedObservationsDao) {
        this.normalizedObservationsDao = normalizedObservationsDao;
    }

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
        BigDecimal propertyDamage = getCost(parseString(data, "DAMAGE_PROPERTY"));
        BigDecimal cropsDamage = getCost(parseString(data, "DAMAGE_CROPS"));
        List<Map<String, Object>> costs = new ArrayList<>();
        if (propertyDamage != null) {
            costs.add(Map.of("damage_property_cost", propertyDamage));
        }
        if (cropsDamage != null) {
            costs.add(Map.of("damage_crops_cost", cropsDamage));
        }
        if (!costs.isEmpty()) {
            normalizedObservation.setCost(costs);
        }
        Map<String, Object> loss = new HashMap<>();
        if (propertyDamage != null) {
            loss.put(LossUtil.PROPERTY_DAMAGE, propertyDamage);
        }
        if (cropsDamage != null) {
            loss.put(LossUtil.CROPS_DAMAGE, cropsDamage);
        }
        int dead = parseInt(data, "DEATHS_DIRECT") + parseInt(data, "DEATHS_INDIRECT");
        if (dead > 0) {
            loss.put(LossUtil.PEOPLE_DEAD, dead);
        }
        int injured = parseInt(data, "INJURIES_DIRECT") + parseInt(data, "INJURIES_INDIRECT");
        if (injured > 0) {
            loss.put(LossUtil.PEOPLE_INJURED, injured);
        }
        normalizedObservation.setLoss(loss);
        normalizedObservation.setEventSeverity(convertFujitaScale(parseString(data, "TOR_F_SCALE")));

        setGeometry(data, normalizedObservation);

        String timezone = parseTimezone(parseString(data, "CZ_TIMEZONE"));
        String startedAt = parseString(data, "BEGIN_DATE_TIME");
        String endedAt = parseString(data, "END_DATE_TIME");
        normalizedObservation.setStartedAt(convertDate(startedAt != null ? startedAt : endedAt, timezone));
        normalizedObservation.setEndedAt(convertDate(endedAt != null ? endedAt : startedAt, timezone));

        String eventType = parseString(data, "EVENT_TYPE");
        normalizedObservation.setType(EVENT_TYPES_MAPPER.getOrDefault(eventType, EventType.OTHER));

        String zone = parseString(data, "CZ_NAME");
        String state = parseString(data, "STATE");
        normalizedObservation.setName(createName(eventType, zone, state, "USA"));

        normalizedObservation.setSeverityData(getSeverityData(data, normalizedObservation.getType()));

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
        normalizedObservation.setPoint(point);
        try {
            if (startPointPresent && endPointPresent) {
                Geometry geometry = geoJsonWriter.write(wktReader.read(makeWktLine(x1, y1, x2, y2)));
                normalizedObservation.setGeometries(convertGeometryToFeatureCollection(geometry, STORMS_NOAA_TRACK_PROPERTIES));
            } else if (point != null) {
                Geometry geometry = geoJsonWriter.write(wktReader.read(point));
                normalizedObservation.setGeometries(convertGeometryToFeatureCollection(geometry, STORMS_NOAA_POSITION_PROPERTIES));
            }
        } catch (ParseException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private String createName(String eventType, String ... atu) {
        return eventType + " - " + Arrays.stream(atu)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private BigDecimal getCost(String damageProperty) {
        try {
            if (StringUtils.isBlank(damageProperty)) return null;
            String unitStr = RegExUtils.removePattern(damageProperty, "[0-9\\.]");
            String costStr = RegExUtils.removePattern(damageProperty, "[a-zA-Z]");
            BigDecimal unitValue = COST_UNITS.getOrDefault(unitStr, BigDecimal.ONE);
            BigDecimal costValue = costStr.isBlank() ? BigDecimal.ONE : NumberUtils.createBigDecimal(costStr);
            return costValue.multiply(unitValue);
        } catch (Exception e) {
            LOG.warn("Couldn't parse cost from DAMAGE_PROPERTY: " + damageProperty);
            return null;
        }
    }

    private Map<String, Object> getSeverityData(Map<String, String> map, EventType type) {
        Map<String, Object> severityData = new HashMap<>();
        Double magnitude = parseDouble(map, "MAGNITUDE");
        if (magnitude != null && magnitude > 0) {
            String magnitudeType = parseString(map, "MAGNITUDE_TYPE");
            if (magnitudeType == null) {
                // Inches to mm
                severityData.put(HAIL_SIZE_MM, magnitude * 25.4);
            } else if ("ES".equals(magnitudeType) || "MS".equals(magnitudeType)) {
                // Knots to km/h
                double windSpeedKph = magnitude * 1.852;
                severityData.put(WIND_SPEED_KPH, windSpeedKph);
                if (EventType.CYCLONE == type) {
                    severityData.put(CATEGORY_SAFFIR_SIMPSON, getCycloneCategory(windSpeedKph));
                }
            } else if ("EG".equals(magnitudeType) || "MG".equals(magnitudeType)
                    || "E".equals(magnitudeType) || "M".equals(magnitudeType)) {
                severityData.put(WIND_GUST_KPH, magnitude * 1.852);
            } else {
                LOG.warn("Unknown magnitude type from noaa: {}", magnitudeType);
            }
        }
        String cause = parseString(map, "FLOOD_CAUSE");
        if (cause != null) {
            severityData.put(CAUSE, cause);
        }
        String fujitaScale = parseString(map, "TOR_F_SCALE");
        if (fujitaScale != null) {
            severityData.put(FUJITA_SCALE, fujitaScale);
        }

        Double tornadoLength = parseDouble(map, "TOR_LENGTH");
        if (tornadoLength != null) {
            // miles to km
            severityData.put(TORNADO_LENGTH_KM, tornadoLength * 1.60934);
        }

        Double tornadoWidth = parseDouble(map, "TOR_WIDTH");
        if (tornadoWidth != null) {
            // yards to m
            severityData.put(TORNADO_WIDTH_M, tornadoWidth * 0.9144);
        }
        return severityData;
    }

    private String parseString(Map<String, String> map, String key) {
        String value = map.get(key);
        return StringUtils.isBlank(value) ? null : value;
    }

    private Double parseDouble(Map<String, String> map, String key) {
        String value = parseString(map, key);
        return value == null ? null : Double.valueOf(value);
    }

    private int parseInt(Map<String, String> map, String key) {
        String value = parseString(map, key);
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String parseTimezone(String timezoneAbbr) {
        if (timezoneAbbr == null || timezoneAbbr.isBlank()) {
            return "UTC";
        }
        if (zoneOffsetPattern.matcher(timezoneAbbr).find()) {
            return RegExUtils.removePattern(timezoneAbbr, zoneOffsetPattern.pattern());
        }
        return timezoneAbbr;
    }

    private OffsetDateTime convertDate(String date, String timezone) {
        LocalDateTime timestamp = LocalDateTime.parse(date, formatter);
        try {
            return normalizedObservationsDao.getTimestampAtTimezone(timestamp, timezone);
        } catch (Exception e) {
            return OffsetDateTime.of(timestamp, ZoneOffset.UTC);
        }
    }
}
