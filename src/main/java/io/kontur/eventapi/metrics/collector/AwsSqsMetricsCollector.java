package io.kontur.eventapi.metrics.collector;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kontur.eventapi.metrics.MetricCollector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.math.NumberUtils.toInt;

@Component
@Profile("!awsSqsDisabled")
public class AwsSqsMetricsCollector implements MetricCollector {

    private static final Logger LOG = LoggerFactory.getLogger(AwsSqsMetricsCollector.class);
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
            sqsQueueSize.set(safeGetSqsQueueSize(awsUrl, sqsQueueSize.get()));
            sqsDLQueueSize.set(safeGetSqsQueueSize(awsDLQUrl, sqsDLQueueSize.get()));
        }
    }

    private int safeGetSqsQueueSize(String url, int defaultValue) {
        try {
            return getSqsQueueSize(url);
        } catch (Exception e) {
            LOG.warn("Unable to retrieve SQS queue size from {}: {}", url, e.getMessage());
            return defaultValue;
        }
    }

    private int getSqsQueueSize(String url) {
        GetQueueAttributesRequest request = new GetQueueAttributesRequest(url).withAttributeNames("All");
        GetQueueAttributesResult result = sqs.getQueueAttributes(request);
        return toInt(result.getAttributes().get("ApproximateNumberOfMessages"));
    }
}
