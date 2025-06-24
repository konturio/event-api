package io.kontur.eventapi.usgs.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.usgs.client.UsgsClient;
import io.kontur.eventapi.usgs.converter.UsgsShakeMapDataLakeConverter;
import org.springframework.stereotype.Service;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.kontur.eventapi.usgs.converter.UsgsShakeMapDataLakeConverter.USGS_SHAKEMAP_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;

@Service
public class UsgsShakeMapService {

    private final UsgsClient client;
    private final DataLakeDao dataLakeDao;
    private final UsgsShakeMapDataLakeConverter converter;

    public UsgsShakeMapService(UsgsClient client, DataLakeDao dataLakeDao, UsgsShakeMapDataLakeConverter converter) {
        this.client = client;
        this.dataLakeDao = dataLakeDao;
        this.converter = converter;
    }

    public void importLatestShakeMaps() {
        String feed = client.getShakeMapEvents("geojson", "time", "shakemap", 20, 4.5);
        FeatureCollection fc = (FeatureCollection) GeoJSONFactory.create(feed);
        Set<String> ids = Arrays.stream(fc.getFeatures()).map(Feature::getId).collect(Collectors.toSet());
        Map<String, DataLake> exists = new HashMap<>();
        dataLakeDao.getDataLakesByExternalIdsAndProvider(ids, USGS_SHAKEMAP_PROVIDER)
                .forEach(dl -> exists.put(dl.getExternalId(), dl));
        List<DataLake> toStore = new ArrayList<>();
        for (Feature feature : fc.getFeatures()) {
            String id = feature.getId();
            if (exists.containsKey(id)) continue;
            String detail = client.getEvent(id, "geojson", "shakemap");
            OffsetDateTime updated = getDateTimeFromMilli(((Number) feature.getProperties().get("updated")).longValue());
            DataLake dl = converter.convertDataLake(id, updated, detail);
            toStore.add(dl);
        }
        if (!toStore.isEmpty()) {
            dataLakeDao.storeDataLakes(toStore);
        }
    }
}
