package io.kontur.eventapi.converter;

import io.kontur.eventapi.resource.dto.EpisodeDto;
import io.kontur.eventapi.resource.dto.EventDto;
import org.wololo.geojson.Feature;

import java.util.*;

import static java.util.Collections.emptyMap;

public class EventDtoConverter {

    public static List<Feature> convertGeoJson(EventDto event) {
        List<Feature> features = new ArrayList<>();
        for (EpisodeDto episode : event.getEpisodes()) {
            if (episode.getGeometries() != null && episode.getGeometries().getFeatures() != null) {
                features.addAll(Arrays.stream(episode.getGeometries().getFeatures())
                        .map(episodeFeature -> createFeature(event, episode, episodeFeature)).toList());
            } else {
                features.add(createFeature(event, episode, new Feature(null, emptyMap())));
            }
        }
        return features;
    }

    private static Feature createFeature(EventDto event, EpisodeDto episode, Feature episodeFeature) {
        Map<String, Object> properties = new HashMap<>();
        for (var episodeFeatureProperty : episodeFeature.getProperties().entrySet()) {
            properties.put("feature_" + episodeFeatureProperty.getKey(), episodeFeatureProperty.getValue());
        }
        properties.put("eventId", event.getEventId());
        properties.put("episode_name", episode.getName());
        properties.put("episode_properName", episode.getProperName());
        properties.put("episode_description", episode.getDescription());
        properties.put("episode_type", episode.getType());
        properties.put("episode_severity", episode.getSeverity());
        properties.put("episode_active", episode.getActive());
        properties.put("episode_startedAt", episode.getStartedAt());
        properties.put("episode_endedAt", episode.getEndedAt());
        properties.put("episode_updatedAt", episode.getUpdatedAt());
        properties.put("episode_sourceUpdatedAt", episode.getSourceUpdatedAt());
        properties.put("episode_location", episode.getLocation());
        properties.put("episode_urls", episode.getUrls());
        properties.put("episode_episodeDetails", episode.getEpisodeDetails());
        properties.put("episode_observations", episode.getObservations());

        return new Feature(episodeFeature.getGeometry(), properties);
    }

}
