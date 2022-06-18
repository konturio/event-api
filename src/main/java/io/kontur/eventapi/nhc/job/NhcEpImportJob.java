package io.kontur.eventapi.nhc.job;

import io.kontur.eventapi.nhc.NhcUtil;
import io.kontur.eventapi.nhc.converter.NhcXmlParser;
import io.kontur.eventapi.nhc.service.NhcEpService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NhcEpImportJob extends NhcImportJob {

    @Autowired
    protected NhcEpImportJob(NhcEpService nhcService, NhcXmlParser nhcXmlParser, MeterRegistry meterRegistry) {
        super(nhcService, nhcXmlParser, null, meterRegistry,
                "nhcEpFeedXML", "NHC Eastern Pacific Cyclones feed did not update (hours)");
    }

    @Override
    public String getName() {
        return "nhcEpImport";
    }

    @Override
    protected String getProvider() {
        return NhcUtil.NHC_EP_PROVIDER;
    }
}
