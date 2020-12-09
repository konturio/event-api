--liquibase formatted sql

--changeset event-api-migrations:v0/add-schema-to-functions runOnChange:false

ALTER TABLE normalized_observations DROP COLUMN collected_geography;

DROP FUNCTION collectGeomFromGeoJSON;

CREATE FUNCTION collectGeomFromGeoJSON(jsonb) RETURNS geometry
    AS 'select public.ST_Collect( public.ST_GeomFromGeoJSON(feature->''geometry'')) from jsonb_array_elements($1->''features'') feature;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

ALTER TABLE normalized_observations ADD COLUMN collected_geography geography GENERATED ALWAYS AS (collectGeomFromGeoJSON(geometries)::geography) STORED;

CREATE INDEX ON normalized_observations USING GIST (collected_geography);
