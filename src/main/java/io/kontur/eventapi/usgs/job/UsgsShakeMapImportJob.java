package io.kontur.eventapi.usgs.job;

import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.usgs.service.UsgsShakeMapService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class UsgsShakeMapImportJob extends AbstractJob {

    private final UsgsShakeMapService service;

    public UsgsShakeMapImportJob(MeterRegistry meterRegistry, UsgsShakeMapService service) {
        super(meterRegistry);
        this.service = service;
    }

    @Override
    public void execute() {
        service.importLatestShakeMaps();
    }

    @Override
    public String getName() {
        return "usgsShakeMapImport";
    }
}
