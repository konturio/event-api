package io.kontur.eventapi.tornado.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import org.apache.commons.lang3.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class TornadoNormalizer extends Normalizer {

    private final static Map<String, Severity> SEVERITIES = Map.of(
            "0", Severity.MINOR,
            "1", Severity.MODERATE,
            "2", Severity.MODERATE,
            "3", Severity.SEVERE,
            "4", Severity.EXTREME,
            "5", Severity.EXTREME
    );

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return getProviders().contains(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        setCommonFields(dataLakeDto, normalizedObservation);
        setDataFields(dataLakeDto.getData(), normalizedObservation);
        setGeometry(dataLakeDto.getData(), normalizedObservation);
        return normalizedObservation;
    }

    private void setCommonFields(DataLake dataLakeDto, NormalizedObservation normalizedObservation) {
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());
        normalizedObservation.setType(EventType.TORNADO);
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setActive(false);
    }

    protected Severity convertSeverity(String fujitaScale) {
        return SEVERITIES.getOrDefault(StringUtils.defaultString(fujitaScale), Severity.UNKNOWN);
    }

    protected String createName(String ... atu) {
        return "Tornado - " + Arrays.stream(atu)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    protected Double parseDouble(String string) {
        return StringUtils.isBlank(string) ? null : Double.valueOf(string);
    }

    protected static String makeWktLineString(Double startLon, Double startLat, Double endLon, Double endLat) {
        return String.format("LINESTRING(%s %s, %s %s)", startLon, startLat, endLon, endLat);
    }

    protected abstract void setDataFields(String data, NormalizedObservation normalizedObservation);

    protected abstract void setGeometry(String data, NormalizedObservation normalizedObservation);

    protected abstract List<String> getProviders();

}
