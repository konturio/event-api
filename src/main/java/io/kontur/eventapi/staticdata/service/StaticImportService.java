package io.kontur.eventapi.staticdata.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.codec.digest.DigestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class StaticImportService {

    protected DataLakeDao dataLakeDao;

    public StaticImportService(DataLakeDao dataLakeDao) {
        this.dataLakeDao = dataLakeDao;
    }

    public abstract void saveDataLakes(String content, String provider, OffsetDateTime updatedAt);

    protected Optional<DataLake> createDataLakeIfNotExists(String data, String provider, OffsetDateTime updatedAt) {
        String externalId = DigestUtils.md5Hex(data);
        if (dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, provider).isEmpty()) {
            DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
            dataLake.setProvider(provider);
            dataLake.setData(data);
            return Optional.of(dataLake);
        }
        return Optional.empty();
    }
}
