package io.kontur.eventapi.nhc.normalization;

import static io.kontur.eventapi.nhc.NhcUtil.*;
import static io.kontur.eventapi.util.GeometryUtil.*;
import static io.kontur.eventapi.util.SeverityUtil.*;
import static io.kontur.eventapi.util.SeverityUtil.WIND_SPEED_KPH;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.nhc.NhcUtil;
import io.kontur.eventapi.nhc.converter.NhcXmlParser;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.util.DateTimeUtil;
import io.kontur.eventapi.util.SeverityUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

@Component
public class NhcNormalizer extends Normalizer {

    private static final Logger LOG = LoggerFactory.getLogger(NhcNormalizer.class);

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return NHC_PROVIDERS.contains(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NhcXmlParser parser = new NhcXmlParser();
        Optional<CapParsedItem> parsedItem = parser.getParsedItem(dataLakeDto.getData());
        if (parsedItem.isPresent() && StringUtils.isNotBlank(parsedItem.get().getDescription())) {
            String description = parsedItem.get().getDescription();
            NormalizedObservation normalizedObservation = new NormalizedObservation();

            normalizedObservation.setObservationId(dataLakeDto.getObservationId());
            normalizedObservation.setProvider(dataLakeDto.getProvider());
            normalizedObservation.setType(EventType.CYCLONE);
            normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
            normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
            normalizedObservation.setUrls(parsedItem.get().getLink() != null ? List.of(parsedItem.get().getLink()) : null);
            normalizedObservation.setActive(null);

            Map<Integer, Map<Integer, String>> mainMatchers = parser.parseDescription(description);
            try {
                if (MapUtils.isNotEmpty(mainMatchers) && MapUtils.isNotEmpty(mainMatchers.get(1))) {
                    Map<Integer, String> mainMatches = mainMatchers.get(1);
                    Integer currentDay;
                    Integer currentMonth;
                    Integer currentYear;
                    OffsetDateTime currentDateTime;
                    if (StringUtils.isNotBlank(mainMatches.get(EVENT_ID_POS))
                            && StringUtils.isNotBlank(mainMatches.get(TYPE_POS))
                            && StringUtils.isNotBlank(mainMatches.get(NAME_POS))
                            && StringUtils.isNotBlank(mainMatches.get(ADV_NUMBER_POS)) ) {
                        normalizedObservation.setExternalEventId(mainMatches.get(EVENT_ID_POS));
                        normalizedObservation.setName(
                                (mainMatches.get(TYPE_POS) != null ? mainMatches.get(TYPE_POS).trim() : "")
                                + " " + (mainMatches.get(NAME_POS) != null ? mainMatches.get(NAME_POS).trim() : ""));
                        normalizedObservation.setDescription(mainMatches.get(NEWS_POS).trim());
                        normalizedObservation.setEpisodeDescription(mainMatches.get(NEWS_POS).trim());
                        normalizedObservation.setExternalEpisodeId(
                                (mainMatches.get(EVENT_ID_POS) != null ? mainMatches.get(EVENT_ID_POS).trim() : "") + "_"
                                        + (mainMatches.get(ADV_NUMBER_POS) != null
                                        ? mainMatches.get(ADV_NUMBER_POS).trim() : ""));
                        normalizedObservation.setProperName(mainMatches.get(NAME_POS).trim());
                    } else {
                        LOG.warn("Empty one of the base parameters for NHC cyclone (name, type, id, adv number). {}",
                                parsedItem.get().getDescription());
                        return null;
                    }
                    if (StringUtils.isNotBlank(mainMatches.get(CURRENT_TIME_POS))) {
                        try {
                            String time = mainMatches.get(CURRENT_TIME_POS).trim().replaceAll("UTC", "GMT");
                            String timeRfc = time.substring(9, 12) + ", " //day of week
                                    + time.substring(17, 19) + " " // day of month
                                    + time.substring(13, 16) + " " // month
                                    + time.substring(20) + " " // year
                                    + time.substring(0, 2) + ":" + time.substring(2, 4) + ":00 " // time
                                    + time.substring(5, 8); // timezone
                            currentDateTime = DateTimeUtil.parseDateTimeFromString(timeRfc);
                            normalizedObservation.setStartedAt(currentDateTime);
                            currentDay = currentDateTime.getDayOfMonth();
                            currentMonth = currentDateTime.getMonthValue();
                            currentYear = currentDateTime.getYear();
                        } catch (Exception e) {
                            LOG.warn("Can't parse current time for NHC cyclone. {}", parsedItem.get().getDescription());
                            return null;
                        }
                    } else {
                        LOG.warn("Empty current time for NHC cyclone. {}", parsedItem.get().getDescription());
                        return null;
                    }

                    Map<Integer, Map<Integer, String>> maxSustainedWind;
                    if (StringUtils.isNotBlank(mainMatches.get(MAX_SUSTAINED_WIND_POS))) {
                        maxSustainedWind = parser.parseByPattern(mainMatches.get(MAX_SUSTAINED_WIND_POS),
                                MAX_SUSTAINED_WIND_REGEXP);
                        if (MapUtils.isNotEmpty(maxSustainedWind) && MapUtils.isNotEmpty(maxSustainedWind.get(1))) {
                            try {
                                int maxWind = Integer.parseInt(maxSustainedWind.get(1).get(MAX_WIND_POS));
                                Double maxWindKph = NhcUtil.convertKnotsToKph((double) maxWind, 2);
                                String category = SeverityUtil.getCycloneCategory(maxWindKph);
                                normalizedObservation.setSeverityData(Map.of(WIND_SPEED_KPH, maxWindKph, CATEGORY_SAFFIR_SIMPSON, category));
                                if (category.equals(CATEGORY_TD)) {
                                    normalizedObservation.setEventSeverity(Severity.MINOR);
                                } else if (category.equals(CATEGORY_TS)) {
                                    normalizedObservation.setEventSeverity(Severity.MODERATE);
                                } else if (category.equals(CATEGORY_1)) {
                                    normalizedObservation.setEventSeverity(Severity.SEVERE);
                                } else {
                                    normalizedObservation.setEventSeverity(Severity.EXTREME);
                                }
                            } catch (Exception e) {
                                LOG.warn("Can't get max sustained wind speed from {}",
                                        mainMatches.get(MAX_SUSTAINED_WIND_POS));
                                return null;
                            }
                        } else {
                            LOG.warn("Can't parse max sustained wind for NHC cyclone. {}",
                                    mainMatches.get(MAX_SUSTAINED_WIND_POS));
                            return null;
                        }
                    } else {
                        LOG.warn("Empty max sustained wind for NHC cyclone. {}", parsedItem.get().getDescription());
                        return null;
                    }

                    List<Feature> featuresList = new ArrayList<>();

                    // Create geometry for cyclone's center point
                    if (StringUtils.isNotBlank(mainMatches.get(CENTER_POS))) {
                        Map<Integer, Map<Integer, String>> centerPointInfo =
                                parser.parseByPattern(mainMatches.get(CENTER_POS), CENTER_REGEXP);
                        Optional<Feature> centerPointFeature = prepareFeature(centerPointInfo, maxSustainedWind,
                                currentDay, currentMonth, currentYear, currentDateTime, CENTER_LAT_POS, CENTER_LONG_POS,
                                CENTER_DAY_POS, CENTER_HOURS_POS, CENTER_MINUTES_POS, MAX_WIND_POS, MAX_GUSTS_POS,
                                true);
                        if (centerPointFeature.isEmpty()) {
                            LOG.warn("Can't parse center location point for NHC cyclone. {}",
                                    mainMatches.get(CENTER_POS));
                            return null;
                        }
                        centerPointFeature.ifPresent(feature ->
                                prepareWindSections(parser, maxSustainedWind.get(1), MAX_WIND_64_POS, MAX_WIND_34_POS)
                                        .ifPresent(props -> feature.getProperties().putAll(props)));
                        centerPointFeature.ifPresent(featuresList::add);
                    } else {
                        LOG.warn("Empty center location point for NHC cyclone. {}", parsedItem.get().getDescription());
                        return null;
                    }
                    // Create geometries for cyclone's forecast points
                    Map<Integer, Map<Integer, String>> forecastsPointInfo =
                            parser.parseByPattern(mainMatches.get(FORECAST_POS), FORECAST_REGEXP);
                    if (MapUtils.isNotEmpty(forecastsPointInfo)) {
                        for (Integer idx : forecastsPointInfo.keySet()) {
                            Map<Integer, String> forecast = forecastsPointInfo.get(idx);
                            if (MapUtils.isNotEmpty(forecast)) {
                                Map<Integer, Map<Integer, String>> forecastInfo =
                                        parser.parseByPattern(forecast.get(1), WIND_SPEED_REGEXP);
                                Optional<Feature> forecastFeature = prepareFeature(forecastInfo, forecastInfo,
                                        currentDay, currentMonth, currentYear, currentDateTime, FORECAST_LAT_POS,
                                        FORECAST_LONG_POS, FORECAST_DAY_POS, FORECAST_HOURS_POS, FORECAST_MINUTES_POS,
                                        FORECAST_WIND_POS, FORECAST_GUSTS_POS, false);
                                forecastFeature.ifPresent(feature ->
                                        prepareWindSections(parser, forecastInfo.get(1), FORECAST_64_POS, FORECAST_34_POS)
                                                .ifPresent(props -> feature.getProperties().putAll(props)));
                                forecastFeature.ifPresent(featuresList::add);
                            }
                        }
                    }

                    // Create geometries for cyclone's outlook points
                    Map<Integer, Map<Integer, String>> outlookPointInfo =
                            parser.parseByPattern(mainMatches.get(OUTLOOK_POS), OUTLOOK_REGEXP);
                    if (MapUtils.isNotEmpty(outlookPointInfo)) {
                        for (Integer idx : outlookPointInfo.keySet()) {
                            Map<Integer, String> outlook = outlookPointInfo.get(idx);
                            if (MapUtils.isNotEmpty(outlook)) {
                                Map<Integer, Map<Integer, String>> outlookInfo =
                                        parser.parseByPattern(outlook.get(1), WIND_SPEED_REGEXP);
                                Optional<Feature> outlookFeature = prepareFeature(outlookInfo, outlookInfo,
                                        currentDay, currentMonth, currentYear, currentDateTime, FORECAST_LAT_POS,
                                        FORECAST_LONG_POS, FORECAST_DAY_POS, FORECAST_HOURS_POS, FORECAST_MINUTES_POS,
                                        FORECAST_WIND_POS, FORECAST_GUSTS_POS, false);
                                outlookFeature.ifPresent(featuresList::add);
                            }
                        }
                    }
                    normalizedObservation.setGeometries(new FeatureCollection(featuresList.toArray(new Feature[0])));
                } else {
                    LOG.warn("Can't parse description for NHC cyclone. {}", parsedItem.get().getDescription());
                    return null;
                }
            } catch (Exception e) {
                LOG.warn("Error while parsing NHC cyclone's description {}", parsedItem.get().getDescription());
                return null;
            }
            return normalizedObservation;
        } else {
            LOG.error("Can't parse input source for normalization for event: {}", dataLakeDto.getExternalId());
        }
        return null;
    }

