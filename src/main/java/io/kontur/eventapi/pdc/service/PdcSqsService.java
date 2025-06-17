package io.kontur.eventapi.pdc.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PdcSqsService {

    private final static Logger LOG = LoggerFactory.getLogger(PdcSqsService.class);
    private final DataLakeDao dataLakeDao;
    private final PdcDataLakeConverter pdcDataLakeConverter;
    private final PdcProductS3Service productS3Service;

    public PdcSqsService(DataLakeDao dataLakeDao,
                         PdcDataLakeConverter pdcDataLakeConverter,
                         PdcProductS3Service productS3Service) {
        this.dataLakeDao = dataLakeDao;
        this.pdcDataLakeConverter = pdcDataLakeConverter;
        this.productS3Service = productS3Service;
    }

    public  void saveMessage(String sqsMessage, String type, String messageId) {
        if (dataLakeDao.getDataLakesByExternalId(messageId).isEmpty()) {
            DataLake dataLake = pdcDataLakeConverter.convertSQSMessage(sqsMessage, type, messageId);
            dataLakeDao.storeEventData(dataLake);
        }
    }

    public void saveProduct(String productId, String sqsMessage) {
        LOG.debug("Storing product {}", productId);
        productS3Service.saveProduct(productId, sqsMessage);
    }

}
