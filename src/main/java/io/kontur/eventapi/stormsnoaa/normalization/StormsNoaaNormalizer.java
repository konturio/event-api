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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob.STORMS_NOAA_PROVIDER;
import static io.kontur.eventapi.util.CsvUtil.parseRow;
import static io.kontur.eventapi.util.SeverityUtil.convertFujitaScale;

@Component
public class StormsNoaaNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(StormsNoaaNormalizer.class);
    private static final GeoJSONWriter geoJsonWriter = new GeoJSONWriter();
    private static final WKTReader wktReader = new WKTReader();
    private final NormalizedObservationsDao normalizedObservationsDao;

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
            Map.entry("Drought", EventType.DROUGHT),
            Map.entry("Flash Flood", EventType.FLOOD),
            Map.entry("Flood", EventType.FLOOD),
            Map.entry("HAIL FLOODING", EventType.FLOOD),
            Map.entry("Lakeshore Flood", EventType.FLOOD),
            Map.entry("THUNDERSTORM WINDS/ FLOOD", EventType.FLOOD),
            Map.entry("THUNDERSTORM WINDS/FLASH FLOOD", EventType.FLOOD),
            Map.entry("THUNDERSTORM WINDS/FLOODING", EventType.FLOOD),
            Map.entry("Coastal Flood", EventType.FLOOD),
            Map.entry("Dust Storm", EventType.STORM),
            Map.entry("Tornado", EventType.TORNADO),
            Map.entry("TORNADO/WATERSPOUT", EventType.TORNADO),
            Map.entry("TORNADOES, TSTM WIND, HAIL", EventType.TORNADO),
            Map.entry("Dust Devil", EventType.TORNADO),
            Map.entry("Funnel Cloud", EventType.TORNADO),
            Map.entry("Waterspout", EventType.TORNADO),
            Map.entry("THUNDERSTORM WINDS FUNNEL CLOU", EventType.TORNADO),
            Map.entry("Hurricane", EventType.CYCLONE),
            Map.entry("Hurricane (Typhoon)", EventType.CYCLONE),
            Map.entry("Marine Hurricane/Typhoon", EventType.CYCLONE),
            Map.entry("Marine Tropical Storm", EventType.CYCLONE),
            Map.entry("Tropical Storm", EventType.CYCLONE),
            Map.entry("Marine Tropical Depression", EventType.CYCLONE),
            Map.entry("Tropical Depression", EventType.CYCLONE),
            Map.entry("Tsunami", EventType.TSUNAMI),
            Map.entry("Volcanic Ash", EventType.VOLCANO),
            Map.entry("Volcanic Ashfall", EventType.VOLCANO),
            Map.entry("Wildfire", EventType.WILDFIRE),
            Map.entry("Winter Storm", EventType.WINTER_STORM),
            Map.entry("Blizzard", EventType.WINTER_STORM));

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
        normalizedObservation.setCost(getCost(parseString(data, "DAMAGE_PROPERTY")));
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
            Feature feature = new Feature(geometry, Collections.emptyMap());
            normalizedObservation.setGeometries(new FeatureCollection(new Feature[] {feature}).toString());
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
