--liquibase formatted sql

--changeset event-api-migrations:create_collectGeomFromGeoJSON_function.sql runOnChange:true

create or replace function collectGeomFromGeoJSON(jsonb) returns geometry
as 'select public.ST_Collect(public.ST_GeomFromGeoJSON(nullif(feature -> ''geometry'', ''null''::jsonb))) from jsonb_array_elements($1->''features'') feature;'
    language sql
    immutable
    strict;
