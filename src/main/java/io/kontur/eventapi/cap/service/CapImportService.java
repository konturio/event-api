package io.kontur.eventapi.cap.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import feign.FeignException;
import io.kontur.eventapi.cap.client.CapImportClient;
import io.kontur.eventapi.cap.converter.CapDataLakeConverter;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.cap.dto.CapParsedEvent;
import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.entity.DataLake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public abstract class CapImportService {

    private final static Logger LOG = LoggerFactory.getLogger(CapImportService.class);

    private final DataLakeDao dataLakeDao;
    private final CapImportClient client;

    private final CapDataLakeConverter dataLakeConverter;

    public CapImportService(DataLakeDao dataLakeDao, CapImportClient client, CapDataLakeConverter dataLakeConverter) {
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

    public List<DataLake> createDataLakes(Map<String, CapParsedEvent> events, String provider) {
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

    protected List<DataLake> createDataLakeList(Map<String, CapParsedEvent> events,
                                                Map<String, List<OffsetDateTime>> existsDataLakes,
                                                String provider) {
        List<DataLake> dataLakes = new ArrayList<>();
        for (String key : events.keySet()) {
            try {
                CapParsedItem item = (CapParsedItem) events.get(key);
                if (!existsDataLakes.containsKey(key)
                        || existsDataLakes.get(key).stream().noneMatch(time -> time.isEqual(item.getPubDate()))) {
                    dataLakes.add(getDataLakeConverter().convertEvent(item, provider));
                }
            } catch (Exception e) {
                LOG.warn("Error while processing {} wildfire event. {}", provider, e.getMessage());
            }
        }
        return dataLakes;
    }

    public void saveDataLakes(List<DataLake> dataLakes) {
        if (!CollectionUtils.isEmpty(dataLakes)) {
            dataLakeDao.storeDataLakes(dataLakes);
        }
    }

    protected CapImportClient getClient() {
        return client;
    }

    protected DataLakeDao getDataLakeDao() {
        return dataLakeDao;
    }

    protected CapDataLakeConverter getDataLakeConverter() {
        return dataLakeConverter;
    }
}
