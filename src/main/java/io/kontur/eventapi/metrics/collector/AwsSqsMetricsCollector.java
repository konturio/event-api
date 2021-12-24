package io.kontur.eventapi.metrics.collector;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import io.kontur.eventapi.metrics.MetricCollector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.math.NumberUtils.toInt;

@Component
@Profile("!awsSqsDisabled")
public class AwsSqsMetricsCollector implements MetricCollector {

    @Value("${aws.sqs.enabled:true}")
    private Boolean awsEnable;

    @Value("${aws.sqs.url}")
    private String awsUrl;

    @Value("${aws.sqs.dlqUrl}")
    private String awsDLQUrl;

    private final AtomicInteger sqsQueueSize;
    private final AtomicInteger sqsDLQueueSize;

    private final AmazonSQS sqs;

    public AwsSqsMetricsCollector(AtomicInteger sqsQueueSize, AtomicInteger sqsDLQueueSize, AmazonSQS sqs) {
        this.sqsQueueSize = sqsQueueSize;
        this.sqsDLQueueSize = sqsDLQueueSize;
        this.sqs = sqs;
    }

    @Override
    public void collect() {
        if (awsEnable) {
            sqsQueueSize.set(getSqsQueueSize(awsUrl));
            sqsDLQueueSize.set(getSqsQueueSize(awsDLQUrl));
        }
    }

    private int getSqsQueueSize(String url) {
        GetQueueAttributesRequest request = new GetQueueAttributesRequest(url).withAttributeNames("All");
        GetQueueAttributesResult result = sqs.getQueueAttributes(request);
        return toInt(result.getAttributes().get("ApproximateNumberOfMessages"));
    }
}
