# Database Schema

The application uses PostgreSQL with PostGIS and Liquibase for migrations. Below is an overview of the main tables after applying migrations up to version 1.21.

## `data_lake`
Stores raw provider payloads.

| Column | Type | Notes |
| ------ | ---- | ----- |
| `observation_id` | `uuid` | unique identifier |
| `external_id` | `text` | provider specific id |
| `updated_at` | `timestamptz` | timestamp from provider |
| `loaded_at` | `timestamptz` | when data was loaded |
| `provider` | `text` | name of the data provider |
| `data` | `text` | raw JSON or XML |

`external_id`, `provider`, `updated_at` are unique together.

## `normalized_observations`
Normalized information extracted from `data_lake`.

| Column | Type |
| ------ | ---- |
| `observation_id` | `uuid` (references `data_lake`)
| `external_id` | `text`
| `provider` | `text`
| `point` | `geometry`
| `geometries` | `jsonb`
| `event_severity` | `text`
| `name` | `text`
| `description` | `text`
| `episode_description` | `text`
| `type` | `text`
| `active` | `boolean`
| `cost` | `numeric`
| `region` | `text`
| `loaded_at` | `timestamptz`
| `started_at` | `timestamptz`
| `ended_at` | `timestamptz`
| `updated_at` | `timestamptz`
| `source_uri` | `text`
| `external_episode_id` | `text`
| `collected_geography` | `geography` (generated from `geometries`)

Indexes exist for `external_id` and `collected_geography`.

## `feeds`
List of available feeds.

| Column | Type | Notes |
| ------ | ---- | ----- |
| `feed_id` | `uuid` | primary key |
| `description` | `text` | |
| `alias` | `text` | unique identifier used in API |
| `providers` | `text[]` | source providers associated with the feed |
| `roles` | `text[]` | roles allowed to read the feed |

## `feed_data`
Stores event versions for each feed. Table was redesigned in version 1.15.

| Column | Type | Notes |
| ------ | ---- | ----- |
| `event_id` | `uuid` |
| `feed_id` | `uuid` references `feeds` |
| `version` | `smallint` |
| `enrichment_attempts` | `smallint` default `0` |
| `severity_id` | `smallint` references `severities` |
| `is_latest_version` | `boolean` default `true` |
| `enriched` | `boolean` default `false` |
| `updated_at` | `timestamptz` |
| `started_at` | `timestamptz` |
| `ended_at` | `timestamptz` |
| `composed_at` | `timestamptz` default `now()` |
| `enriched_at` | `timestamptz` |
| `enrichment_skipped` | `boolean` default `false` |
| `type` | `text` |
| `name` | `text` |
| `description` | `text` |
| `episodes` | `jsonb` |
| `observations` | `uuid[]` |
| `event_details` | `jsonb` default `{}` |
| `geometries` | `jsonb` |
| `urls` | `text[]` |
| `proper_name` | `text` |
| `location` | `text` |
| `collected_geometry` | `geometry` generated from episodes |

Unique key: (`event_id`, `version`, `feed_id`). Several GIST and BTREE indexes exist for geometry and timestamps. An additional
index `feed_data_event_feed_latest_idx` on `(event_id, feed_id)` with condition `is_latest_version` speeds up retrieval of the
latest event by its ID.

## `severities`
Reference table of possible severity levels.

| Column | Type |
| ------ | ---- |
| `severity_id` | `smallserial` primary key |
| `severity` | `text` |

## `feed_event_status`
Tracks current events per feed.

| Column | Type |
| ------ | ---- |
| `feed_id` | `uuid` references `feeds` |
| `event_id` | `uuid` |
| `actual` | `boolean` |

Unique on (`feed_id`, `event_id`).
