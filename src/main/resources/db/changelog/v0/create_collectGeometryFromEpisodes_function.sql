--liquibase formatted sql

--changeset event-api-migrations:v0/create_collectGeometryFromEpisodes_function.sql runOnChange:true splitStatements:false

create or replace function collectGeometryFromEpisodes(jsonb) returns geometry
as $$
select ST_Collect(ST_GeomFromGeoJSON(nullif(feature.geometries, 'null'::jsonb)))
from (select jsonb_array_elements(e -> 'geometries' -> 'features') -> 'geometry' as geometries
      from jsonb_array_elements($1) e) feature;
$$
    language sql
    strict
    immutable parallel safe;
