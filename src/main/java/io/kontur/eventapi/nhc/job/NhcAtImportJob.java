package io.kontur.eventapi.nhc.job;

import io.kontur.eventapi.nhc.NhcUtil;
import io.kontur.eventapi.nhc.converter.NhcXmlParser;
import io.kontur.eventapi.nhc.service.NhcAtService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NhcAtImportJob extends NhcImportJob {

    @Autowired
    protected NhcAtImportJob(NhcAtService nhcAtService, NhcXmlParser nhcXmlParser, MeterRegistry meterRegistry) {
        super(nhcAtService, nhcXmlParser, null, meterRegistry, null, null);
    }

    @Override
    public String getName() {
        return "nhcAtImport";
    }

    @Override
    protected String getProvider() {
        return NhcUtil.NHC_AT_PROVIDER;
    }
}
