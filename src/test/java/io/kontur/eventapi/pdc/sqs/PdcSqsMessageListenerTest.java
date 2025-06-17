package io.kontur.eventapi.pdc.sqs;

import io.kontur.eventapi.pdc.service.PdcSqsService;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PdcSqsMessageListenerTest {

    private static PdcSqsService sqsService;
    private static PdcSqsMessageListener messageListener;

    @BeforeAll
    public static void init() {
        sqsService = mock(PdcSqsService.class);
        messageListener = new PdcSqsMessageListener(sqsService);
    }

    @BeforeEach
    public void reset() {
        Mockito.reset(sqsService);
    }

    @Test
    public void testSqsMessagesAcknowledged() throws NoSuchMethodException {
        Class<PdcSqsMessageListener> pdcSqsMessageListenerClass = PdcSqsMessageListener.class;
        Method method = pdcSqsMessageListenerClass.getMethod("read", String.class);
        SqsListener sqsListenerAnnotation = method.getAnnotation(SqsListener.class);

        assertEquals(SqsMessageDeletionPolicy.ON_SUCCESS.name(), sqsListenerAnnotation.deletionPolicy().name(),
                "Deletion policy for the listener method should be declared as ON_SUCCESS");
    }

    @Test
    public void testReceiveHazard() throws IOException {
        String json = readMessageFromFile("testhazard01.json");
        messageListener.read(json);

        verify(sqsService, times(1)).saveMessage(json, "HAZARD", "07cc96ec-7260-5a26-9fad-c1d63113b1f1");
    }

    @Test
    public void testReceiveMag() throws IOException {
        String json = readMessageFromFile("testmag01.json");
        messageListener.read(json);

        verify(sqsService, times(1)).saveMessage(json, "MAG", "41275851-547e-5b43-a278-0f6b1745e230");
    }

    @Test
    public void testReceivePing() throws IOException {
        String json = readMessageFromFile("testping01.json");
        messageListener.read(json);

        verify(sqsService, never()).saveMessage(anyString(), anyString(), anyString());
    }

    @Test
    public void testReceiveProduct() throws IOException {
        String json = readMessageFromFile("testproduct01.json");
        messageListener.read(json);

        verify(sqsService, times(1)).saveProduct("12776eb9-2585-4d93-9001-944cc7d9d022", json);
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}