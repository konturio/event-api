package io.kontur.eventapi.tornadojapanma.job;

import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.tornadojapanma.service.TornadoJapanMaImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class TornadoJapanMaCsvImportJob extends AbstractJob {

    private final TornadoJapanMaImportService service;
    private final String csvUrl;

    public TornadoJapanMaCsvImportJob(MeterRegistry meterRegistry,
                                      TornadoJapanMaImportService service,
                                      @Value("${tornadoJapanMa.csv}") String csvUrl) {
        super(meterRegistry);
        this.service = service;
        this.csvUrl = csvUrl;
    }

    @Override
    public String getName() {
        return "tornadoJapanMaCsvImport";
    }

    @Override
    public void execute() throws Exception {
        String csvData = service.downloadCsv(csvUrl);
        OffsetDateTime updatedAt = OffsetDateTime.now();
        service.storeCsvData(csvData, updatedAt);
    }
}
