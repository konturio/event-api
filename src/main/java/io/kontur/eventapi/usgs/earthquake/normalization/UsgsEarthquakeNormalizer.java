package io.kontur.eventapi.usgs.earthquake.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter;
import io.kontur.eventapi.util.JsonUtil;
import io.kontur.eventapi.util.GeometryUtil;
import io.kontur.eventapi.dao.ShakemapDao;
import static io.kontur.eventapi.util.SeverityUtil.PGA40_MASK;
import static io.kontur.eventapi.util.SeverityUtil.COVERAGE_PGA_HIGHRES;
import org.wololo.geojson.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.util.GeometryUtil.*;

@Component
public class UsgsEarthquakeNormalizer extends Normalizer {

    private static final Logger LOG = LoggerFactory.getLogger(UsgsEarthquakeNormalizer.class);

    private static final DateTimeFormatter DESCRIPTION_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US);

    private final ShakemapDao shakemapDao;

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

    public UsgsEarthquakeNormalizer(ShakemapDao shakemapDao) {
        this.shakemapDao = shakemapDao;
    }

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
        LOG.debug("Start normalization of USGS earthquake {}", dataLake.getExternalId());
        Map<String, Object> feature = JsonUtil.readJson(dataLake.getData(), Map.class);
        NormalizedObservation obs = new NormalizedObservation();
        List<Feature> geometryFeatures = new ArrayList<>();
        Double depthKm = null;
        Double magnitude = null;
        Double maxPga = null;
        Severity eventSeverity = Severity.UNKNOWN;
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
            String place = readString(props, "place");
            obs.setDescription(place);
            magnitude = readDouble(props, "mag");
            if (magnitude != null && place != null) {
                obs.setName(String.format(Locale.US, "M %.1f - %s", magnitude, place));
            } else if (magnitude != null) {
                obs.setName(String.format(Locale.US, "M %.1f", magnitude));
            } else if (place != null) {
                obs.setName(place);
            }
            obs.setProperName(null);
            obs.setStartedAt(readDateTime(props, "time"));

            if (place != null) {
                int idx = place.lastIndexOf(',');
                obs.setRegion(idx >= 0 ? place.substring(idx + 1).trim() : place);
            }

            Integer sig = readInt(props, "sig");
            eventSeverity = defineSeverityFromSig(sig);
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
            Object smObj = feature.get("shakemap");
            Map<String, Object> shakemap = null;
            if (smObj instanceof List<?> list) {
                LOG.debug("ShakeMap is array with {} item(s)", list.size());
                if (!list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                    shakemap = (Map<String, Object>) first;
                } else {
                    LOG.debug("ShakeMap array is empty or first element is not a map");
                }
            } else if (smObj instanceof Map<?, ?>) {
                LOG.debug("ShakeMap is an object");
                shakemap = (Map<String, Object>) smObj;
            } else if (smObj != null) {
                LOG.debug("Unexpected ShakeMap type: {}", smObj.getClass());
            } else {
                LOG.debug("No ShakeMap found in root object");
            }
            if (shakemap != null) {
                Map<String, Object> shaProps = (Map<String, Object>) shakemap.get("properties");
                if (shaProps != null) {
                    maxPga = enrichPgaMask(shakemap, shaProps);
                    obs.setSeverityData(shaProps);
                }

                FeatureCollection smPolygons = buildShakemapPolygons(shakemap);
                if (smPolygons != null) {
                    Feature[] smFeatures = smPolygons.getFeatures();
                    if (smFeatures != null) {
                        for (Feature smFeature : smFeatures) {
                            Map<String, Object> polygonProps = smFeature.getProperties() == null
                                    ? new HashMap<>()
                                    : new HashMap<>(smFeature.getProperties());

                        Object valObj = polygonProps.get("value");
                        String intensity = null;
                        if (valObj != null) {
                            try {
                                double val = Double.parseDouble(valObj.toString());
                                intensity = (val % 1 == 0)
                                        ? String.valueOf((int) val)
                                        : String.format(Locale.US, "%.1f", val);
                            } catch (NumberFormatException ignored) {
                                intensity = valObj.toString();
                            }
                        }

                        if (intensity != null) {
                            polygonProps.put("Class", "Poly_SMPInt_" + intensity);
                            polygonProps.put("eventid", dataLake.getExternalId());
                            polygonProps.put("eventtype", "EQ");
                            polygonProps.put("polygonlabel", "Intensity " + intensity);
                        }

                            geometryFeatures.add(new Feature(smFeature.getGeometry(), polygonProps));
                        }
                        LOG.debug("Appended {} ShakeMap polygon(s)", smFeatures.length);
                    } else {
                        LOG.debug("ShakeMap polygons feature array is null");
                    }
                } else {
                    LOG.debug("No ShakeMap polygons were built");
                }
            }
        }

        Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
        if (geometry != null) {
            List<?> coords = (List<?>) geometry.get("coordinates");
            if (coords != null && coords.size() >= 2) {
                Double lon = Double.valueOf(coords.get(0).toString());
                Double lat = Double.valueOf(coords.get(1).toString());
                if (coords.size() >= 3) {
                    depthKm = Double.valueOf(coords.get(2).toString());
                }
                Point point = new Point(new double[]{lon, lat});
                FeatureCollection fc = convertGeometryToFeatureCollection(point, Map.of(AREA_TYPE_PROPERTY, CENTER_POINT));
                geometryFeatures.addAll(Arrays.asList(fc.getFeatures()));

                try {
                    String circleJson = shakemapDao.buildCentroidBuffer(lon, lat);
                    if (circleJson != null) {
                        org.wololo.geojson.Geometry circle = JsonUtil.readJson(circleJson, org.wololo.geojson.Geometry.class);
                        Map<String, Object> circleProps = new HashMap<>();
                        circleProps.put("Class", "Poly_Circle");
                        circleProps.put("eventid", dataLake.getExternalId());
                        circleProps.put("areaType", ALERT_AREA);
                        circleProps.put("eventtype", "EQ");
                        circleProps.put("polygonlabel", "100km");
                        geometryFeatures.add(new Feature(circle, circleProps));
                        LOG.debug("Appended 100km buffer polygon");
                    } else {
                        LOG.debug("buildCentroidBuffer returned null JSON");
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to build 100km buffer polygon", e);
                }
            }
        }

        if (obs.getStartedAt() != null) {
            String dateStr = obs.getStartedAt().format(DESCRIPTION_DATE_FORMATTER);
            StringBuilder sb = new StringBuilder();
            sb.append("On ").append(dateStr).append(", an earthquake occurred ");
            if (obs.getDescription() != null) {
                sb.append(obs.getDescription());
            }
            sb.append(". The earthquake had");
            if (magnitude != null) {
                sb.append(" Magnitude ").append(magnitude).append("M,");
            }
            if (depthKm != null) {
                sb.append(" Depth:").append(depthKm).append("km.");
            } else {
                if (sb.charAt(sb.length() - 1) == ',') {
                    sb.setCharAt(sb.length() - 1, '.');
                } else {
                    sb.append('.');
                }
            }
            obs.setEpisodeDescription(sb.toString());
        }

        if (!geometryFeatures.isEmpty()) {
            obs.setGeometries(new FeatureCollection(geometryFeatures.toArray(new Feature[0])));
        }

        if ((maxPga != null && maxPga >= 0.4) || (magnitude != null && magnitude >= 7.5)) {
            if (eventSeverity.getValue() < Severity.SEVERE.getValue()) {
                eventSeverity = Severity.SEVERE;
            }
        }
        obs.setEventSeverity(eventSeverity);

        LOG.debug("Finished normalization of USGS earthquake {}", dataLake.getExternalId());
        return obs;
    }

    @SuppressWarnings("unchecked")
    private Double enrichPgaMask(Map<String, Object> shakemap, Map<String, Object> shaProps) {
        try {
            Object maxPgaObj = shaProps.get("maxpga");
            Double maxPga = null;
            if (maxPgaObj != null) {
                String pgaStr = maxPgaObj.toString();
                try {
                    maxPga = Double.valueOf(pgaStr);
                } catch (NumberFormatException ignored) {
                    LOG.debug("Cannot parse maxpga value '{}'", pgaStr);
                }
            }

            Object coverage = shakemap.get("coverage_pga_high_res");
            if (coverage instanceof Map) {
                //noinspection unchecked
                shaProps.put(COVERAGE_PGA_HIGHRES, (Map<String, Object>) coverage);

                if (maxPga != null && maxPga >= 0.4) {
                    String mask = shakemapDao.buildPgaMask(JsonUtil.writeJson(coverage));
                    if (mask != null) {
                        shaProps.put(PGA40_MASK, JsonUtil.readJson(mask, Map.class));
                    }
                }
            }
            return maxPga;
        } catch (Exception e) {
            LOG.warn("Failed to build PGA mask", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private FeatureCollection buildShakemapPolygons(Map<String, Object> shakemap) {
        Object contObj = shakemap.get("cont_mmi");
        if (!(contObj instanceof Map)) {
            LOG.debug("ShakeMap does not contain cont_mmi section");
            return null;
        }
        try {
            String fcJson = shakemapDao.buildShakemapPolygons(JsonUtil.writeJson(contObj));
            if (fcJson == null) {
                LOG.debug("buildShakemapPolygons returned null JSON");
                return null;
            }
            LOG.debug("ShakeMap polygons JSON size: {} bytes", fcJson.length());
            return JsonUtil.readJson(fcJson, FeatureCollection.class);
        } catch (Exception e) {
            LOG.warn("Failed to build ShakeMap polygons", e);
            return null;
        }
    }

    private Severity defineSeverityFromSig(Integer sig) {
        if (sig == null) return Severity.UNKNOWN;
        if (sig >= 900) return Severity.EXTREME;
        if (sig >= 600) return Severity.SEVERE;
        if (sig >= 450) return Severity.MODERATE;
        if (sig >= 300) return Severity.MINOR;
        return Severity.UNKNOWN;
    }

}
