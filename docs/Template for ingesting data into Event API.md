# Template for ingesting data into Event API

### **Need to determine before data ingestion:**
* source (sources) of information (be sure that commercial use of data is allowed (or we have paid for data));
* is it real-time (updated) or historical-only data:
  * if data is updated: 
    * the schedule of updating;
    * order of row processing;
    * how observations associated with event;
    * how new episodes are generated;
    * the way we should process updated rows (do we have id, probably we need to generate it…);
* the way of data normalization;
* fields which exist in the data source, which ones do we need (do we need just textual info if we don't have spatial), how does it match with our DB-structure;
* do we need textual or spatial info from other data sources;
* do we have overlapped data from different sources or only one source for one place:
  * if we have overlaps how should we process it;
* the way we understand if event actual or not; 

### Additional tips:
* there may be rules for generating fields with **name** and **description** (they are empty by default);
* there should be rules for determining [**the level of severity**](https://kontur.fibery.io/Tasks/document/Severities---Kontur-Event-API-589 "https://kontur.fibery.io/Tasks/document/Severities---Kontur-Event-API-589") of the event (not necessary, but critical for real-time data);
* define **requirements for API** (what kind of requests do our customers/users have and what kind of output data do they expect);
* determine [event's types](https://kontur.fibery.io/Tasks/document/Event-Types---Kontur-Event-API-588 "https://kontur.fibery.io/Tasks/document/Event-Types---Kontur-Event-API-588") (is we have feed with the same event type);
* determine if we need to alert from feed (rules and text);  
* what should we do if connection with external data source is lost;
* should we check coords (line is too long etc.).

### Table structure that we should describe:

### data_lake

General logic: 
* 

|     |     |     |
| --- | --- | --- |
| Event API DB (data_lake) | provider_name | Comment |
| observation_id uuid |  |  |
| external_id text |  |  |
| updated_at timestamp with time zone |  |  |
| loaded_at timestamp with time zone |  |  |
| provider text |  |  |
| data text |  |  |

### normalized_observations

|     |     |     |
| --- | --- | --- |
| Event API DB (normalized_observations) | provider_name-dat | Comment |
| observation_id uuid |  |  |
| external_event_id text |  |  |
| provider text |  |  |
| point geometry |  |  |
| geometries jsonb |  |  |
| event_severity text |  |  |
| "name" text |  |  |
| description text |  |  |
| episode_description text |  |  |
| "type" text |  |  |
| active bool |  |  |
| "cost" numeric |  |  |
| region text |  |  |
| loaded_at timestamptz |  |  |
| started_at timestamptz |  |  |
| ended_at timestamptz |  |  |
| source_updated_at timestamptz |  |  |
| source_uri text |  |  |
| external_episode_id text |  |  |
| collected_geography geography |  |  |
* how observations are associated with event:**
* 
* how new episodes are generated:**
* 
* how version is incremented for different feeds:**
* 

|     |     |
| --- | --- |
| Event API DB (feed_data) | Comment |
| event_id uuid |  |
| feed_id uuid |  |
| version bigint |  |
| name text |  |
| description text |  |
| started_at timestamp |  |
| ended_at timestampz |  |
| updated_at timestampz |  |
| episodes jsonb |  |
| observations uuid\[\]   |  |
| collected_geometry geometry |  |
|  is_latest_version boolean |  |
|  episode_types text\[\] |  |
* **episode data**:
  * name  - 
  * type - 
  * active - 
  * ended_at  - 
  * updated_at - 
  * started_at - 
  * severity - 
  * geometries - 
  * description - 
  * source_updated_at - 
