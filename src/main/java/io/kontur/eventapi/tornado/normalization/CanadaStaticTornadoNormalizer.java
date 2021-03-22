package io.kontur.eventapi.tornado.normalization;

import io.kontur.eventapi.entity.DataLake;
import org.springframework.stereotype.Component;
import java.util.Map;
import static io.kontur.eventapi.tornado.job.StaticTornadoImportJob.CANADA_GOV;

@Component
public class CanadaStaticTornadoNormalizer extends StaticTornadoNormalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return CANADA_GOV.equals(dataLakeDto.getProvider());
    }

    @Override
    protected String createName(Map<String, Object> properties) {
        String nearestCity = (String) properties.get("nearest_city");
        String state = (String) properties.get("admin0");
        return "Tornado - " + nearestCity + ", " + state + ", Canada";
    }

    @Override
    protected String getSourceUpdatedAt() {
        return "20180816";
    }
}
