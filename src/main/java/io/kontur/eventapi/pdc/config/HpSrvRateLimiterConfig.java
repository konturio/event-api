package io.kontur.eventapi.pdc.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class HpSrvRateLimiterConfig {

    @Bean
    public Bucket bucket() {
        Refill refill = Refill.intervally(1, Duration.ofSeconds(1));
        Bandwidth limit = Bandwidth.classic(1, refill);
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

}
