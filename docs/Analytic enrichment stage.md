# Analytic enrichment stage

The property `enrichmentExecutor.poolSize` defines how many threads process analytic enrichment concurrently.
Adjust it in configuration to tune resource usage.

### **Business requirements:**
* we want to add Kontur analytic data into feeds (to DN can use it and for selling);
* some **feeds** can be enriched and others - not (depends on who pays for analytic data or who needs it). so enrichment happens on the feed level;
* we can stop (?) enrich one feed for some reasons;
  * or we can recalculate all feed_data to add analytic;
* analytic enrichment can be on the level of the episode **and** on an event level;
* list of enrichment types:
  * population (divided into 'total' and 'humanitarian impact');
  * location - *can be usual enrichment type but can be used for name forming;*
  * geocoding;
  * *fire type - specific for firms data. we can limit this enrichment to only event_type HOTSPOT;*
  * loss cost;
  * rebuilt cost.
* when we add new enrichment to feed:
  * just add it to new events/episodes;
  * we enrich all already published feed_data or a part of it depends on the time of event (need to create some job witch will initiate the process).
* Some assumptions:**
* we need to minimize quantity of requests to external services (with population, boundaries)
* analytic enrichment stage process after feed combination
* we can reduce some fields from normalized_observations table (region/name);
* we can add firms observations as 'HOTSPOT' event type into normalized_observations and put into feed_data as classified WILDFIRE or other.

### DB changes:

|     |     |
| --- | --- |
| **Table** | **Changes** |
| feeds  | new field with info on what analytic need for each feed: **field:** enrichment **type:** text\[\] **data structure:**  \[    
    analytic1,    
    analytic2,    
    analytic3   
\] |
| feed_data  | new field with analytic data for an entire event: **field:** event_details **type:** jsonb **data structure:**  {
"analytic1": value,
"analytic2": value,
"analytic3": value
}  |
|  | new field with a flag if an event is enriched/processed along the enrichment stage: **field:** enrichment_passed **type:** boolean  |

### List of enrichment types/possible analytics

[[Tasks/document: List of enrichment types#^b2d59af0-3b70-11e9-be77-04d77e8d50cb/6b366e70-de63-11eb-b1d1-5dd7fb1980e6]] 

### API changes:
* we show for clients events only after the enrichment stage;
* we show additional objects with analytical data:
  * eventDetails - for the entire event;
  * episodeDetails - for the episode;

### Example of API response 

```
{
  "data": [
    {
      "eventId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "version": 0,
      "name": "string",
      "description": "string",
      "startedAt": "2021-06-02T10:25:21.728Z",
      "endedAt": "2021-06-02T10:25:21.728Z",
      "updatedAt": "2021-06-02T10:25:21.728Z",
      "eventDetails": {//same as for episodes, view episode analytic
            "population": "number",
            "humanitarianImpact": {}
            "type": "text", // ----for thermal anomaly only, maybe we need type only on the higher level
            "lossCostUSD": "number",        
            "noObjectsSettledAreaKm2": "number", 
            "noObjectsSettledAreaPeople": "number", 
            "noRoadsSettledAreaKm2": "number", 
            "noRoadsSettledAreaPeople": "number", 
            "noBuildingsSettledAreaKm2": "number", 
            "noBuildingsSettledAreaPeople": "number"
      },
      "observations": [
        "3fa85f64-5717-4562-b3fc-2c963f66afa6"
      ],
      "episodes": [
        {
          "name": "string",
          "description": "string",
          "type": "FLOOD",
          "active": true,
          "severity": "EXTREME",
          "startedAt": "2021-06-02T10:25:21.728Z",
          "endedAt": "2021-06-02T10:25:21.728Z",
          "updatedAt": "2021-06-02T10:25:21.728Z",
          "sourceUpdatedAt": "2021-06-02T10:25:21.728Z",

          "episodeDetails": {
            "population": "number",
            "humanitarianImpact": {}
            "type": "text", // ----for thermal anomaly only, maybe we need type only on the higher level
            "lossCostUSD": "number",        
            "noObjectsSettledAreaKm2": "number", 
            "noObjectsSettledAreaPeople": "number", 
            "noRoadsSettledAreaKm2": "number", 
            "noRoadsSettledAreaPeople": "number", 
            "noBuildingsSettledAreaKm2": "number", 
            "noBuildingsSettledAreaPeople": "number"
          },
          "observations": [
            "3fa85f64-5717-4562-b3fc-2c963f66afa6"
          ],
          "geometries": {
            "type": "string",
            "features": [
              {
                "type": "string",              
                  },
                "properties" {
                  "additionalProp1": {},
                  "additionalProp2": {},
                  "additionalProp3": {}
              }
            ]
          }
        }
      ]
    }
  ],
  "pageMetadata": {
    "nextAfterValue": "2021-06-02T10:25:21.728Z"
  }

```
