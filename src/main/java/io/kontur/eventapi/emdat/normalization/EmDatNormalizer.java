package io.kontur.eventapi.emdat.normalization;

import io.kontur.eventapi.emdat.jobs.EmDatImportJob;
import io.kontur.eventapi.emdat.normalization.converter.EmDatSeverityConverter;
import io.kontur.eventapi.emdat.service.EmDatNormalizationService;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.job.Applicable;
import io.kontur.eventapi.normalization.Normalizer;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class EmDatNormalizer extends Normalizer {

    private static final Logger LOG = LoggerFactory.getLogger(EmDatNormalizer.class);

    private static final Map<String, EventType> typeMap = Map.of(
            "Drought", EventType.DROUGHT,
            "Earthquake", EventType.EARTHQUAKE,
            "Flood", EventType.FLOOD,
            "Storm", EventType.STORM,
            "Tornado", EventType.TORNADO,
            "Tropical cyclone", EventType.CYCLONE,
            "Tsunami", EventType.TSUNAMI,
            "Volcanic activity", EventType.VOLCANO,
            "Wildfire", EventType.WILDFIRE,
            "Winter storm/Blizzard", EventType.WINTER_STORM
    );

    private final List<EmDatSeverityConverter> severityConverters;
    private final EmDatNormalizationService normalizationService;
    private final WKTReader wktReader = new WKTReader();
    ;

    public EmDatNormalizer(
            List<EmDatSeverityConverter> severityConverters,
            EmDatNormalizationService normalizationService) {
        this.severityConverters = severityConverters;
        this.normalizationService = normalizationService;
    }

    @Override
    public boolean isApplicable(DataLake dataLake) {
        return EmDatImportJob.EM_DAT_PROVIDER.equals(dataLake.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLake) {
        Map<String, String> csvData = getCsvDataMap(dataLake);
        NormalizedObservation obs = new NormalizedObservation();
        obs.setObservationId(dataLake.getObservationId());
        obs.setExternalEventId(dataLake.getExternalId());
        obs.setProvider(dataLake.getProvider());
        obs.setType(defineEventType(csvData.get("Disaster Type"), csvData.get("Disaster Subtype"),
                csvData.get("Disaster Subsubtype")));
        obs.setLoadedAt(dataLake.getLoadedAt());
        obs.setSourceUpdatedAt(dataLake.getUpdatedAt());
        obs.setStartedAt(getExtractStartedAtValue(csvData, dataLake));
        obs.setEndedAt(getExtractEndedAtValue(csvData, dataLake));
        EmDatSeverityConverter severityConverter = Applicable.get(severityConverters, obs.getType());
        obs.setEventSeverity(severityConverter.defineSeverity(csvData));
        obs.setActive(false);
        obs.setName(makeName(csvData));
        obs.setRegion(csvData.get("ISO") + " " + csvData.get("Location"));
        if (!StringUtils.isEmpty(csvData.get("Total Damages ('000 US$)"))) {
            try {
                obs.setCost(new BigDecimal(csvData.get("Total Damages ('000 US$)")));
            } catch (NumberFormatException e) {
                LOG.debug(String.format("'%s' for observation %s", e.getMessage(), obs.getObservationId()));
            }
        }

        normalizationService
                .obtainGeometries(csvData.get("Country"), csvData.get("Location"))
                .map(geometry -> {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("country", csvData.get("Country"));
                    properties.put("regions", csvData.get("Location"));
                    properties.put("name", obs.getName());
                    properties.put("severity", obs.getEventSeverity());
                    properties.put("type", obs.getType());
                    properties.put("injured", csvData.get("No Injured"));
                    properties.put("affected", csvData.get("No Affected"));
                    properties.put("deaths", csvData.get("Total Deaths"));
                    properties.put("homeless", csvData.get("No Homeless"));
                    properties.put("total_affected", csvData.get("Total Affected"));
                    properties.put("reconstruction_costs", csvData.get("Reconstruction Costs"));
                    properties.put("total_damages", csvData.get("Total Damages"));
                    properties.put("dis_mag_scale", csvData.get("Dis Mag Scale"));
                    properties.put("dis_mag_value", csvData.get("Dis Mag Value"));
                    return new Feature(geometry, properties);
                })
                //TODO add affected population
                .map(f -> new Feature[]{f})
                .map(FeatureCollection::new)
                .ifPresent(fc -> obs.setGeometries(fc.toString()));

        if (!StringUtils.isEmpty(csvData.get("Latitude")) && !StringUtils.isEmpty(csvData.get("Longitude"))) {
            try {
                String point = makeWktPoint(Double.parseDouble(csvData.get("Longitude")),
                        Double.parseDouble(csvData.get("Latitude")));
                wktReader.read(point); //validate coordinates
                obs.setPoint(point);
            } catch (NumberFormatException | ParseException e) {
                LOG.debug(String.format("'%s' for observation %s", e.getMessage(), obs.getObservationId()));
            }
        }
//        obs.setDescription();
//        obs.setEpisodeDescription();

        return obs;
    }

    private Map<String, String> getCsvDataMap(DataLake dataLake) {
        String[] csvHeaderAndRow = dataLake.getData().split("\n");
        return parseRow(csvHeaderAndRow[0], csvHeaderAndRow[1]);
    }

    private OffsetDateTime getExtractStartedAtValue(Map<String, String> row, DataLake dataLake) {
        int startYear = Integer.parseInt(row.get("Start Year"));
        int startMonth = Integer.parseInt(getOrDefault(row.get("Start Month"), "1"));
        int startDay = Integer.parseInt(getOrDefault(row.get("Start Day"), "1"));
        return convertOffsetDateTime(dataLake, startYear, startMonth, startDay);
    }

    private OffsetDateTime getExtractEndedAtValue(Map<String, String> row, DataLake dataLake) {
        int startYear = Integer.parseInt(row.get("End Year"));
        int startMonth = Integer.parseInt(getOrDefault(row.get("End Month"), "1"));
        int startDay = Integer.parseInt(getOrDefault(row.get("End Day"), "1"));
        return convertOffsetDateTime(dataLake, startYear, startMonth, startDay);
    }

    private OffsetDateTime convertOffsetDateTime(DataLake dataLake, int year, int month, int day) {
        LocalDateTime ldt;
        try {
            ldt = LocalDateTime.of(year, month, day, 0, 0, 0);
        } catch (DateTimeException e) {
            LOG.warn("'{}' for observation {}", e.getMessage(), dataLake.getObservationId());
            ldt = LocalDateTime.of(year, month, 1, 0, 0, 0);
        }
        return OffsetDateTime.of(ldt, ZoneOffset.UTC);
    }

    private String getOrDefault(String value, String def) {
        if (value == null || value.isBlank()) {
            return def;
        }
        return value;
    }

    private EventType defineEventType(String disasterType, String disasterSubtype, String disasterSubSubtype) {
        EventType type = typeMap.get(disasterType);
        if (type == null) {
            type = typeMap.get(disasterSubtype);
        }
        if (type == null) {
            type = typeMap.get(disasterSubSubtype);
        }
        return Optional.ofNullable(type).orElse(EventType.OTHER);
    }

    private String makeName(Map<String, String> csvData) {
        return csvData.get("Country") +
                " " +
                csvData.get("Year") +
                "-" +
                csvData.get("Disaster Type") +
                " " +
                csvData.get("Disaster Subtype") +
                "-" +
                csvData.get("Event Name");
    }
}
