package io.kontur.eventapi.health;

import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import static io.kontur.eventapi.gdacs.job.GdacsSearchJob.XML_PUB_DATE;

@Component
public class GdacsFeedXmlHealthIndicator extends AbstractHealthIndicator {
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        builder.up();
        var currentDateTime = DateTimeUtil.uniqueOffsetDateTime();
        if(currentDateTime.minusHours(12).isAfter(XML_PUB_DATE)){
            builder.down()
                    .withDetail("Gdacs feed xml did not updated last 12 hours. Last pubDate:", XML_PUB_DATE);
        }
    }
}
