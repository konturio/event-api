package io.kontur.eventapi.tornado.normalization;

import io.kontur.eventapi.entity.DataLake;
import org.springframework.stereotype.Component;
import java.util.Map;

import static io.kontur.eventapi.tornado.job.StaticTornadoImportJob.AUSTRALIAN_BM;

@Component
public class AustraliaStaticTornadoNormalizer extends StaticTornadoNormalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return AUSTRALIAN_BM.equals(dataLakeDto.getProvider());
    }

    @Override
    protected String createName(Map<String, Object> properties) {
        String nearestCity = (String) properties.get("nearest_city");
        return "Tornado - " + nearestCity + ", Australia";
    }

    @Override
    protected String getSourceUpdatedAt() {
        return "20200101";
    }
}
