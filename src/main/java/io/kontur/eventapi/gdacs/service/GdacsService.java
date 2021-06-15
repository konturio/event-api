package io.kontur.eventapi.gdacs.service;

import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
public class GdacsService {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsService.class);
    public static final String ALERT_BY_LINK = "Alert by link https://www.gdacs.org{}";

    private final DataLakeDao dataLakeDao;
    private final GdacsDataLakeConverter dataLakeConverter;
    private final GdacsClient gdacsClient;

    @Autowired
    public GdacsService(DataLakeDao dataLakeDao, GdacsDataLakeConverter dataLakeConverter, GdacsClient gdacsClient) {
        this.dataLakeDao = dataLakeDao;
        this.dataLakeConverter = dataLakeConverter;
        this.gdacsClient = gdacsClient;
    }

    public Optional<String> fetchGdacsXml() {
        try {
            return Optional.of(gdacsClient.getXml());
        } catch (FeignException e) {
            LOG.warn("Gdacs cap xml has not found");
        }
        return Optional.empty();
    }

    public Map<String, String> fetchAlerts(List<String> links) {
        Map<String, String> processedLinks = new HashMap<>();

        for (String link : links) {
            getAlertAfterHandleException(link)
                    .ifPresent(alert -> processedLinks.put(link, alert.startsWith("\uFEFF") ? alert.substring(1) : alert));
        }

        return processedLinks;
    }

    private Optional<String> getAlertAfterHandleException(String link) {
        try {
            String alertByLink = gdacsClient.getAlertByLink(link);
            if (isEmpty(alertByLink)) {
                LOG.warn(ALERT_BY_LINK + " is empty", link);
                return Optional.empty();
            }
            return Optional.of(alertByLink);
        } catch (FeignException e) {
            LOG.warn(ALERT_BY_LINK + " not found", link);
        }
        return Optional.empty();
    }

    public List<DataLake> createDataLakeListWithAlertsAndGeometry(List<ParsedAlert> alerts) {
        var dataLakes = new ArrayList<DataLake>();
        for (ParsedAlert alert : alerts) {
            var dataLakesByExternalId = dataLakeDao.getDataLakesByExternalId(alert.getIdentifier());
            if (dataLakesByExternalId.isEmpty()) {
                var geometry = getGeometryToAlert(
                        alert.getEventType(),
                        alert.getEventId(),
                        alert.getCurrentEpisodeId(),
                        alert.getIdentifier());

                if (geometry.isPresent()) {
                    dataLakes.add(dataLakeConverter.convertGdacs(alert));
                    dataLakes.add(dataLakeConverter.convertGdacsWithGeometry(alert, geometry.get()));
                }
            }
        }
        dataLakes.sort(Comparator.comparing(DataLake::getLoadedAt));
        return dataLakes;
    }

    private Optional<String> getGeometryToAlert(String eventType, String eventId, String currentEpisodeId, String externalId) {
        try {
            return Optional.of(
                    gdacsClient.getGeometryByLink(eventType, eventId, currentEpisodeId)
            );
        } catch (FeignException e) {
            LOG.warn("Geometry for gdacs alert has not found. identifier = {}", externalId);
        }
        return Optional.empty();
    }

    public void saveGdacs(List<DataLake> dataLakes) {
        dataLakeDao.storeDataLakes(dataLakes);
    }


}
