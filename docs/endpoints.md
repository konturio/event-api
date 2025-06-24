# Event API Endpoints

This document describes all available REST endpoints exposed by the application.

## `GET /`
Simple service check endpoint. Returns plain `OK` text.

All paths below are prefixed with `/v1`.

## `GET /v1/`
Search for events within a feed.

**Parameters**
- `feed` – feed name (required).
- `types` – list of event types.
- `severities` – list of severity values (`UNKNOWN`, `TERMINATION`, `MINOR`, `MODERATE`, `SEVERE`, `EXTREME`).
  When several values are provided, events matching any of them are returned.
- `after` – return events updated after this timestamp.
- `datetime` – interval filter. Accepts single RFC3339 timestamp or open/closed interval.
- `bbox` – bounding box defined as `minLon,minLat,maxLon,maxLat`. Each latitude must
  be between `-90` and `90`, longitude between `-180` and `180`, and minimum values
  should be less than maximum ones.
- `limit` – page size (default `20`).
- `sortOrder` – `ASC` or `DESC` by `updatedAt`.
- `episodeFilterType` – `ANY`, `LATEST` or `NONE`.
- `geometryFilterType` – `ANY` or `NONE`.

Returns events sorted by update date using cursor based pagination. Response body is JSON containing `pageMetadata.nextAfterValue` and event data.

## `GET /v1/geojson/events`
Same as the root `/v1/` endpoint but returns results as GeoJSON `FeatureCollection`.

Additional optional parameter `access_token` can be passed for geojson visualisation services.

## `GET /v1/observations/{observationId}`
Return raw observation data by its UUID. Content type can be JSON, XML, CSV, or another one depending on the source.

Requires `read:raw-data` permission.

## `GET /v1/event`
Return a single event by feed alias, event ID and optional version. When the version is omitted the latest one is returned.

**Parameters**
- `feed` – feed name (required).
- `eventId` – event UUID (required).
- `version` – event version number (optional).
- `episodeFilterType` – `ANY`, `LATEST` or `NONE`.
- `geometryFilterType` – `ANY` or `NONE`.

## `GET /v1/event/similar`
Find events similar to the specified event. Similarity is determined by event type and proximity of geometries.

**Parameters**
- `feed` – feed name (required).
- `eventId` – reference event UUID (required).
- `limit` – number of records to return. Default is `10`.
- `distance` – search radius in meters. Default is `50000`.

## `GET /v1/user_feeds`
Returns the list of feeds available for the authenticated user. The list is built from the roles present in the JWT token and is cached for one hour to improve response time.

## `GET /v1/merge_pair`
Retrieve merge candidate pair for manual review. When `pairID` is omitted the next available pair is returned.

**Parameters**
- `pairID` – array with two external event IDs.
