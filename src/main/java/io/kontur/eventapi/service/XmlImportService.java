package io.kontur.eventapi.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import feign.FeignException;
import io.kontur.eventapi.client.XmlImportClient;
import io.kontur.eventapi.converter.DataLakeConverter;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dto.ParsedEvent;
import io.kontur.eventapi.dto.ParsedItem;
import io.kontur.eventapi.entity.DataLake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public abstract class XmlImportService {

    private final static Logger LOG = LoggerFactory.getLogger(XmlImportService.class);

    private final DataLakeDao dataLakeDao;
    private final XmlImportClient client;

    private final DataLakeConverter dataLakeConverter;

    public XmlImportService(DataLakeDao dataLakeDao, XmlImportClient client, DataLakeConverter dataLakeConverter) {
        this.dataLakeDao = dataLakeDao;
        this.client = client;
        this.dataLakeConverter = dataLakeConverter;
    }

    public Optional<String> fetchXml(String providerName) {
        try {
            return Optional.of(client.getXml());
        } catch (FeignException e) {
            LOG.warn("{} cap xml has not found", providerName);
        }
        return Optional.empty();
    }

    public List<DataLake> createDataLakes(Map<String, ParsedEvent> events, String provider) {
        List<DataLake> dataLakes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(events)) {
            Map<String, List<OffsetDateTime>> existsDataLakes = new HashMap<>();
            getDataLakeDao().getDataLakesByExternalIdsAndProvider(events.keySet(), provider)
                    .forEach(dataLake -> {
                        if (!existsDataLakes.containsKey(dataLake.getExternalId())) {
                            existsDataLakes.put(dataLake.getExternalId(), new ArrayList<>());
                        }
                        existsDataLakes.get(dataLake.getExternalId()).add(dataLake.getUpdatedAt());
                    });
            dataLakes.addAll(createDataLakeList(events, existsDataLakes, provider));
            dataLakes.sort(Comparator.comparing(DataLake::getLoadedAt));
        }
        return dataLakes;
    }

    protected List<DataLake> createDataLakeList(Map<String, ParsedEvent> events,
                                                Map<String, List<OffsetDateTime>> existsDataLakes,
                                                String provider) {
        List<DataLake> dataLakes = new ArrayList<>();
        for (String key : events.keySet()) {
            try {
                ParsedItem item = (ParsedItem) events.get(key);
                if (!existsDataLakes.containsKey(key)
                        || existsDataLakes.get(key).stream().noneMatch(time -> time.isEqual(item.getPubDate()))) {
                    dataLakes.add(getDataLakeConverter().convertEvent(item, provider));
                }
            } catch (Exception e) {
                LOG.error("Error while processing {} wildfire event. {}", provider, e.getMessage());
            }
        }
        return dataLakes;
    }

    public void saveDataLakes(List<DataLake> dataLakes) {
        if (!CollectionUtils.isEmpty(dataLakes)) {
            dataLakeDao.storeDataLakes(dataLakes);
        }
    }

    protected XmlImportClient getClient() {
        return client;
    }

    protected DataLakeDao getDataLakeDao() {
        return dataLakeDao;
    }

    protected DataLakeConverter getDataLakeConverter() {
        return dataLakeConverter;
    }
}
