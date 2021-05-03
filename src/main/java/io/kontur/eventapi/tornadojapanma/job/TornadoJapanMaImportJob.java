package io.kontur.eventapi.tornadojapanma.job;

import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.tornadojapanma.dto.ParsedCase;
import io.kontur.eventapi.tornadojapanma.parser.TornadoJapanMaHtmlParser;
import io.kontur.eventapi.tornadojapanma.service.TornadoJapanMaImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Set;

@Component
public class TornadoJapanMaImportJob extends AbstractJob {

    private final TornadoJapanMaHtmlParser parser;
    private final TornadoJapanMaImportService service;

    protected TornadoJapanMaImportJob(MeterRegistry meterRegistry, TornadoJapanMaImportService service,
                                      TornadoJapanMaHtmlParser parser) {
        super(meterRegistry);
        this.service = service;
        this.parser = parser;
    }

    @Override
    public String getName() {
        return "tornadoJapanMaImport";
    }

    @Override
    public void execute() throws Exception {
        OffsetDateTime updatedAt = service.convertDate(parser.parseUpdatedAt());
        Set<ParsedCase> parsedCases = parser.parseCases();
        service.storeDataLakes(parsedCases, updatedAt);
    }
}
