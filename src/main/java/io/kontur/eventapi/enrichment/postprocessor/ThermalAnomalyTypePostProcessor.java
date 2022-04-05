package io.kontur.eventapi.enrichment.postprocessor;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.kontur.eventapi.enrichment.EnrichmentConfig.*;
import static io.kontur.eventapi.util.GeometryUtil.calculateAreaKm2;
import static java.util.Comparator.comparing;

@Component
public class ThermalAnomalyTypePostProcessor extends EnrichmentPostProcessor {

    private final static Map<EventType, String> typeNames = Map.of(
            EventType.INDUSTRIAL_HEAT, "Industrial heat",
            EventType.VOLCANO, "Volcano",
            EventType.WILDFIRE, "Wildfire"
    );

    @Override
    public void process(FeedData event) {
        event.getEpisodes()
                .stream()
                .filter(this::isApplicable)
                .forEach(episode -> {
                    double industrialAreaKm2 = toDouble(episode.getEpisodeDetails().get(INDUSTRIAL_AREA_KM2));
                    double forestAreaKm2 = toDouble(episode.getEpisodeDetails().get(FOREST_AREA_KM2));
                    double volcanoesCount = toDouble(episode.getEpisodeDetails().get(VOLCANOES_COUNT));
                    double hotspotDaysPerYearMax = toDouble(episode.getEpisodeDetails().get(HOTSPOT_DAYS_PER_YEAR_MAX));

                    double area = calculateAreaKm2(unionGeometry(toGeometryCollection(episode.getGeometries())));

                    if (industrialAreaKm2 > 0) {
                        episode.setType(EventType.INDUSTRIAL_HEAT);
                    } else if (hotspotDaysPerYearMax > 70 && volcanoesCount > 0 && industrialAreaKm2 == 0) {
                        episode.setType(EventType.VOLCANO);
                    } else if (forestAreaKm2 / area > 0.5) {
                        episode.setType(EventType.WILDFIRE);
                    } else {
                        episode.setType(EventType.THERMAL_ANOMALY);
                    }
                    episode.setName(updateName(episode.getName(), episode.getType()));
                });
        event.setName(event.getEpisodes()
                .stream()
                .max(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getName)
                .orElse(event.getName()));
    }

    @Override
    public boolean isApplicable(Feed feed) {
        return feed.getEnrichmentPostProcessors() != null
                && feed.getEnrichmentPostProcessors().contains(WILDFIRE_TYPE_POSTPROCESSOR);
    }

    public boolean isApplicable(FeedEpisode episode) {
        return episode.getType() == EventType.THERMAL_ANOMALY
                && episode.getEpisodeDetails() != null
                && episode.getEpisodeDetails().containsKey(INDUSTRIAL_AREA_KM2)
                && episode.getEpisodeDetails().containsKey(FOREST_AREA_KM2)
                && episode.getEpisodeDetails().containsKey(VOLCANOES_COUNT)
                && episode.getEpisodeDetails().containsKey(HOTSPOT_DAYS_PER_YEAR_MAX);
    }

    private String updateName(String oldName, EventType type) {
        return typeNames.containsKey(type) ? oldName.replace("Thermal anomaly", typeNames.get(type)) : oldName;
    }
}
