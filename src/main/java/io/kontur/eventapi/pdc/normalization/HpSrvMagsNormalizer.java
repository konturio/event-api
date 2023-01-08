package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.magsDateTimeFormatter;
import static io.kontur.eventapi.util.JsonUtil.readJson;

@Component
public class HpSrvMagsNormalizer extends PdcHazardNormalizer {

    @Override
    public boolean isApplicable(DataLake dataLake) {
        return HP_SRV_MAG_PROVIDER.equals(dataLake.getProvider());
    }

    @Override
    public NormalizedObservation runNormalization(DataLake dataLakeDto) {
        NormalizedObservation normalizedDto = new NormalizedObservation();
        normalizedDto.setObservationId(dataLakeDto.getObservationId());
        normalizedDto.setProvider(dataLakeDto.getProvider());
        normalizedDto.setLoadedAt(dataLakeDto.getLoadedAt());
        FeatureCollection fc = readJson(dataLakeDto.getData(), FeatureCollection.class);

        if (fc.getFeatures() != null && fc.getFeatures().length > 0) {
            List<Feature> features = Arrays.asList(fc.getFeatures());
            features.sort(Comparator.comparing(f -> readDateTime(f.getProperties(), "updateDate")));

            normalizedDto.setGeometries(convertGeometries(features));

            Map<String, Object> props = features.get(features.size() - 1).getProperties(); //take last updated feature
            normalizedDto.setExternalEventId(readString(props, "hazard.uuid"));
            normalizedDto.setExternalEpisodeId(readString(props, "uuid"));
            normalizedDto.setName(readString(props, "hazard.hazardName"));
            normalizedDto.setType(defineType(readString(props, "hazard.hazardType.typeId")));
            normalizedDto.setEventSeverity(Severity.UNKNOWN);
            normalizedDto.setActive(readBoolean(props, "isActive"));

            normalizedDto.setStartedAt(readDateTime(props, "hazard.startDate"));
            normalizedDto.setEndedAt(readDateTime(props, "hazard.endDate"));
            normalizedDto.setSourceUpdatedAt(readDateTime(props, "updateDate"));

            normalizedDto.setPoint(makeWktPoint(readDouble(props, "hazard.longitude"),
                    readDouble(props, "hazard.latitude")));
        }

        return normalizedDto;
    }

    private FeatureCollection convertGeometries(List<Feature> input) {
        List<Feature> features = new ArrayList<>(input.size());

        input.forEach(feature -> features.add(new Feature(feature.getGeometry(), MAG_PROPERTIES)));

        return new FeatureCollection(features.toArray(new Feature[0]));
    }

    @Override
    protected OffsetDateTime readDateTime(Map<String, Object> map, String key) {
        String dateTime = readString(map, key);
        return dateTime == null ? null : OffsetDateTime.parse(dateTime, magsDateTimeFormatter);
    }
}
