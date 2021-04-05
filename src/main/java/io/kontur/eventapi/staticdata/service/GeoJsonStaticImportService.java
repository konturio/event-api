package io.kontur.eventapi.staticdata.service;


import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.OffsetDateTime;
import java.util.*;

@Component("geojson")
public class GeoJsonStaticImportService extends StaticImportService {

    public GeoJsonStaticImportService(DataLakeDao dataLakeDao) {
        super(dataLakeDao);
    }

    @Override
    public void saveDataLakes(String data, String provider, OffsetDateTime updatedAt) {
        FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(data);
        Feature[] features = featureCollection.getFeatures();

        List<DataLake> dataLakes = new ArrayList<>();
        for (Feature feature : features) {
            createDataLakeIfNotExists(feature.toString(), provider, updatedAt).ifPresent(dataLakes::add);
        }
        dataLakeDao.storeDataLakes(dataLakes);
    }
}
