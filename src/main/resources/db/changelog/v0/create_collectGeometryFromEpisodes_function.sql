--liquibase formatted sql

--changeset event-api-migrations:v0/create_collectGeometryFromEpisodes_function.sql runOnChange:true splitStatements:false

CREATE OR REPLACE FUNCTION collectGeometryFromEpisodes(jsonb) RETURNS geometry
AS $$
select ST_Collect(ST_GeomFromGeoJSON(NULLIF(feature.geometries, 'null'::jsonb)))
from (select jsonb_array_elements(e -> 'geometries' -> 'features') -> 'geometry' as geometries
      from jsonb_array_elements($1) e) feature;
$$
    LANGUAGE SQL
    STRICT
    IMMUTABLE PARALLEL SAFE;
