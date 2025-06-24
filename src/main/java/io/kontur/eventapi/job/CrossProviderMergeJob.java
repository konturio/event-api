package io.kontur.eventapi.job;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CrossProviderMergeJob extends AbstractJob {
    private static final Logger LOG = LoggerFactory.getLogger(CrossProviderMergeJob.class);

    public CrossProviderMergeJob(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }

    @Override
    public void execute() {
        LOG.debug("Cross provider merge job executed");
        // TODO implement candidate search logic
    }

    @Override
    public String getName() {
        return "crossProviderMerge";
    }
}
