package io.kontur.eventapi.jtwc.job;

import io.kontur.eventapi.jtwc.parser.JtwcRssParser;
import io.kontur.eventapi.jtwc.service.JtwcImportService;
import io.kontur.eventapi.job.AbstractJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class JtwcImportJob extends AbstractJob {

    private final JtwcRssParser parser;
    private final JtwcImportService service;

    public JtwcImportJob(MeterRegistry meterRegistry, JtwcRssParser parser, JtwcImportService service) {
        super(meterRegistry);
        this.parser = parser;
        this.service = service;
    }

    @Override
    public String getName() {
        return "jtwcImport";
    }

    @Override
    public void execute() throws Exception {
        for (JtwcRssParser.RssItem item : parser.parse()) {
            String text = parser.loadText(item.link());
            service.storeText(text, item.pubDate());
        }
    }
}
