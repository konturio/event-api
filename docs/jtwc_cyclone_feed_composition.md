# JTWC Cyclone Feed Composition

This document describes how cyclone episodes are generated from Joint Typhoon Warning Center (JTWC) observations and how the resulting feed data is composed in Event API.

## Episode Generation
- A single JTWC observation generates one **current** episode using the point geometry marked `is_observed = true`.
- Forecast points of the same observation (where `is_observed = false`) are converted into separate episodes.
- When several observations exist, forecast points are used **only** for the latest observation (by `source_updated_at`).

## Feed Data Fields
| Field | Description |
|-------|-------------|
|`event_id`|Event identifier.|
|`feed_id`|Feed identifier.|
|`version`|Incremental version of the feed data.|
|`name`|Name of the current episode.|
|`description`|Description of the current episode.|
|`started_at`|Start of the earliest episode.|
|`ended_at`|End of the last episode.|
|`updated_at`|Latest `normalized_observations.loaded_at`.|
|`episodes`|List of episodes for every event version ordered by `updated_at`.|
|`observations`|All observation identifiers used in this version.|
|`collected_geometry`|Union geometry from all episodes.|
|`geometries`|Union of all point geometries (area_type `position`) and derived alert areas and track lines.|

## Episode Data
- `name` – taken from `normalized_observations.name`.
- `type` – taken from `normalized_observations.type`.
- `active` – taken from `normalized_observations.active`.
- `started_at` – timestamp from the point geometry.
- `ended_at` – timestamp of the next geometry; if missing, equals `started_at`.
- `updated_at` – `normalized_observations.loaded_at`.
- `severity` – determined by `wind_speed_kmph / 1.852`:
  - `<= 33` → MINOR
  - `<= 63` → MODERATE
  - `<= 82` → SEVERE
  - otherwise EXTREME
- `geometries` include:
  - the original point geometry with all properties;
  - polygons created from wind radii (`64_kt_*`, `50_kt_*`, `34_kt_*`). Each polygon has properties:
    - `area_type = alert`
    - `is_observed` inherited from the point
    - `forecast_hrs` inherited from the point when present
    - `timestamp` inherited from the point
    - `wind_speed_kmph` set to 120 for 64 kt, 90 for 50 kt, or 60 otherwise
- `description` – from `normalized_observations.description`.
- `source_updated_at` – from `normalized_observations.source_updated_at`.

## Geometry Collection
- All point geometries are joined to build track lines (`area_type = track`).
- Lines connecting past episodes contain `is_observed = true`; lines connecting forecast points are marked `is_observed = false`.
- Alert areas are generated from point geometries using the methodology described in `How to build a cyclone?`.

