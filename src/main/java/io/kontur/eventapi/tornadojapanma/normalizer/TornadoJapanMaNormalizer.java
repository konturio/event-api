package io.kontur.eventapi.tornadojapanma.normalizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.tornadojapanma.dto.Details;
import io.kontur.eventapi.tornadojapanma.dto.ParsedCase;
import io.kontur.eventapi.util.SeverityUtil;
import org.apache.commons.lang3.StringUtils;
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
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.kontur.eventapi.tornadojapanma.service.TornadoJapanMaImportService.TORNADO_JAPAN_MA_PROVIDER;

@Component
public class TornadoJapanMaNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(TornadoJapanMaNormalizer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final GeoJSONWriter geoJsonWriter = new GeoJSONWriter();
    private static final WKTReader wktReader = new WKTReader();

    private final static Pattern coordinatePattern = Pattern.compile("\\d+度\\d+分\\d+秒");
    private final static Pattern numberPattern = Pattern.compile("\\d+");
    private final static Pattern detailsDatePattern = Pattern.compile("\\d+年\\d+月\\d+日(\\d+時(\\d+分)?)?");
    private final static Pattern generalDatePattern = Pattern.compile("\\d+/\\d+/\\d+( \\d+(:\\d+)?)?");

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return TORNADO_JAPAN_MA_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());
        normalizedObservation.setActive(false);
        try {
            ParsedCase parsedCase = objectMapper.readValue(dataLakeDto.getData(), ParsedCase.class);
            if (parsedCase.getDetails() != null) {
                setGeometries(normalizedObservation, parsedCase.getDetails());
                normalizedObservation.setEventSeverity(parseSeverity(parsedCase.getDetails().getFScale()));
                normalizedObservation.setName(createName(parsedCase.getDetails().getOccurrencePlace().getPrefectures(),
                        parsedCase.getDetails().getOccurrencePlace().getMunicipalities(),
                        parsedCase.getDetails().getOccurrencePlace().getAddress()));
                OffsetDateTime startedAt = parseDate(parsedCase.getDetails().getOccurrenceDateTime(), detailsDatePattern);
                OffsetDateTime endedAt = parseDate(parsedCase.getDetails().getDisappearanceDateTime(), detailsDatePattern);
                normalizedObservation.setStartedAt(startedAt != null ? startedAt : endedAt);
                normalizedObservation.setEndedAt(endedAt != null ? endedAt : startedAt);
            } else {
                String fujitaScale = parsedCase.getJefScale() != null ? parsedCase.getJefScale().getFScale() : parsedCase.getFScale();
                normalizedObservation.setEventSeverity(parseSeverity(fujitaScale));
                normalizedObservation.setName(createName(parsedCase.getOccurrencePlace()));
                OffsetDateTime date = parseDate(parsedCase.getOccurrenceDateTime(), generalDatePattern);
                normalizedObservation.setStartedAt(date);
                normalizedObservation.setEndedAt(date);
            }
            normalizedObservation.setType(parseType(parsedCase.getType()));
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
        return normalizedObservation;
    }

    private void setGeometries(NormalizedObservation normalizedObservation, Details details) {
        Double x1 = parseCoordinate(details.getOccurrencePlace().getLongitude());
        Double y1 = parseCoordinate(details.getOccurrencePlace().getLatitude());
        Double x2 = parseCoordinate(details.getDisappearancePlace().getLongitude());
        Double y2 = parseCoordinate(details.getDisappearancePlace().getLatitude());
        boolean startPointPresent = x1 != null && y1 != null;
        boolean endPointPresent = x2 != null && y2 != null;
        String point = startPointPresent ? makeWktPoint(x1, y1) : endPointPresent ? makeWktPoint(x2, y2) : null;
        String geom = startPointPresent && endPointPresent ? makeWktLine(x1, y1, x2, y2) : point;
        normalizedObservation.setPoint(point);
        try {
            Geometry geometry = geom == null ? null : geoJsonWriter.write(wktReader.read(geom));
            Feature feature = new Feature(geometry, Collections.emptyMap());
            normalizedObservation.setGeometries(new FeatureCollection(new Feature[]{feature}).toString());
        } catch (ParseException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private Double parseCoordinate(String str) {
        String coordinateString = findMatch(str, coordinatePattern);
        if (coordinateString != null) {
            List<Integer> nums = findNumbers(coordinateString);
            if (nums.size() == 3) {
                double coordinate = nums.get(0) + nums.get(1) / 60. + nums.get(2) / 3600.;
                return new BigDecimal(coordinate).setScale(6, RoundingMode.HALF_UP).doubleValue();
            }
        }
        return null;
    }

    private Severity parseSeverity(String str) {
        List<Integer> nums = findNumbers(str);
        return nums.size() > 0 ? SeverityUtil.convertFujitaScale(nums.get(nums.size() - 1).toString()) : Severity.UNKNOWN;
    }

    private String createName(String... atu) {
        String atuString = Arrays.stream(atu)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
        return "Tornado - Japan" + (atuString.isBlank() ? "" : ", ") + atuString;
    }

    private EventType parseType(String type) {
        return type.contains("竜巻") ? EventType.TORNADO : EventType.OTHER;
    }

    private OffsetDateTime parseDate(String str, Pattern pattern) {
        String dateString = findMatch(str, pattern);
        if (dateString != null) {
            List<Integer> nums = findNumbers(dateString);
            if (nums.size() >= 3) {
                return OffsetDateTime.of(nums.get(0), nums.get(1), nums.get(2), nums.size() > 3 ? nums.get(3) : 0,
                        nums.size() > 4 ? nums.get(4) : 0, 0, 0, ZoneOffset.UTC);
            }
        }
        return null;
    }

    private List<Integer> findNumbers(String searchString) {
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = numberPattern.matcher(searchString);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers;
    }

    private String findMatch(String searchString, Pattern pattern) {
        Matcher matcher = pattern.matcher(searchString);
        return matcher.find() ? matcher.group() : null;
    }
}
