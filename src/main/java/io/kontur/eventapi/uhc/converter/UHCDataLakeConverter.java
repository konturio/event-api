package io.kontur.eventapi.uhc.converter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;

@Component
public class UHCDataLakeConverter {
    private static final Logger LOG = LoggerFactory.getLogger(UHCDataLakeConverter.class);

    public final static String UHC_PROVIDER = "kontur.events";

    public DataLake convertEvent(Feature feature, DataLakeDao dataLakeDao) {
        try {
            String externalId = String.valueOf(feature.getProperties().get("event_id"));
            String updatedAtValue = (String) feature.getProperties().get("updated_at");
            if (StringUtils.isNotBlank(updatedAtValue)) {
                OffsetDateTime updatedAt = DateTimeUtil.parseDateTimeByPattern(updatedAtValue,
                        DateTimeUtil.UHC_DATETIME_PATTERN);
                if (updatedAt != null && dataLakeDao.isNewEvent(externalId, UHC_PROVIDER,
                        updatedAt.format(DateTimeFormatter.ISO_INSTANT))) {
                    DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt,
                            DateTimeUtil.uniqueOffsetDateTime());
                    dataLake.setProvider(UHC_PROVIDER);
                    dataLake.setData(feature.toString());
                    return dataLake;
                }
            }
        } catch (Exception e) {
            LOG.error("Error while convert Humanitarian Crisis feature to data_lake. {}", e.getMessage());
        }
        return null;
    }
}
