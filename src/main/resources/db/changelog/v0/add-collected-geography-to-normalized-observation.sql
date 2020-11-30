--liquibase formatted sql

--changeset event-api-migrations:v0/add-collected-geography-to-normalized-observation runOnChange:false
DELETE FROM feed_data; -- because of changes in episode composition job
DELETE FROM kontur_events WHERE provider in ('firms.modis-c6','firms.suomi-npp-viirs-c2','firms.noaa-20-viirs-c2'); -- because of changes for firms event composition

ALTER TABLE normalized_observations DROP COLUMN collected_geometry;
ALTER TABLE normalized_observations ADD COLUMN collected_geography geography GENERATED ALWAYS AS (collectGeomFromGeoJSON(geometries)::geography) STORED;
CREATE INDEX ON normalized_observations USING GIST (collected_geography);