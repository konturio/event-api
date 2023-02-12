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
public class EpisodeDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 5078150019488329350L;

    private String name;
    private String properName;
    private String description;
    private EventType type;
    private Severity severity;
    private Boolean active;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime sourceUpdatedAt;
    private String location;
    private List<String> urls = new ArrayList<>();
    private Map<String, Object> episodeDetails;
    private Set<UUID> observations = new HashSet<>();
    private FeatureCollection geometries;

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = (String) in.readObject();
        properName = (String) in.readObject();
        description = (String) in.readObject();
        type = (EventType) in.readObject();
        severity = (Severity) in.readObject();
        active = (Boolean) in.readObject();
        startedAt = (OffsetDateTime) in.readObject();
        endedAt = (OffsetDateTime) in.readObject();
        updatedAt = (OffsetDateTime) in.readObject();
        sourceUpdatedAt = (OffsetDateTime) in.readObject();
        location = (String) in.readObject();
        urls = (List<String>) in.readObject();
        episodeDetails = (Map<String, Object>) in.readObject();
        observations = (Set<UUID>) in.readObject();
        Object geometriesObj = in.readObject();
        geometries = geometriesObj == null ? null : (FeatureCollection) GeoJSONFactory.create((String) geometriesObj);
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(name);
        out.writeObject(properName);
        out.writeObject(description);
        out.writeObject(type);
        out.writeObject(severity);
        out.writeObject(active);
        out.writeObject(startedAt);
        out.writeObject(endedAt);
        out.writeObject(updatedAt);
        out.writeObject(sourceUpdatedAt);
        out.writeObject(location);
        out.writeObject(urls);
        out.writeObject(episodeDetails);
        out.writeObject(observations);
        out.writeObject(geometries == null ? null : geometries.toString());
    }
}
