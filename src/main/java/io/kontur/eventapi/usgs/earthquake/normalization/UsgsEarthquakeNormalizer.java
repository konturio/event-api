package io.kontur.eventapi.usgs.earthquake.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter;
import io.kontur.eventapi.util.JsonUtil;
import io.kontur.eventapi.util.GeometryUtil;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.util.GeometryUtil.*;

@Component
public class UsgsEarthquakeNormalizer extends Normalizer {

    private static final Map<String, String> NETWORKS = Map.ofEntries(
            Map.entry("admin", "USGS Administrative"),
            Map.entry("ak", "Alaska Earthquake Center"),
            Map.entry("at", "National Tsunami Warning Center"),
            Map.entry("atlas", "ATLAS seismic network"),
            Map.entry("av", "Alaska Volcano Observatory"),
            Map.entry("cgs", "California Geological Survey"),
            Map.entry("ci", "California Integrated Seismic Network (Caltech/USGS)"),
            Map.entry("ew", "Early Warning (ShakeAlert earthquake early warning)"),
            Map.entry("hv", "Hawaiian Volcano Observatory"),
            Map.entry("ismp", "Idaho Strong Motion Program"),
            Map.entry("ld", "Lamont-Doherty Cooperative Seismographic Network"),
            Map.entry("mb", "Montana Bureau of Mines and Geology"),
            Map.entry("nc", "Northern California Seismic System"),
            Map.entry("nm", "New Madrid Seismic Network"),
            Map.entry("nn", "Nevada Seismological Laboratory"),
            Map.entry("np", "National Strong-Motion Project"),
            Map.entry("official", "Official"),
            Map.entry("ok", "Oklahoma Geological Survey"),
            Map.entry("pr", "Puerto Rico Seismic Network"),
            Map.entry("pt", "Pacific Tsunami Warning Center"),
            Map.entry("se", "Center for Earthquake Research and Information"),
            Map.entry("tx", "Texas Seismological Network"),
            Map.entry("us", "USGS National Earthquake Information Center"),
            Map.entry("uu", "University of Utah Seismograph Stations"),
            Map.entry("uw", "Pacific Northwest Seismic Network")
    );

    private static Severity mapAlert(String alert) {
        if (alert == null) return Severity.UNKNOWN;
        return switch (alert) {
            case "green" -> Severity.MINOR;
            case "yellow" -> Severity.MODERATE;
            case "orange" -> Severity.SEVERE;
            case "red" -> Severity.EXTREME;
            default -> Severity.UNKNOWN;
        };
    }

    @Override
    public boolean isApplicable(DataLake dataLake) {
        return UsgsEarthquakeDataLakeConverter.USGS_EARTHQUAKE_PROVIDER.equals(dataLake.getProvider());
    }

    @Override
    @SuppressWarnings("unchecked")
    public NormalizedObservation normalize(DataLake dataLake) {
        Map<String, Object> feature = JsonUtil.readJson(dataLake.getData(), Map.class);
        NormalizedObservation obs = new NormalizedObservation();
        obs.setObservationId(dataLake.getObservationId());
        obs.setProvider(dataLake.getProvider());
        obs.setLoadedAt(dataLake.getLoadedAt());
        obs.setSourceUpdatedAt(dataLake.getUpdatedAt());
        obs.setEndedAt(dataLake.getUpdatedAt());
        obs.setActive(true);
        obs.setType(EventType.EARTHQUAKE);
        obs.setRecombined(false);
        obs.setNormalizedAt(OffsetDateTime.now());

        obs.setExternalEventId(readString(feature, "id"));

        Map<String, Object> props = (Map<String, Object>) feature.get("properties");
        if (props != null) {
            obs.setName(readString(props, "title"));
            obs.setDescription(readString(props, "place"));
            obs.setProperName(obs.getName() != null ? obs.getName() : obs.getDescription());
            obs.setStartedAt(readDateTime(props, "time"));
            obs.setEventSeverity(mapAlert(readString(props, "alert")));
            String net = readString(props, "net");
            if (net == null) {
                net = readString(props, "source");
            }
            if (net != null) {
                Map<String, String> originMap = Map.of(
                        "code", net,
                        "origin_name", NETWORKS.getOrDefault(net, net)
                );
                obs.setOrigin(JsonUtil.writeJson(originMap));
            }
            List<String> urls = new ArrayList<>();
            String url = readString(props, "url");
            String detail = readString(props, "detail");
            if (url != null) urls.add(url);
            if (detail != null) urls.add(detail);
            String shmUrl = readString(props, "shm_url");
            if (shmUrl != null) urls.add(shmUrl);
            String detailUrl = readString(feature, "detail_url");
            if (detailUrl != null) urls.add(detailUrl);
            String lossUrl = readString(feature, "loss_url");
            if (lossUrl != null) urls.add(lossUrl);
            obs.setUrls(urls);
            Map<String, Object> shakemap = (Map<String, Object>) props.get("shakemap");
            if (shakemap != null) {
                Map<String, Object> shaProps = (Map<String, Object>) shakemap.get("properties");
                if (shaProps != null) {
                    obs.setSeverityData(shaProps);
                }
            }
        }

        Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
        if (geometry != null) {
            List<?> coords = (List<?>) geometry.get("coordinates");
            if (coords != null && coords.size() >= 2) {
                Double lon = Double.valueOf(coords.get(0).toString());
                Double lat = Double.valueOf(coords.get(1).toString());
                Point point = new Point(new double[]{lon, lat});
                FeatureCollection fc = convertGeometryToFeatureCollection(point, Map.of(AREA_TYPE_PROPERTY, CENTER_POINT));
                obs.setGeometries(fc);
            }
        }
        return obs;
    }
}
