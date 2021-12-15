package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.GeneralMapper;
import io.kontur.eventapi.entity.ProcessingDuration;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

import static io.kontur.eventapi.metrics.config.MetricsConfig.*;

@Component
public class GeneralDao {
    private final GeneralMapper mapper;

    public GeneralDao(GeneralMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<ProcessingDuration> getProcessingDuration(String stage, OffsetDateTime latestProcessedAt) {
        return switch (stage) {
            case NORMALIZATION -> mapper.getObservationNormalizationDuration(latestProcessedAt);
            case RECOMBINATION -> mapper.getObservationsRecombinationDuration(latestProcessedAt);
            case COMPOSITION -> mapper.getEventCompositionDuration(latestProcessedAt);
            case ENRICHMENT -> mapper.getEventEnrichmentDuration(latestProcessedAt);
            default -> null;
        };
    }
}
