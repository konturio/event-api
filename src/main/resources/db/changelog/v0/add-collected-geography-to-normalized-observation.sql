--liquibase formatted sql

--changeset event-api-migrations:v0/add-collected-geography-to-normalized-observation runOnChange:false
delete from feed_data; -- because of changes in episode composition job
delete from kontur_events where provider in ('firms.modis-c6','firms.suomi-npp-viirs-c2','firms.noaa-20-viirs-c2'); -- because of changes for firms event composition

alter table normalized_observations drop column collected_geometry;
alter table normalized_observations add column collected_geography geography generated always as (collectGeomFromGeoJSON(geometries)::geography) stored;
create index on normalized_observations using gist (collected_geography);
