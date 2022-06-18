package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.job.XmlImportJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GdacsSearchJob extends XmlImportJob {

    @Autowired
    public GdacsSearchJob(GdacsService gdacsService, GdacsAlertXmlParser gdacsAlertParser, DataLakeDao dataLakeDao,
                          MeterRegistry meterRegistry) {
        super(gdacsService, gdacsAlertParser, dataLakeDao, meterRegistry,
                "gdacsFeedXML", "Gdacs CAP feed did not update (hours)");
    }

    public Map<String, String> filterExistsAlerts(Map<String, String> alerts) {
        Map<String, String> filteredAlerts = new HashMap<>();
        Map<String, DataLake> existsDataLakes = new HashMap<>();
        getDataLakeDao().getDataLakesByExternalIds(alerts.keySet())
                .forEach(dataLake -> existsDataLakes.put(dataLake.getExternalId(), dataLake));
        alerts.keySet().stream()
                .filter(key -> !existsDataLakes.containsKey(key))
                .forEach(key -> filteredAlerts.put(key, alerts.get(key)));
        return filteredAlerts;
    }

    @Override
    public String getName() {
        return "gdacsSearch";
    }

    @Override
    protected String getProvider() {
        return "";
    }
}




