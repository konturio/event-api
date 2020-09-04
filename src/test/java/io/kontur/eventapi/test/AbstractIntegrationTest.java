package io.kontur.eventapi.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.localstack.LocalStackContainer;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AwsTestConfig.class)
public abstract class AbstractIntegrationTest {

    public static final LocalStackContainer localStack;

    static {
        localStack = new LocalStackContainer()
                .withServices(SQS)
                .withEnv("DEFAULT_REGION", "us-west-1");
        localStack.start();
    }
}
