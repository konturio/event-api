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
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
public class GdacsService {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsService.class);

    private final DataLakeDao dataLakeDao;
    private final GdacsDataLakeConverter dataLakeConverter;
    private final GdacsClient gdacsClient;

    @Autowired
    public GdacsService(DataLakeDao dataLakeDao, GdacsDataLakeConverter dataLakeConverter, GdacsClient gdacsClient) {
        this.dataLakeDao = dataLakeDao;
        this.dataLakeConverter = dataLakeConverter;
        this.gdacsClient = gdacsClient;
    }

    public Optional<String> getGdacsXml() {
        try {
            return Optional.of(gdacsClient.getXml());
        } catch (FeignException e) {
            LOG.warn("Gdacs cap xml has not found");
        }
        return Optional.empty();
    }

    public List<String> getAlerts(List<String> links) {
        return links.stream()
                .map(this::getAlertAfterHandleException)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(alert -> alert.startsWith("\uFEFF") ? alert.substring(1) : alert)
                .collect(toList());
    }

    private Optional<String> getAlertAfterHandleException(String link) {
        try {
            return Optional.of(gdacsClient.getAlertByLink(link));
        } catch (FeignException e) {
            LOG.warn("Alert by link https://www.gdacs.org{} not found", link);
        }
        return Optional.empty();
    }

    public List<DataLake> getDataLakes(List<ParsedAlert> alerts) {
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

    public void saveGdacs(List<DataLake> dataLakes){
        dataLakes.forEach(dataLakeDao::storeEventData);
    }


}
