package io.kontur.eventapi.nhc.job;

import io.kontur.eventapi.nhc.NhcUtil;
import io.kontur.eventapi.nhc.converter.NhcXmlParser;
import io.kontur.eventapi.nhc.service.NhcCpService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NhcCpImportJob extends NhcImportJob {

    @Autowired
    protected NhcCpImportJob(NhcCpService nhcService, NhcXmlParser nhcXmlParser, MeterRegistry meterRegistry) {
        super(nhcService, nhcXmlParser, null, meterRegistry, null, null);
    }

    @Override
    public String getName() {
        return "nhcCpImport";
    }

    @Override
    protected String getProvider() {
        return NhcUtil.NHC_CP_PROVIDER;
    }
}
