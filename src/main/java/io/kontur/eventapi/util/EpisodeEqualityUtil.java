package io.kontur.eventapi.util;

import io.kontur.eventapi.entity.FeedEpisode;
import org.apache.commons.lang3.StringUtils;
import org.wololo.geojson.FeatureCollection;

import java.util.Objects;

/**
 * Utility methods for comparing {@link FeedEpisode} instances.
 * This encapsulates equality rules so all feeds use the same criteria
 * when deciding whether two episodes represent the same event.
 */
public final class EpisodeEqualityUtil {

    private EpisodeEqualityUtil() {
    }

    /**
     * Compare two episodes to determine if they should be treated as the same.
     * The comparison checks name, location and geometry case-insensitively,
     * severity equality and loss equality, mirroring PDC/NIFC combinator logic.
     *
     * @param episode1 first episode
     * @param episode2 the second episode
     * @return {@code true} if episodes are considered the same
     */
    public static boolean areSame(FeedEpisode episode1, FeedEpisode episode2) {
        FeatureCollection geom1 = episode1.getGeometries();
        FeatureCollection geom2 = episode2.getGeometries();
        boolean geometriesEqual = (geom1 == null && geom2 == null)
                || (geom1 != null && geom2 != null && GeometryUtil.isEqualGeometries(geom1, geom2));

        return StringUtils.equalsIgnoreCase(episode1.getName(), episode2.getName())
                && Objects.equals(episode1.getLoss(), episode2.getLoss())
                && Objects.equals(episode1.getSeverity(), episode2.getSeverity())
                && StringUtils.equalsIgnoreCase(episode1.getLocation(), episode2.getLocation())
                && geometriesEqual;
    }
}
