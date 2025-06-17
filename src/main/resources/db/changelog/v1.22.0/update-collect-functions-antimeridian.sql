--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/update-collect-functions-antimeridian.sql runOnChange:true splitStatements:false

CREATE OR REPLACE FUNCTION collectGeomFromGeoJSON(jsonb) RETURNS geometry
AS $$
select ST_Collect(split_antimeridian(ST_GeomFromGeoJSON(feature->'geometry')))
from jsonb_array_elements($1->'features') feature;
$$ LANGUAGE SQL IMMUTABLE RETURNS NULL ON NULL INPUT;

CREATE OR REPLACE FUNCTION collectGeometryFromEpisodes(jsonb) RETURNS geometry
AS $$
select ST_Collect(split_antimeridian(ST_GeomFromGeoJSON(NULLIF(feature.geometries, 'null'::jsonb))))
from (select jsonb_array_elements(e -> 'geometries' -> 'features') -> 'geometry' as geometries
      from jsonb_array_elements($1) e) feature;
$$ LANGUAGE SQL STRICT IMMUTABLE PARALLEL SAFE;
