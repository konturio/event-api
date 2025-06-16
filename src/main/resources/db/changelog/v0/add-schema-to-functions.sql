--liquibase formatted sql

--changeset event-api-migrations:v0/add-schema-to-functions runOnChange:false

alter table normalized_observations drop column collected_geography;

drop function collectGeomFromGeoJSON;

create function collectGeomFromGeoJSON(jsonb) returns geometry
    as 'select public.ST_Collect(public.ST_GeomFromGeoJSON(feature->''geometry'')) from jsonb_array_elements($1->''features'') feature;'
    language sql
    immutable
    strict;

alter table normalized_observations add column collected_geography geography generated always as (collectGeomFromGeoJSON(geometries)::geography) stored;

create index on normalized_observations using gist (collected_geography);
