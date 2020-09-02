package io.kontur.eventapi.pdc.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import org.springframework.stereotype.Component;

@Component
public class PdcSqsService {

    private final DataLakeDao dataLakeDao;

    public PdcSqsService(DataLakeDao dataLakeDao) {
        this.dataLakeDao = dataLakeDao;
    }

    public  void saveMessage(String sqsMessage, String type, String messageId) {
        if (dataLakeDao.getDataLakesByExternalId(messageId).isEmpty()) {
            DataLake dataLake = PdcDataLakeConverter.convertSQSMessage(sqsMessage, type, messageId);
            dataLakeDao.storeEventData(dataLake);
        }
    }

}
