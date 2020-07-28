package io.kontur.eventapi.pdc.config;

import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfiguration {

    @Value("${pdc.user}")
    private String hpSrvUser;

    @Value("${pdc.password}")
    private String hpSrvPassword;

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor()  {
        return new BasicAuthRequestInterceptor(hpSrvUser, hpSrvPassword);
    }
}