    private Optional<Feature> prepareFeature(Map<Integer, Map<Integer, String>> info,
                                             Map<Integer, Map<Integer, String>> windInfo,
                                             Integer currentDay, Integer currentMonth, Integer currentYear,
                                             OffsetDateTime currentDateTime, Integer latPos, Integer longPos,
                                             Integer dayPos, Integer hoursPos, Integer minutesPos,
                                             Integer windPos, Integer gustsPos, Boolean isObserved) {
        if (MapUtils.isNotEmpty(info) && MapUtils.isNotEmpty(info.get(1))
                && MapUtils.isNotEmpty(windInfo) && MapUtils.isNotEmpty(windInfo.get(1))) {
            Map<String, Object> properties = new HashMap<>();
            Point point = preparePoint(info.get(1), latPos, longPos);

            Double maxWind = Double.valueOf(windInfo.get(1).get(windPos));
            properties.put(WIND_SPEED_KNOTS, maxWind);
            Double maxGusts = Double.valueOf(windInfo.get(1).get(gustsPos));
            Double maxWindKph = NhcUtil.convertKnotsToKph(maxWind, 2);
            properties.put(WIND_SPEED_KPH, maxWindKph);
            Double maxGustsKph = NhcUtil.convertKnotsToKph(maxGusts, 2);
            properties.put(WIND_GUSTS_KPH, maxGustsKph);

            properties.put(IS_OBSERVED_PROPERTY, isObserved);

            Optional<OffsetDateTime> dateTime = prepareForecastDateTime(info.get(1), currentDay,
                    currentMonth, currentYear, dayPos, hoursPos, minutesPos);
            dateTime.ifPresent(value -> {
                properties.put(TIMESTAMP_PROPERTY, value);
                if (Boolean.FALSE.equals(isObserved) && Duration.between(currentDateTime, value).toHours() > 0) {
                    properties.put(FORECAST_HRS_PROPERTY,
                            Duration.between(currentDateTime, value).toHours());
                }
            });

            return Optional.of(new Feature(point, properties));
        }
        return Optional.empty();
    }

