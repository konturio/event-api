package io.kontur.eventapi.nhc.job;

import java.util.HashMap;
import java.util.Map;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.cap.dto.CapParsedEvent;
import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.cap.job.CapImportJob;
import io.kontur.eventapi.nhc.converter.NhcXmlParser;
import io.kontur.eventapi.cap.service.CapImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class NhcImportJob extends CapImportJob {

    public NhcImportJob(CapImportService nhcAtService, NhcXmlParser nhcXmlParser, DataLakeDao dataLakeDao,
                        MeterRegistry meterRegistry, String meterName, String meterDesc) {
        super(nhcAtService, nhcXmlParser, dataLakeDao, meterRegistry, meterName, meterDesc);
    }

    @Override
    protected Map<String, CapParsedEvent> filterParsedItems(Map<String, CapParsedEvent> parsedEvents) {
        if (MapUtils.isNotEmpty(parsedEvents)) {
            Map<String, CapParsedEvent> filteredEvents = new HashMap<>();
            for (String key : parsedEvents.keySet()) {
                CapParsedItem item = (CapParsedItem) parsedEvents.get(key);
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
