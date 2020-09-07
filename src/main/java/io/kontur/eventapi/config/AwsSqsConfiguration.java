package io.kontur.eventapi.config;

import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableSqs
@Profile("!awsSqsDisabled")
public class AwsSqsConfiguration {

}
