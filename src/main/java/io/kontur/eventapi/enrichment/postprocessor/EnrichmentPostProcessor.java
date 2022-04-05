package io.kontur.eventapi.enrichment.postprocessor;

import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.job.Applicable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONReader;

import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public abstract class EnrichmentPostProcessor implements Applicable<Feed> {

    protected final static GeoJSONReader geoJSONReader = new GeoJSONReader();

    public abstract void process(FeedData event);

    protected Double toDouble(Object value) {
        try {
            return value == null ? 0.0 : (double) value;
        } catch (Exception e) {
            return (double) toLong(value);
        }
    }

    protected Long toLong(Object value) {
        try {
            return value == null ? 0L : (long) value;
        } catch (Exception e) {
            return (long) toInteger(value);
        }
    }

    protected Integer toInteger(Object value) {
        return value == null ? 0 : (int) value;
    }

    protected Set<Geometry> toGeometryCollection(FeatureCollection fc) {
        return Arrays.stream(fc.getFeatures())
                .map(Feature::getGeometry)
                .map(geoJSONReader::read)
                .collect(toSet());
    }

    protected Geometry unionGeometry(Set<Geometry> geometryCollection) {
        return OverlayNGRobust.union(geometryCollection);
    }
}
