package io.kontur.eventapi.jtwc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.jtwc.JtwcUtil;
import io.kontur.eventapi.normalization.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.kontur.eventapi.util.GeometryUtil.*;
import static io.kontur.eventapi.util.SeverityUtil.*;

@Component
public class JtwcNormalizer extends Normalizer {

    private static final Logger LOG = LoggerFactory.getLogger(JtwcNormalizer.class);

    private static final Pattern MAIN_PATTERN = Pattern.compile("1\\.\\s*([A-Z \\-/]+)\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*WARNING NR\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern POSITION_PATTERN = Pattern.compile("WARNING POSITION:\s*(\\d{1,2}/\\d{4}Z) --- NEAR ([0-9.]+[NS]) ([0-9.]+[EW])", Pattern.CASE_INSENSITIVE);
    private static final Pattern WIND_PATTERN = Pattern.compile("MAX SUSTAINED WINDS - (\\d+) KT, GUSTS (\\d+) KT", Pattern.CASE_INSENSITIVE);
    private static final Pattern RADIUS_PATTERN = Pattern.compile("RADIUS OF (\\d{3}) KT WINDS - (\\d+) NM NORTHEAST QUADRANT\\s+(\\d+) NM SOUTHEAST QUADRANT\\s+(\\d+) NM SOUTHWEST QUADRANT\\s+(\\d+) NM NORTHWEST QUADRANT", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORECAST_PATTERN = Pattern.compile("(\\d+) HRS, VALID AT:\s*(\\d{1,2}/\\d{4}Z) --- ([0-9.]+[NS]) ([0-9.]+[EW]).*?MAX SUSTAINED WINDS - (\\d+) KT, GUSTS (\\d+) KT", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMARKS_PATTERN = Pattern.compile("REMARKS:\s*(.*?)\\s*//", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return JtwcUtil.JTWC_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation observation = new NormalizedObservation();
        observation.setObservationId(dataLakeDto.getObservationId());
        observation.setProvider(dataLakeDto.getProvider());
        observation.setType(EventType.CYCLONE);
        observation.setLoadedAt(dataLakeDto.getLoadedAt());
        observation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        observation.setActive(null);

        String text = dataLakeDto.getData().replaceAll("\r", " ").replaceAll("\n", " ");
        text = text.replaceAll("\\s+", " ").trim();

        Matcher mainMatcher = MAIN_PATTERN.matcher(text);
        if (mainMatcher.find()) {
            String type = mainMatcher.group(1).trim();
            String eventId = mainMatcher.group(2).trim();
            String name = mainMatcher.group(3).trim();
            String adv = mainMatcher.group(4).trim();

            observation.setExternalEventId(eventId);
            observation.setExternalEpisodeId(eventId + "_" + adv);
            observation.setName(type + " " + name);
            observation.setProperName(name);
        } else {
            LOG.warn("Can't parse main info for JTWC cyclone. {}", text);
            return null;
        }

        Matcher descMatcher = REMARKS_PATTERN.matcher(dataLakeDto.getData());
        if (descMatcher.find()) {
            String desc = descMatcher.group(1).trim();
            observation.setDescription(desc);
            observation.setEpisodeDescription(desc);
        }

        List<Feature> features = new ArrayList<>();

        Matcher posMatcher = POSITION_PATTERN.matcher(text);
        if (posMatcher.find()) {
            OffsetDateTime time = parseTime(posMatcher.group(1), dataLakeDto.getUpdatedAt());
            Point point = parsePoint(posMatcher.group(2), posMatcher.group(3));
            Map<String, Object> props = new HashMap<>();
            props.put(AREA_TYPE_PROPERTY, POSITION);
            props.put(IS_OBSERVED_PROPERTY, true);
            if (time != null) {
                props.put(TIMESTAMP_PROPERTY, time);
            }
            features.add(new Feature(point, props));
            observation.setStartedAt(time);
        }

        Matcher windMatcher = WIND_PATTERN.matcher(text);
        if (windMatcher.find()) {
            int wind = Integer.parseInt(windMatcher.group(1));
            int gust = Integer.parseInt(windMatcher.group(2));
            double windKph = JtwcUtil.convertKnotsToKph((double) wind, 2);
            double gustKph = JtwcUtil.convertKnotsToKph((double) gust, 2);
            Map<String, Object> severityData = new HashMap<>();
            severityData.put(WIND_SPEED_KPH, windKph);
            severityData.put(WIND_GUST_KPH, gustKph);
            observation.setSeverityData(severityData);
            if (wind <= 33) {
                observation.setEventSeverity(Severity.MINOR);
            } else if (wind <= 63) {
                observation.setEventSeverity(Severity.MODERATE);
            } else if (wind <= 82) {
                observation.setEventSeverity(Severity.SEVERE);
            } else {
                observation.setEventSeverity(Severity.EXTREME);
            }
        }

        Matcher radiusMatcher = RADIUS_PATTERN.matcher(text);
        while (radiusMatcher.find()) {
            int speed = Integer.parseInt(radiusMatcher.group(1));
            int ne = Integer.parseInt(radiusMatcher.group(2));
            int se = Integer.parseInt(radiusMatcher.group(3));
            int sw = Integer.parseInt(radiusMatcher.group(4));
            int nw = Integer.parseInt(radiusMatcher.group(5));
            Map<String, Object> props = new HashMap<>();
            props.put(AREA_TYPE_PROPERTY, ALERT_AREA);
            props.put(WIND_SPEED_KPH, JtwcUtil.convertKnotsToKph((double) speed, 0));
            props.put(IS_OBSERVED_PROPERTY, true);
            props.put("direction", "NE");
            props.put("radiusKm", JtwcUtil.convertKnotsToKph((double) ne, 0));
            features.add(new Feature(null, props));
        }

        Matcher forecastMatcher = FORECAST_PATTERN.matcher(text);
        while (forecastMatcher.find()) {
            int hrs = Integer.parseInt(forecastMatcher.group(1));
            OffsetDateTime time = parseTime(forecastMatcher.group(2), dataLakeDto.getUpdatedAt());
            Point point = parsePoint(forecastMatcher.group(3), forecastMatcher.group(4));
            Map<String, Object> props = new HashMap<>();
            props.put(AREA_TYPE_PROPERTY, POSITION);
            props.put(IS_OBSERVED_PROPERTY, false);
            props.put(FORECAST_HRS_PROPERTY, hrs);
            if (time != null) {
                props.put(TIMESTAMP_PROPERTY, time);
            }
            props.put(WIND_SPEED_KPH, JtwcUtil.convertKnotsToKph(Double.valueOf(forecastMatcher.group(5)), 2));
            props.put(WIND_GUST_KPH, JtwcUtil.convertKnotsToKph(Double.valueOf(forecastMatcher.group(6)), 2));
            features.add(new Feature(point, props));
        }

        if (!features.isEmpty()) {
            observation.setGeometries(new FeatureCollection(features.toArray(new Feature[0])));
        }

        return observation;
    }

    private static OffsetDateTime parseTime(String ddhhmm, OffsetDateTime updatedAt) {
        try {
            String[] parts = ddhhmm.replace("Z", "").split("/");
            int day = Integer.parseInt(parts[0]);
            String hm = parts[1];
            int hour = Integer.parseInt(hm.substring(0, 2));
            int minute = Integer.parseInt(hm.substring(2));
            return OffsetDateTime.of(updatedAt.getYear(), updatedAt.getMonthValue(), day, hour, minute, 0, 0, ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
    }

    private static Point parsePoint(String latStr, String lonStr) {
        double lat = Double.parseDouble(latStr.substring(0, latStr.length() - 1));
        if (latStr.endsWith("S")) {
            lat = -lat;
        }
        double lon = Double.parseDouble(lonStr.substring(0, lonStr.length() - 1));
        if (lonStr.endsWith("W")) {
            lon = -lon;
        }
        return new Point(new double[]{lon, lat});
    }
}
