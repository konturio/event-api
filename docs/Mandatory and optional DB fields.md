# Mandatory and optional DB fields

Field: Content

There are 6 tables in Event API database:
* data_lake
* normalized_observations
* kontur_events
* feed_event_status
* feed_data
* feeds

They have the following fields: 
* ***mandatory***  - fields that we get from source data and that are required to be set
* ***optional*** - fields that we get from source data and that are not required to be set

Fields also can be:
* ***generated*** - columns computed by DB, can not be set during insert or update, their value is calculated automatically by DB  
* ***auto*** - fields that are calculated and set in code, that don't depend on source data (IDs, fields in feed_data, etc.)

### data_lake

|     |     |     |
| --- | --- | --- |
| **Name** | **Field classification** | **Where data came from?** |
| observation_id | mandatory auto | random UUID |
| external_id | mandatory | from source data / record hash / etc. |
| updated_at | optional | from source data / equals `data_lake.loaded_at` / etc.  |
| loaded_at | mandatory auto | NOW |
| provider | mandatory auto | known when job for specific provider runs |
| data | mandatory | from source data |
| normalized | mandatory auto | `false` while record didn't pass the normalization step, `true` after normalization |

### normalized_observations

|     |     |     |
| --- | --- | --- |
| **Name** | **Field classification** | **Where data came from?** |
| observation_id | mandatory auto | `data_lake.observation_id` |
| external_event_id | mandatory (if the flow for event combination is default - by IDs) /  optional (if the flow for event combination is custom and does not depend on IDs) | `data_lake.external_id `/ other value from source data / etc. |
| provider | mandatory auto | `data_lake.provider` |
| point | optional | from source data |
| geometries | optional | from source data |
| event_severity | optional  | from source data, usually `UNKNOWN` when no data present for severity |
| name | optional | from source data |
| description | optional | from source data |
| episode_description | optional | from source data |
| type | optional | from source data, usually `OTHER` when no data present for type |
| active | optional | from source data |
| cost | optional | from source data |
| region | optional | from source data |
| loaded_at | mandatory auto | `data_lake.loaded_at` |
| started_at | mandatory | from source data |
| ended_at | optional | from source data / equals `started_at` |
| source_updated_at | optional | from source data |
| source_uri | optional | from source data |
| external_episode_id | optional | from source data |
| collected_geography | mandatory generated | collected geometry (`geography` type) from feature collection `normalized_observations.geometries (jsonb)`  |
| recombined | mandatory auto | `false` while observation didn't pass the event combination step, `true` after event combination |

### kontur_events

|     |     |     |
| --- | --- | --- |
| **Name** | **Field classification** | **Where data came from?** |
| event_id | mandatory auto | random UUID |
| observation_id | mandatory auto | `normalized_observations.observation_id` |
| provider | mandatory auto | `normalized_observations.provider` |

### feed_event_status

|     |     |     |
| --- | --- | --- |
| **Name** | **Field classification** | **Where data came from?** |
| feed_id | mandatory auto | `feeds.feed_id` the ID of feed to add event to |
| event_id | mandatory auto | `kontur_events.event_id` the ID of event to add to feed |
| actual | mandatory auto | `false` while hasn't been added to feed yet, `true` when event has been added to feed |

### feed_data

|     |     |     |
| --- | --- | --- |
| **Name** | **Field classification** | **Where data came from?** |
| event_id | mandatory auto | `feed_event_status.event_id` |
| feed_id | mandatory auto | `feeds.feed_id` |
| version | mandatory auto | latest `feed_data` (same `feed_id` and `event_id`) version **+1**,  or **1** when there were no versions before |
| name | optional auto | latest not empty value of `episode.name` or null if there no filled names |
| description | optional auto | latest not empty value of `episode.description` or null if there no filled descriptions |
| started_at | mandatory auto | latest not empty value of `episode.started_at` or null if there no filled dates (in fact always latest value, cause this field must be always set to prevent started / ended date collisions) |
| ended_at | optional auto | latest not empty value of `episode.ended_at` or null if there no filled dates (in fact always latest value, cause this field must be always set to prevent started / ended date collisions) |
| updated_at | mandatory auto | latest not empty value of `episode.updated_at` or null if there no filled dates (in fact always latest value, cause this field is always set automatically) |
| observations | mandatory auto | list of observation that were combined during event combination to form the event |
| episodes | mandatory auto | list of event episodes generated during feed composition step |
| collected_geometry | mandatory generated | collected geometry from all event episodes (`geometry`) |
| episode_types | mandatory generated | list of distinct episode types |
| is_latest_version | mandatory auto | shows if particular event version is currently the latest |
| enriched | mandatory auto | whether event was fully enriched during enrichment stage |
| event_details | optional auto | event analytics retrieved during enrichment stage |
| geometries | optional auto | geometry of entire event (`jsonb`) |

### feeds

|     |     |     |
| --- | --- | --- |
| **Name** | **Field classification** | **Where data came from?** |
| feed_id | mandatory | random UUID |
| description | optional  | feed description |
| alias | mandatory | feed short name |
| providers | mandatory | list of event providers to use for the feed |
| roles | optional | ??? [[Alexander Zapasnik#@aeb4a10c-3b70-11e9-be77-04d77e8d50cb/2e5ddc70-3c0f-11e9-9428-04d77e8d50cb]] Why do we need this field?  [[Palina Krukovich#@aeb4a10c-3b70-11e9-be77-04d77e8d50cb/6d0fad88-676c-4f87-812f-f158ef6e389d]]  Rudiment. It wasn't used and can be removed.  |
| enrichment | mandatory | list of analytic fields for feed, default is empty array |
