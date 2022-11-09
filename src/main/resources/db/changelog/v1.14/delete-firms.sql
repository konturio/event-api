--liquibase formatted sql

--changeset event-api-migrations:v1.14/delete-firms.sql runOnChange:true

with firms_event_ids as (
    select distinct event_id
    from kontur_events
    where provider in ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2')
)
delete from feed_data fd
using firms_event_ids fei
where fd.event_id = fei.event_id;

with firms_event_ids as (
    select distinct event_id
    from kontur_events
    where provider in ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2')
)
delete from feed_event_status fes
using firms_event_ids fei
where fes.event_id = fei.event_id;

delete from kontur_events
where provider in ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2');

delete from normalized_observations
where provider in ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2');

update data_lake
set normalized = false
where provider in ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2');

update feeds
set providers = '{"gdacsAlert", "gdacsAlertGeometry", "kontur.events"}',
    enrichment_postprocessors = '{}'
where alias = 'kontur-public';