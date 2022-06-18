package io.kontur.eventapi.nhc.job;

import java.util.HashMap;
import java.util.Map;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dto.ParsedEvent;
import io.kontur.eventapi.dto.ParsedItem;
import io.kontur.eventapi.job.XmlImportJob;
import io.kontur.eventapi.nhc.converter.NhcXmlParser;
import io.kontur.eventapi.service.XmlImportService;
import io.micrometer.core.instrument.MeterRegistry;
import liquibase.repackaged.org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class NhcImportJob extends XmlImportJob {

    public NhcImportJob(XmlImportService nhcAtService, NhcXmlParser nhcXmlParser, DataLakeDao dataLakeDao,
                        MeterRegistry meterRegistry, String meterName, String meterDesc) {
        super(nhcAtService, nhcXmlParser, dataLakeDao, meterRegistry, meterName, meterDesc);
    }

    @Override
    protected Map<String, ParsedEvent> filterParsedItems(Map<String, ParsedEvent> parsedEvents) {
        if (MapUtils.isNotEmpty(parsedEvents)) {
            Map<String, ParsedEvent> filteredEvents = new HashMap<>();
            for (String key : parsedEvents.keySet()) {
                ParsedItem item = (ParsedItem) parsedEvents.get(key);
                if (StringUtils.isNotBlank(item.getData()) && item.getData().contains("Forecast Advisory")) {
                    filteredEvents.put(key, item);
                }
            }
            return filteredEvents;
        } else {
            return super.filterParsedItems(parsedEvents);
        }
    }
}
