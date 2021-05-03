package io.kontur.eventapi.tornadojapanma.job;

import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.tornadojapanma.dto.ParsedCase;
import io.kontur.eventapi.tornadojapanma.parser.HistoricalTornadoJapanMaHtmlParser;
import io.kontur.eventapi.tornadojapanma.service.TornadoJapanMaImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

@Component
public class HistoricalTornadoJapanMaImportJob extends AbstractJob {

    private final HistoricalTornadoJapanMaHtmlParser parser;
    private final TornadoJapanMaImportService service;

    protected HistoricalTornadoJapanMaImportJob(MeterRegistry meterRegistry, TornadoJapanMaImportService service,
                                                HistoricalTornadoJapanMaHtmlParser parser) {
        super(meterRegistry);
        this.service = service;
        this.parser = parser;
    }

    @Override
    public String getName() {
        return "historicalTornadoJapanMaImport";
    }

    @Override
    public void execute() throws Exception {
        OffsetDateTime updatedAt = service.convertDate(parser.parseUpdatedAt());
        Set<ParsedCase> parsedCases = parser.parseCases();
        service.storeDataLakes(parsedCases, updatedAt);
    }
}
