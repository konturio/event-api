package io.kontur.eventapi.jtwc.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.jtwc.JtwcUtil;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class JtwcImportService {

    private static final Logger LOG = LoggerFactory.getLogger(JtwcImportService.class);

    private final DataLakeDao dataLakeDao;

    public JtwcImportService(DataLakeDao dataLakeDao) {
        this.dataLakeDao = dataLakeDao;
    }

    public void storeText(String text, OffsetDateTime updatedAt) {
        String externalId = DigestUtils.md5Hex(text);
        try {
            if (dataLakeDao.isNewEvent(externalId, JtwcUtil.JTWC_PROVIDER,
                    updatedAt.format(DateTimeFormatter.ISO_INSTANT))) {
                DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt,
                        DateTimeUtil.uniqueOffsetDateTime());
                dataLake.setProvider(JtwcUtil.JTWC_PROVIDER);
                dataLake.setData(text);
                dataLakeDao.storeEventData(dataLake);
            }
        } catch (Exception e) {
            LOG.warn("Failed to store JTWC record: {}", e.getMessage());
        }
    }
}
