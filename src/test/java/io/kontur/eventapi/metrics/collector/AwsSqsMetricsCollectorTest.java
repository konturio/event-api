package io.kontur.eventapi.metrics.collector;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsSqsMetricsCollectorTest {

    @Test
    public void collectShouldNotFailWhenSdkClientExceptionOccurs() {
        AmazonSQS sqs = mock(AmazonSQS.class);
        AtomicInteger mainGauge = new AtomicInteger(1);
        AtomicInteger dlqGauge = new AtomicInteger(2);

        AwsSqsMetricsCollector collector = new AwsSqsMetricsCollector(mainGauge, dlqGauge, sqs);
        ReflectionTestUtils.setField(collector, "awsEnable", true);
        ReflectionTestUtils.setField(collector, "awsUrl", "mainUrl");
        ReflectionTestUtils.setField(collector, "awsDLQUrl", "dlqUrl");

        GetQueueAttributesResult result = new GetQueueAttributesResult();
        result.setAttributes(Map.of("ApproximateNumberOfMessages", "5"));

        when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
                .thenThrow(new SdkClientException("test"))
                .thenReturn(result);

        assertDoesNotThrow(collector::collect);

        assertEquals(1, mainGauge.get());
        assertEquals(5, dlqGauge.get());
    }
}
