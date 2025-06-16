--liquibase formatted sql

--changeset event-api-migrations:18-add-collected-geometry-to-normalized-observation runOnChange:false

create function collectGeomFromGeoJSON(jsonb) returns geometry
    as 'select ST_Collect(ST_GeomFromGeoJSON(feature->''geometry'')) from jsonb_array_elements($1->''features'') feature;'
    language sql
    immutable
    strict;

alter table normalized_observations add column collected_geometry geometry generated always as (collectGeomFromGeoJSON(geometries)) stored;

create index on normalized_observations using gist (collected_geometry);
