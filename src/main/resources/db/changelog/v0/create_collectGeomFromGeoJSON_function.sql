--liquibase formatted sql

--changeset event-api-migrations:create_collectGeomFromGeoJSON_function.sql runOnChange:true

CREATE OR REPLACE FUNCTION collectGeomFromGeoJSON(jsonb) RETURNS geometry
AS 'select public.ST_Collect(public.ST_GeomFromGeoJSON(NULLIF(feature -> ''geometry'', ''null''::jsonb))) from jsonb_array_elements($1->''features'') feature;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;