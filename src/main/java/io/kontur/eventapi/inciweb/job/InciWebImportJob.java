package io.kontur.eventapi.inciweb.job;

import io.kontur.eventapi.inciweb.converter.InciWebXmlParser;
import io.kontur.eventapi.inciweb.service.InciWebService;
import io.kontur.eventapi.job.XmlImportJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InciWebImportJob extends XmlImportJob {

    public final static String INCIWEB_PROVIDER = "wildfire.inciweb";

    @Autowired
    protected InciWebImportJob(InciWebService inciWebService, InciWebXmlParser inciWebXmlParser, MeterRegistry meterRegistry) {
        super(inciWebService, inciWebXmlParser, null, meterRegistry,
                "inciwebFeedXML", "InciWeb feed did not update (hours)");
    }

    @Override
    public String getName() {
        return "inciWebImport";
    }

    @Override
    protected String getProvider() {
        return INCIWEB_PROVIDER;
    }
}
