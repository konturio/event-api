--liquibase formatted sql

--changeset event-api-migrations:18-add-collected-geometry-to-normalized-observation runOnChange:false

CREATE FUNCTION collectGeomFromGeoJSON(jsonb) RETURNS geometry
    AS 'select ST_Collect( ST_GeomFromGeoJSON(feature->''geometry'')) from jsonb_array_elements($1->''features'') feature;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

ALTER TABLE normalized_observations ADD COLUMN collected_geometry geometry GENERATED ALWAYS AS (collectGeomFromGeoJSON(geometries)) STORED;

CREATE INDEX ON normalized_observations USING GIST (collected_geometry);