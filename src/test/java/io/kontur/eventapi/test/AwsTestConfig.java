package io.kontur.eventapi.test;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;

import static io.kontur.eventapi.EventapiApplicationTests.localStack;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@TestConfiguration
public class AwsTestConfig {

    @Bean
    public AmazonSQSAsync amazonSQS() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(localStack.getDefaultCredentialsProvider())
                .withEndpointConfiguration(localStack.getEndpointConfiguration(SQS))
                .build();
    }

    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(@Autowired AmazonSQSAsync amazonSQS) {
        return new QueueMessagingTemplate(amazonSQS);
    }
}