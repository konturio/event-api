package io.kontur.eventapi.resource.dto;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import lombok.Data;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;

@Data
public class EventDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 6806566181403544985L;

    private UUID eventId;
    private Long version;
    private String name;
    private String properName;
    private String description;
    private EventType type;
    private Severity severity;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private String location;
    private List<String> urls = new ArrayList<>();
    private Map<String, Object> eventDetails;
    private Set<UUID> observations = new HashSet<>();
    private FeatureCollection geometries;
    private List<EpisodeDto> episodes = new ArrayList<>();
    private List<Double> bbox = new ArrayList<>();
    private List<Double> centroid = new ArrayList<>();

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        eventId = (UUID) in.readObject();
        version = (Long) in.readObject();
        name = (String) in.readObject();
        properName = (String) in.readObject();
        description = (String) in.readObject();
        type = (EventType) in.readObject();
        severity = (Severity) in.readObject();
        startedAt = (OffsetDateTime) in.readObject();
        endedAt = (OffsetDateTime) in.readObject();
        updatedAt = (OffsetDateTime) in.readObject();
        location = (String) in.readObject();
        urls = (List<String>) in.readObject();
        eventDetails = (Map<String, Object>) in.readObject();
        observations = (Set<UUID>) in.readObject();
        Object geometriesObj = in.readObject();
        geometries = geometriesObj == null ? null : (FeatureCollection) GeoJSONFactory.create((String) geometriesObj);
        episodes = (List<EpisodeDto>) in.readObject();
        bbox = (List<Double>) in.readObject();
        centroid = (List<Double>) in.readObject();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(eventId);
        out.writeObject(version);
        out.writeObject(name);
        out.writeObject(properName);
        out.writeObject(description);
        out.writeObject(type);
        out.writeObject(severity);
        out.writeObject(startedAt);
        out.writeObject(endedAt);
        out.writeObject(updatedAt);
        out.writeObject(location);
        out.writeObject(urls);
        out.writeObject(eventDetails);
        out.writeObject(observations);
        out.writeObject(geometries == null ? null : geometries.toString());
        out.writeObject(episodes);
        out.writeObject(bbox);
        out.writeObject(centroid);
    }
}
