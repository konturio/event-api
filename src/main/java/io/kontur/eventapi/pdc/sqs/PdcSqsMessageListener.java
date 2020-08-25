package io.kontur.eventapi.pdc.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.kontur.eventapi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class PdcSqsMessageListener {

    private final Logger LOG = LoggerFactory.getLogger(PdcSqsMessageListener.class);

    private final DataLakeDao dataLakeDao;

    public PdcSqsMessageListener(DataLakeDao dataLakeDao) {
        this.dataLakeDao = dataLakeDao;
    }

    @SqsListener(value = "${aws.sqs.url}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void read(String sqsMessage, Acknowledgment acknowledgment) {
        LOG.debug("Message received: {}", sqsMessage);

        JsonNode root = JsonUtil.readTree(sqsMessage);
        JsonNode sns = root.get("Sns");
        JsonNode message = JsonUtil.readTree(sns.get("Message").asText());
        JsonNode event = JsonUtil.readTree(message.get("event").asText());
        JsonNode masterSyncEvents = event.get("syncDa").get("masterSyncEvents");

        String type = masterSyncEvents.get("type").asText();
        LOG.info("SQS Message received: Type: {}", type);
        if ("PING".equals(type)) {
            acknowledgment.acknowledge();
            return;
        } else if ("PRODUCT".equals(type)) {
            return; //skip products until it is clear how to handle them
        }

        String messageId = sns.get("MessageId").asText();

        if (dataLakeDao.getDataLakesByExternalId(messageId).isEmpty()) {
            DataLake dataLake = PdcDataLakeConverter.convertSQSMessage(sqsMessage, type, messageId);
            dataLakeDao.storeEventData(dataLake);
        }
        acknowledgment.acknowledge();
    }

}
