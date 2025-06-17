# Event API Endpoints

This document describes all available REST endpoints exposed by the application. All paths are prefixed with `/v1`.

## `GET /v1/`
Search for events within a feed.

**Parameters**
- `feed` – feed name (required).
- `types` – list of event types.
- `severities` – list of severity values.
- `after` – return events enriched after this timestamp.
- `datetime` – interval filter. Accepts single RFC3339 timestamp or open/closed interval.
- `bbox` – bounding box defined as `minLon,minLat,maxLon,maxLat`.
- `limit` – page size (default `20`).
- `sortOrder` – `ASC` or `DESC` by `enrichedAt`.
- `episodeFilterType` – `ANY`, `LATEST` or `NONE`.

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

## `GET /v1/user_feeds`
Return the list of feeds available for the authenticated user. The list is built from the roles present in the JWT token.