    private Optional<Map<String, Object>> prepareWindSections(NhcXmlParser parser, Map<Integer, String> info,
                                                              Integer wind64pos, Integer wind34pos) {
        Map<String, Object> properties = new HashMap<>();
        for (int i = wind64pos; i <= wind34pos; i++) {
            String windInfoString = info.get(i);
            if (StringUtils.isNotBlank(windInfoString)) {
                Map<Integer, Map<Integer, String>> windInfo =
                        parser.parseByPattern(windInfoString.trim(), WIND_SECTIONS_REGEXP);
                if (MapUtils.isNotEmpty(windInfo) && MapUtils.isNotEmpty(windInfo.get(1))
                        && StringUtils.isNotBlank(windInfo.get(1).get(1))) {
                    String propName = windInfo.get(1).get(WIND_PROP_NAME_POS);
                    prepareWindSpeed(windInfo.get(1).get(WIND_NE_POS))
                            .ifPresent(value -> properties.put(propName + "_kt_NE", value));
                    prepareWindSpeed(windInfo.get(1).get(WIND_SE_POS))
                            .ifPresent(value -> properties.put(propName + "_kt_SE", value));
                    prepareWindSpeed(windInfo.get(1).get(WIND_SW_POS))
                            .ifPresent(value -> properties.put(propName + "_kt_SW", value));
                    prepareWindSpeed(windInfo.get(1).get(WIND_NW_POS))
                            .ifPresent(value -> properties.put(propName + "_kt_NW", value));
                }
            }
        }
        if (MapUtils.isNotEmpty(properties)) {
            return Optional.of(properties);
        }
        return Optional.empty();
    }
    private Optional<Double> prepareWindSpeed(String value) {
        if (StringUtils.isNotBlank(value)) {
            try {
                return Optional.of(NhcUtil.convertKnotsToKph(Double.valueOf(value), 0));
            } catch (Exception e) {
                LOG.warn("Error while parsing wind speed value {}", value);
            }
        }
        return Optional.empty();
    }

