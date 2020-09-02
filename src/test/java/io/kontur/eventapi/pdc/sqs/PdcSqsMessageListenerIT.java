package io.kontur.eventapi.pdc.sqs;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import io.kontur.eventapi.pdc.service.PdcSqsService;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;

import java.io.IOException;

import static io.kontur.eventapi.test.AwsTestConfig.SQS_QUEUE_NAME;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PdcSqsMessageListenerIT extends AbstractIntegrationTest {

    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;

    @MockBean
    private PdcSqsService sqsService;

    @SpyBean
    private AmazonSQSAsync amazonSQSAsync;

    @SpyBean
    private PdcSqsMessageListener sqsMessageListener;

    @Test
    public void testReceiveHazard() throws IOException {
        String json = sendMessageFromFileAndWaitForListenerToRead("testhazard01.json");

        verify(sqsService, times(1)).saveMessage(json, "HAZARD", "07cc96ec-7260-5a26-9fad-c1d63113b1f1");
        verify(amazonSQSAsync, times(1)).deleteMessageAsync(any(DeleteMessageRequest.class));
    }

    @Test
    public void testReceiveMag() throws IOException {
        String json = sendMessageFromFileAndWaitForListenerToRead("testmag01.json");

        verify(sqsService, times(1)).saveMessage(json, "MAG", "41275851-547e-5b43-a278-0f6b1745e230");
        verify(amazonSQSAsync, times(1)).deleteMessageAsync(any(DeleteMessageRequest.class));
    }

    @Test
    public void testReceivePing() throws IOException {
        sendMessageFromFileAndWaitForListenerToRead("testping01.json");

        verify(sqsService, never()).saveMessage(anyString(), anyString(), anyString());
        verify(amazonSQSAsync, times(1)).deleteMessageAsync(any(DeleteMessageRequest.class));
    }

    @Test
    public void testReceiveProduct() throws IOException {
        sendMessageFromFileAndWaitForListenerToRead("testproduct01.json");

        verify(sqsService, never()).saveMessage(anyString(), anyString(), anyString());
        verify(amazonSQSAsync, times(1)).deleteMessageAsync(any(DeleteMessageRequest.class));
    }

    private String sendMessageFromFileAndWaitForListenerToRead(String fileName) throws IOException {
        String message = IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
        queueMessagingTemplate.convertAndSend(SQS_QUEUE_NAME, message);

        given().await()
                .atMost(5, SECONDS)
                .untilAsserted(() -> verify(sqsMessageListener).read(anyString(), any()));

        return message;
    }

}