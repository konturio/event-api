package io.kontur.eventapi.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAgentFeignConfig {

    @Value("${userAgent:Event API}")
    private String userAgent;

    @Bean
    public RequestInterceptor userAgentInterceptor() {
        return template -> template.header("User-Agent", userAgent);
    }
}