    private Optional<OffsetDateTime> prepareForecastDateTime(Map<Integer, String> info, Integer currentDay,
                                                             Integer currentMonth, Integer currentYear,
                                                             Integer dayPos, Integer hoursPos, Integer minutesPos) {
        if (MapUtils.isNotEmpty(info)) {
            String day = info.get(dayPos);
            String hours = info.get(hoursPos);
            String minutes = info.get(minutesPos);
            Integer month = currentMonth;
            Integer year = currentYear;
            try {
                if (currentDay != null && Integer.parseInt(day) < currentDay) {
                    month = currentMonth + 1;
                    if (month > 12) {
                        year = currentYear + 1;
                        month = 1;
                    }
                }
                String time = year + "-" + String.format("%02d", month) + "-" + day + "T" + hours + ":" + minutes + ":00Z";
                OffsetDateTime dateTime = DateTimeUtil.parseDateTimeByPattern(time, null);
                return dateTime != null ? Optional.of(dateTime) : Optional.empty();
            } catch (Exception e) {
                LOG.warn("Error while parsing forecast datetime {}", day + "/" + hours + minutes + "Z");
            }
        }
        return Optional.empty();
    }

    private Point preparePoint(Map<Integer, String> info, Integer latPos, Integer longPos) {
        String latStr = info.get(latPos);
        double latitude = Double.parseDouble(latStr.substring(0, latStr.length() - 1))
                * ("N".equalsIgnoreCase(latStr.substring(latStr.length() - 1)) ? 1 : -1);
        String longStr = info.get(longPos);
        double longitude = Double.parseDouble(longStr.substring(0, longStr.length() - 1))
                * ("E".equalsIgnoreCase(longStr.substring(longStr.length() - 1)) ? 1 : -1);
        return new Point(new double[] {longitude, latitude});
    }
}
