--liquibase formatted sql

--changeset event-api-migrations:v0/add-gdacs-firms-feed.sql runOnChange:true

update feeds set alias = 'disaster-ninja-02' where alias = 'gdacs-firms';

insert into feeds (feed_id, alias, description, providers, enrichment)
values (uuid_generate_v4(), 'disaster-ninja-02', 'GDACS+FIRMS',
        '{"firms.modis-c6","firms.suomi-npp-viirs-c2","firms.noaa-20-viirs-c2","gdacsAlert","gdacsAlertGeometry"}',
        '{"population", "peopleWithoutOsmBuildings", "areaWithoutOsmBuildingsKm2", "peopleWithoutOsmRoads", "areaWithoutOsmRoadsKm2", "peopleWithoutOsmObjects", "areaWithoutOsmObjectsKm2", "osmGapsPercentage", "industrialAreaKm2", "forestAreaKm2", "volcanoesCount", "hotspotDaysPerYearMax"}')
on conflict do nothing;

insert into feed_event_status (feed_id, event_id, actual)
select distinct on (event_id) fs.feed_id, event_id, false
from kontur_events,
    feeds fs
where provider in
      ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2', 'gdacsAlert', 'gdacsAlertGeometry')
  and alias = 'disaster-ninja-02'
on conflict do nothing;
