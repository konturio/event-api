# Data Flow - Event API

Field: Content

Event API Overview

Prior work:

1. Event Proxy @ [[Tasks/project: Disaster Ninja#^131cd711-3bb4-11e9-9428-04d77e8d50cb/e23db600-3e7d-11e9-83ef-692ac8203621]] ,
2. Hazard API [[---#^f46d0c90-32dd-11ea-af1a-7b786b6201b4/d0fe35b0-8f83-11ea-b794-07342ede6979]] 

Diagram: <https://kontur.fibery.io/Tasks/project/Event-API-288/Event-API-Data-Flow-HLD-850>

## **Data Lake**

На первом этапе мы собираем сырые данные из hp-srv и sqs очереди и складываем их в отдельную таблицу (data_lake) не проводя анализа. Присваиваем данным observation_id (является уникальным для каждой конкретного нового обновления, который к нам поступает.

<https://testemops.pdc.org/hp_srv/#/hazards/searchHazard>

```
HpSrv/services/hazards/1/json/search_hazard :

{
"app_ID": 0, 
"app_IDs": "",
"autoexpire": "Y",
"category_ID": "EVENT",
"charter_Uri": "",
"comment_Text": "No Comment",
"create_Date": "893721600000",
"creator": "DisasterAWARE (P)",
"end_Date": "894153600000",
"glide_Uri": "",
"hazard_ID": 54, // numeric identifier of Hazard
"hazard_Name": "Wildfire (South Kohala/North Kona)-April 28, 1998",
"last_Update": "1559752440011",
"latitude": 19.50000002,
"longitude": -156.39999392,
"master_Incident_ID": "N/A",
"message_ID": "",
"org_ID": -1,
"severity_ID": "ADVISORY",
"snc_url": null,
"start_Date": "893721600000",
"status": "E",
"type_ID": "WILDFIRE",
"update_Date": "915753600000", // date in unix timestamp format
"update_User": "PDC003",
"product_total": "2",
"uuid": "df721b18-161b-40cf-a7cf-f05c3fb72178",
"in_Dashboard": "",
"areabrief_url": null,
"description": "Description unavailable",
"roles": []
}


```

Historical hazards are taken from hpsrv api -[ https://testemops.pdc.org/hp_srv/#/](https://testemops.pdc.org/hp_srv/#/)

*External_id - это идентификатор в том API где мы его получили.* 

*Data - здесь находятся данные, которые мы собрали.*

Hazards itself are taken from <https://testemops.pdc.org/hp_srv/#/hazards/searchHazard> 

Hazards episodes with geometry are taken from <https://testemops.pdc.org/hp_srv/#/mags/getMag_JSON> 

## **Normalization**

At the second stage, the stage of normalization, we bring all the data that we have collected to a single format and structure. Each observation from data_lake is normalized and stored in normalized_observtions table. Data to be normalized taken from data_lake.data. Set of fields stated in recombination flow. External_event_id - id of the event in the system in which we receive data.

Point - 

Event Severity - The degree of destruction, which is determined by the data provider, but we bring it to our own form. [[Tasks/document: Severities - Kontur Event API#^b2d59af0-3b70-11e9-be77-04d77e8d50cb/923d2200-f1c6-11ea-b41c-f70bccf9516d]] 

Type - [[Tasks/document: Event Types - Kontur Event API#^b2d59af0-3b70-11e9-be77-04d77e8d50cb/91dd0c40-f0f8-11ea-b41c-f70bccf9516d]] 

## **Event Recombination**

[https://kontur.fibery.io/Tasks/project/Event-API-288/old-Event-reconbination-flow-874](https://kontur.fibery.io/Tasks/project/Event-API-288/old-Event-reconbination-flow-874)

Чтобы понять где какие ивенты и объединить между собой

New kontur event generated with unique event_id. Each kontur event can be associated with several observations by some rules. E.g. by same normalized_observations.external_event_id or by close distance between geometries (for firms).

Зависимости: Для того, чтобы это функционировало должен 
* работать SQS [[Tasks/Task: Create SQS#^7b708802-3c0b-11e9-9428-04d77e8d50cb/87770b80-95d5-11ea-a29f-d53364ba3743]] 
* права доступа PDC

Потребители: 
* Swiss Re (получает все события от PDC), [[Tasks/project: Event API#^131cd711-3bb4-11e9-9428-04d77e8d50cb/94ba8c60-845d-11ea-ad1b-15d11c004f61]] 
* Unfolded (пожары) [[Tasks/User Story: FIRMS into Event API#^7a8452d0-8558-11ea-8035-51799a2fd608/a2ba8d50-f2c7-11ea-bb35-1520713e6cd3]] 

## Timeline:

FIRMS: [[Tasks/document: RFC1 - Versions and timelines#^b2d59af0-3b70-11e9-be77-04d77e8d50cb/bf5b3e50-233c-11eb-a837-bbc1b5a756e5]] 
