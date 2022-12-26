package io.kontur.eventapi.inciweb.normalization;

import static io.kontur.eventapi.inciweb.job.InciWebImportJob.INCIWEB_PROVIDER;
import static io.kontur.eventapi.util.GeometryUtil.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.inciweb.converter.InciWebXmlParser;
import io.kontur.eventapi.normalization.Normalizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Point;

@Component
public class InciWebNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(InciWebNormalizer.class);

    public final static Map<String, Object> INCIWEB_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, START_POINT, IS_OBSERVED_PROPERTY, true);

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return INCIWEB_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public Optional<NormalizedObservation> normalize(DataLake dataLakeDto) {
        Optional<CapParsedItem> parsedItem = new InciWebXmlParser().getParsedItem(dataLakeDto.getData());
        if (parsedItem.isPresent()) {
            NormalizedObservation normalizedObservation = new NormalizedObservation();

            normalizedObservation.setObservationId(dataLakeDto.getObservationId());
            normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());
            normalizedObservation.setProvider(dataLakeDto.getProvider());
            normalizedObservation.setEventSeverity(Severity.UNKNOWN);
            normalizedObservation.setName(parsedItem.get().getTitle());
            normalizedObservation.setDescription(parsedItem.get().getDescription());
            normalizedObservation.setType(EventType.WILDFIRE);
            normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
            normalizedObservation.setStartedAt(dataLakeDto.getUpdatedAt());
            normalizedObservation.setEndedAt(dataLakeDto.getUpdatedAt());
            normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
            if (StringUtils.isNotBlank(parsedItem.get().getLink())) {
                normalizedObservation.setUrls(List.of(parsedItem.get().getLink()));
            }

            Point point = new Point(new double[] {parsedItem.get().getLongitude(), parsedItem.get().getLatitude()});
            normalizedObservation.setGeometries(convertGeometryToFeatureCollection(point, INCIWEB_PROPERTIES));
            normalizedObservation.setPoint(
                    makeWktPoint(parsedItem.get().getLongitude(), parsedItem.get().getLatitude()));

            return Optional.of(normalizedObservation);
        } else {
            LOG.error("Can't parse input source for normalization for event: {}", dataLakeDto.getExternalId());
        }
        return null;
    }
}
