package io.kontur.eventapi.converter;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.resource.dto.EpisodeDto;
import io.kontur.eventapi.resource.dto.EventDto;
import org.springframework.beans.BeanUtils;
import org.wololo.geojson.Feature;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class EventDtoConverter {

    public static EventDto convert(FeedData dataDto) {
        EventDto eventDto = new EventDto();
        BeanUtils.copyProperties(dataDto, eventDto, "episodes");
        eventDto.setEpisodes(dataDto.getEpisodes().stream().map(EventDtoConverter::convert).collect(toList()));
        return eventDto;
    }

    private static EpisodeDto convert(FeedEpisode dataDto) {
        EpisodeDto episodeDto = new EpisodeDto();
        BeanUtils.copyProperties(dataDto, episodeDto);
        return episodeDto;
    }

    public static List<Feature> convertGeoJson(FeedData event) {
        List<Feature> features = new ArrayList<>();
        for (FeedEpisode episode : event.getEpisodes()) {
            if (episode.getGeometries() != null && episode.getGeometries().getFeatures() != null) {
                features.addAll(Arrays.stream(episode.getGeometries().getFeatures())
                        .map(episodeFeature -> createFeature(event, episode, episodeFeature)).toList());
            } else {
                features.add(createFeature(event, episode, new Feature(null, emptyMap())));
            }
        }
        return features;
    }

    private static Feature createFeature(FeedData event, FeedEpisode episode, Feature episodeFeature) {
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
        properties.put("episode_startedAt", episode.getStartedAt());
        properties.put("episode_endedAt", episode.getEndedAt());
        properties.put("episode_updatedAt", episode.getUpdatedAt());
        properties.put("episode_sourceUpdatedAt", episode.getSourceUpdatedAt());
        properties.put("episode_observations", episode.getObservations());
        properties.put("episode_episodeDetails", episode.getEpisodeDetails());
        properties.put("episode_urls", episode.getUrls());
        properties.put("episode_location", episode.getLocation());

        return new Feature(episodeFeature.getGeometry(), properties);
    }

}
