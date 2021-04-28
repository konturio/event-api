package io.kontur.eventapi.staticdata.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class StaticNormalizer extends Normalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return getProviders().contains(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        setCommonFields(dataLakeDto, normalizedObservation);
        setExtraFields(dataLakeDto, normalizedObservation);
        return normalizedObservation;
    }

    private void setCommonFields(DataLake dataLake, NormalizedObservation normalizedObservation) {
        normalizedObservation.setObservationId(dataLake.getObservationId());
        normalizedObservation.setProvider(dataLake.getProvider());
        normalizedObservation.setLoadedAt(dataLake.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLake.getUpdatedAt());
        normalizedObservation.setExternalEventId(dataLake.getExternalId());
        normalizedObservation.setActive(false);
    }

    protected String createName(String eventType, String ... atu) {
        return eventType + " - " + Arrays.stream(atu)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    protected OffsetDateTime parseLocalDate(String str) {
        return StringUtils.isBlank(str)
                ? null
                : OffsetDateTime.of(LocalDate.parse(str), LocalTime.MIN, ZoneOffset.UTC);
    }

    protected OffsetDateTime parseISOBasicDate(String str) {
        return StringUtils.isBlank(str)
                ? null
                : OffsetDateTime.of(LocalDate.parse(str, DateTimeFormatter.BASIC_ISO_DATE), LocalTime.MIN,  ZoneOffset.UTC);
    }

    protected abstract List<String> getProviders();
    protected abstract void setExtraFields(DataLake dataLake, NormalizedObservation normalizedObservation);
}
