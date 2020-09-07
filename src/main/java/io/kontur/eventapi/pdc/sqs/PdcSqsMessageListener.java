package io.kontur.eventapi.pdc.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.pdc.service.PdcSqsService;
import io.kontur.eventapi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class PdcSqsMessageListener {

    private final Logger LOG = LoggerFactory.getLogger(PdcSqsMessageListener.class);

    private final PdcSqsService sqsService;

    public PdcSqsMessageListener(PdcSqsService sqsService) {
        this.sqsService = sqsService;
    }

    @SqsListener(value = "${aws.sqs.url}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void read(String sqsMessage) {
        LOG.debug("Message received: {}", sqsMessage);

        JsonNode sns = JsonUtil.readTree(sqsMessage).get("Sns");

        String type = getProductType(sns);
        if ("PING".equals(type)) {
            return;
        } else if ("PRODUCT".equals(type)) {
            return; //TODO skip products until it is clear how to handle them
        }

        String messageId = getMessageId(sns);
        sqsService.saveMessage(sqsMessage, type, messageId);
    }

    private String getProductType(JsonNode sns) {
        JsonNode message = JsonUtil.readTree(sns.get("Message").asText());
        JsonNode event = JsonUtil.readTree(message.get("event").asText());
        JsonNode masterSyncEvents = event.get("syncDa").get("masterSyncEvents");

        return masterSyncEvents.get("type").asText();
    }

    private String getMessageId(JsonNode sns) {
        return sns.get("MessageId").asText();
    }

}
