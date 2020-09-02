package io.kontur.eventapi.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AwsTestConfig.class)
public class AbstractIntegrationTest {

}
