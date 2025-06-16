--liquibase formatted sql

--changeset event-api-migrations:v1.14.4/update-schema-for-collectgeometryfromepisodes-function.sql runOnChange:true

create or replace function collectGeometryFromEpisodes(jsonb) returns geometry
    language sql
    strict
    immutable parallel safe
as '
select public.ST_Collect(public.ST_GeomFromGeoJSON(nullif(feature.geometries, ''null''::jsonb)))
from (select jsonb_array_elements(e -> ''geometries'' -> ''features'') -> ''geometry'' as geometries
      from jsonb_array_elements($1) e) feature;
';
