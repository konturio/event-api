package io.kontur.eventapi.emdat.normalization;

import io.kontur.eventapi.emdat.jobs.EmDatImportJob;
import io.kontur.eventapi.emdat.normalization.converter.EmDatGeometryConverter;
import io.kontur.eventapi.emdat.normalization.converter.EmDatSeverityConverter;
import io.kontur.eventapi.emdat.service.EmDatNormalizationService;
import io.kontur.eventapi.emdat.classification.EmDatTypeClassifier;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.job.Applicable;
import io.kontur.eventapi.normalization.Normalizer;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Geometry;
import org.wololo.geojson.Point;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.kontur.eventapi.util.CsvUtil.parseRow;
import static java.lang.Double.parseDouble;

@Component
public class EmDatNormalizer extends Normalizer {

    private static final Logger LOG = LoggerFactory.getLogger(EmDatNormalizer.class);

    private final EmDatTypeClassifier typeClassifier;

    private final List<EmDatSeverityConverter> severityConverters;
    private final EmDatGeometryConverter geometryConverter;
    private final EmDatNormalizationService normalizationService;
    private final WKTReader wktReader = new WKTReader();

    public EmDatNormalizer(
            List<EmDatSeverityConverter> severityConverters,
            EmDatGeometryConverter geometryConverter,
            EmDatNormalizationService normalizationService,
            EmDatTypeClassifier typeClassifier) {
        this.severityConverters = severityConverters;
        this.geometryConverter = geometryConverter;
        this.normalizationService = normalizationService;
        this.typeClassifier = typeClassifier;
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
        obs.setProperName(csvData.get("Event Name"));
        obs.setRegion(csvData.get("ISO") + " " + csvData.get("Location"));
        if (!StringUtils.isEmpty(csvData.get("Total Damages ('000 US$)"))) {
            try {
                obs.setCost(new BigDecimal(csvData.get("Total Damages ('000 US$)")).multiply(BigDecimal.valueOf(1000)));
            } catch (NumberFormatException e) {
                LOG.debug(String.format("'%s' for observation %s", e.getMessage(), obs.getObservationId()));
            }
        }

        Point point = null;
        if (!StringUtils.isEmpty(csvData.get("Latitude")) && !StringUtils.isEmpty(csvData.get("Longitude"))) {
            try {
                Double lon = parseDouble(csvData.get("Longitude"));
                Double lat = parseDouble(csvData.get("Latitude"));
                String wktPoint = makeWktPoint(lon, lat);
                wktReader.read(wktPoint); //validate coordinates
                obs.setPoint(wktPoint);
                point = new Point(new double[]{lon, lat});
            } catch (NumberFormatException | ParseException e) {
                LOG.debug(String.format("'%s' for observation %s", e.getMessage(), obs.getObservationId()));
            }
        }

        Geometry geom = normalizationService
                .obtainGeometries(csvData.get("Country"), csvData.get("Location"))
                .or(() -> normalizationService.convertWktPointIntoGeometry(obs.getPoint()))
                .orElse(null);
        obs.setGeometries(geometryConverter.convertGeometry(geom, point, csvData.get("Dis Mag Scale"), csvData.get("Dis Mag Value")));

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
        EventType type = typeClassifier.classify(disasterSubSubtype);
        if (type == EventType.OTHER) {
            type = typeClassifier.classify(disasterSubtype);
        }
        if (type == EventType.OTHER) {
            type = typeClassifier.classify(disasterType);
        }
        return type;
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
