package io.kontur.eventapi.gdacs.service;

import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dto.ParsedEvent;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.service.XmlImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GdacsService extends XmlImportService {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsService.class);

    @Autowired
    public GdacsService(DataLakeDao dataLakeDao, GdacsDataLakeConverter dataLakeConverter, GdacsClient gdacsClient) {
        super(dataLakeDao, gdacsClient, dataLakeConverter);
    }

    public Optional<String> fetchGdacsXml() {
        try {
            return Optional.of(getClient().getXml());
        } catch (FeignException e) {
            LOG.warn("Gdacs cap xml has not found");
        }
        return Optional.empty();
    }

    @Override
    public List<DataLake> createDataLakes(Map<String, ParsedEvent> events, String provider) {
        var dataLakes = new ArrayList<DataLake>();

        for (String key : events.keySet()) {
            ParsedAlert alert = (ParsedAlert) events.get(key);
            var geometry = getGeometryToAlert(
                    alert.getEventType(),
                    alert.getEventId(),
                    alert.getCurrentEpisodeId(),
                    alert.getIdentifier());

            if (geometry.isPresent()) {
                GdacsDataLakeConverter converter = (GdacsDataLakeConverter) getDataLakeConverter();
                dataLakes.add(converter.convertGdacs(alert));
                dataLakes.add(converter.convertGdacsWithGeometry(alert, geometry.get()));
            }
        }
        dataLakes.sort(Comparator.comparing(DataLake::getLoadedAt));
        return dataLakes;
    }

    private Optional<String> getGeometryToAlert(String eventType, String eventId, String currentEpisodeId, String externalId) {
        try {
            return Optional.of(
                    getClient().getGeometryByLink(eventType, eventId, currentEpisodeId)
            );
        } catch (FeignException e) {
            LOG.warn("Geometry for gdacs alert has not found. identifier = {}", externalId);
        }
        return Optional.empty();
    }

}
