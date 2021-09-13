package io.kontur.eventapi.enrichment.postprocessor;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.job.Applicable;

public interface EnrichmentPostProcessor extends Applicable<FeedData> {
    void process(FeedData event);
}
