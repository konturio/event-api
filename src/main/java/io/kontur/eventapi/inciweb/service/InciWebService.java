package io.kontur.eventapi.inciweb.service;

import static io.kontur.eventapi.inciweb.converter.InciWebDataLakeConverter.INCIWEB_PROVIDER;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.inciweb.client.InciWebClient;
import io.kontur.eventapi.inciweb.converter.InciWebDataLakeConverter;
import io.kontur.eventapi.inciweb.dto.ParsedItem;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class InciWebService {

    private final static Logger LOG = LoggerFactory.getLogger(InciWebService.class);

    private final DataLakeDao dataLakeDao;
    private final InciWebDataLakeConverter dataLakeConverter;
    private final InciWebClient inciWebClient;

    @Autowired
    public InciWebService(DataLakeDao dataLakeDao, InciWebDataLakeConverter dataLakeConverter,
                          InciWebClient inciWebClient) {
        this.dataLakeDao = dataLakeDao;
        this.dataLakeConverter = dataLakeConverter;
        this.inciWebClient = inciWebClient;
    }

    public Optional<String> fetchXml() {
        try {
            return Optional.of(inciWebClient.getXml());
        } catch (FeignException e) {
            LOG.warn("InciWeb xml has not found");
        }
        return Optional.empty();
    }

    public List<DataLake> createDataLake(Map<String, ParsedItem> events, Counter counter) {
        List<DataLake> dataLakes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(events)) {
            Map<String, List<OffsetDateTime>> existsDataLakes = new HashMap<>();
            dataLakeDao.getDataLakesByExternalIdsAndProvider(events.keySet(), INCIWEB_PROVIDER)
                    .forEach(dataLake -> {
                        if (!existsDataLakes.containsKey(dataLake.getExternalId())) {
                            existsDataLakes.put(dataLake.getExternalId(), new ArrayList<>());
                        }
                        existsDataLakes.get(dataLake.getExternalId()).add(dataLake.getUpdatedAt());
                    });
            for (String key : events.keySet()) {
                try {
                    if (!existsDataLakes.containsKey(key)
                            || existsDataLakes.get(key).stream().noneMatch(time -> time.isEqual(events.get(key).getPubDate()))) {
                        dataLakes.add(dataLakeConverter.convertEvent(events.get(key)));
                        if (counter != null) {
                            counter.increment();
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error while processing InciWeb wildfire event. {}", e.getMessage());
                }
            }
            dataLakes.sort(Comparator.comparing(DataLake::getLoadedAt));
        }
        return dataLakes;
    }

    public void saveEventsToDataLake(List<DataLake> dataLakes) {
        if (!CollectionUtils.isEmpty(dataLakes)) {
            dataLakeDao.storeDataLakes(dataLakes);
        }
    }
}
