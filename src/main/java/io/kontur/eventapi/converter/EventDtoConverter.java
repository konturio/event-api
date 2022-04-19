package io.kontur.eventapi.converter;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.resource.dto.EventDto;
import org.springframework.beans.BeanUtils;
import org.wololo.geojson.Feature;

import java.util.*;

public class EventDtoConverter {

    public static EventDto convert(FeedData dataDto) {
        EventDto eventDto = new EventDto();
        BeanUtils.copyProperties(dataDto, eventDto);
        return eventDto;
    }

    public static List<Feature> convertGeoJson(FeedData event) {
        List<Feature> features = new ArrayList<>();
        for (FeedEpisode episode : event.getEpisodes()) {
            for (Feature episodeFeature : episode.getGeometries().getFeatures()) {
                Map<String, Object> properties = new HashMap<>();
                for (var episodeFeatureProperty : episodeFeature.getProperties().entrySet()) {
                    properties.put("feature_" + episodeFeatureProperty.getKey(), episodeFeatureProperty.getValue());
                }
                properties.put("eventId", event.getEventId());
                properties.put("episode_name", episode.getName());
                properties.put("episode_properName", episode.getProperName());
                properties.put("episode_description", episode.getDescription());
                properties.put("episode_type", episode.getType());
                properties.put("episode_active", episode.getActive());
                properties.put("episode_severity", episode.getSeverity());
                properties.put("episode_startedA", episode.getStartedAt());
                properties.put("episode_endedAt", episode.getEndedAt());
                properties.put("episode_updatedAt", episode.getUpdatedAt());
                properties.put("episode_sourceUpdatedAt", episode.getSourceUpdatedAt());
                properties.put("episode_observations", episode.getObservations());
                properties.put("episode_episodeDetails", episode.getEpisodeDetails());
                properties.put("episode_urls", episode.getUrls());
                properties.put("episode_location", episode.getLocation());

                Feature feature = new Feature(episodeFeature.getGeometry(), properties);
                features.add(feature);
            }
        }
        return features;
    }

}
