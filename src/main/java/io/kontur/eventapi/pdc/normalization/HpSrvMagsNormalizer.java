package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.magsDateTimeFormatter;
import static io.kontur.eventapi.util.JsonUtil.readJson;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class HpSrvMagsNormalizer extends Normalizer {

    @Override
    public boolean isApplicable(DataLake dataLake) {
        return HpSrvSearchJob.HP_SRV_MAG_PROVIDER.equals(dataLake.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedDto = new NormalizedObservation();
        normalizedDto.setObservationId(dataLakeDto.getObservationId());
        normalizedDto.setExternalId(dataLakeDto.getExternalId());
        normalizedDto.setProvider(dataLakeDto.getProvider());
        normalizedDto.setLoadedAt(dataLakeDto.getLoadedAt());
        FeatureCollection fc = readJson(dataLakeDto.getData(), FeatureCollection.class);

        if (fc.getFeatures() != null && fc.getFeatures().length > 0) {
            List<Feature> features = Arrays.asList(fc.getFeatures());
            features.sort(Comparator.comparing(f -> readDateTime(f.getProperties(), "updateDate")));

            normalizedDto.setGeometries(writeJson(convertGeometries(features)));

            Map<String, Object> props = features.get(features.size() - 1).getProperties(); //take last updated feature
            normalizedDto.setName(readString(props, "hazard.hazardName"));
            normalizedDto.setEpisodeDescription(convertDescription(props));
            normalizedDto.setType(readString(props, "hazard.hazardType.typeId"));
            normalizedDto.setActive(readBoolean(props, "isActive"));

            normalizedDto.setStartedAt(readDateTime(props, "hazard.startDate"));
            normalizedDto.setEndedAt(readDateTime(props, "hazard.endDate"));
            normalizedDto.setUpdatedAt(readDateTime(props, "updateDate"));

            normalizedDto.setPoint(makeWktPoint(readDouble(props, "hazard.longitude"),
                    readDouble(props, "hazard.latitude")));
        }

        return normalizedDto;
    }

    private FeatureCollection convertGeometries(List<Feature> input) {
        List<Feature> features = new ArrayList<>(input.size());

        input.forEach(feature -> {
            Map<String, Object> props = feature.getProperties();
            Map<String, Object> map = new HashMap<>();

            map.put("description", convertDescription(props));
            map.put("active", readBoolean(props, "isActive"));
            map.put("updatedAt", readDateTime(props, "updateDate"));

            features.add(new Feature(feature.getGeometry(), map));
        });

        return new FeatureCollection(features.toArray(new Feature[0]));
    }

    private String convertDescription(Map<String, Object> props) {
        String commentText = readString(props, "hazard.commentText");
        commentText = commentText != null ? " | " + commentText : "";
        return readString(props, "title") + commentText;
    }

    @Override
    protected OffsetDateTime readDateTime(Map<String, Object> map, String key) {
        String dateTime = readString(map, key);
        return dateTime == null ? null : OffsetDateTime.parse(dateTime, magsDateTimeFormatter);
    }
}
