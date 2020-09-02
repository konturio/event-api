package io.kontur.eventapi.test;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AwsTestConfig {

    public static String SQS_QUEUE_NAME = "LocalStack-test-queue";

    @Value("${embedded.localstack.accessKey}")
    private String accessKey;

    @Value("${embedded.localstack.secretKey}")
    private String secretKey;

    @Value("${embedded.localstack.SQS}")
    private String sqsEndpoint;

    @Value("${embedded.localstack.region}")
    private String region;

    @Bean
    public AmazonSQSAsync amazonSQS() {
        AmazonSQSAsync sqs = AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(getEndpointConfiguration())
                .withCredentials(getAwsCredentialsProvider())
                .build();
        sqs.createQueue(SQS_QUEUE_NAME);

        return sqs;
    }

    private AwsClientBuilder.EndpointConfiguration getEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(sqsEndpoint, region);
    }

    private AWSCredentialsProvider getAwsCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
    }

    @Bean
    public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(
            AmazonSQSAsync amazonSQSAsync) {
        SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
        factory.setAmazonSqs(amazonSQSAsync);
        factory.setWaitTimeOut(1);
        return factory;
    }

    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(@Autowired AmazonSQSAsync amazonSQS) {
        return new QueueMessagingTemplate(amazonSQS);
    }
}