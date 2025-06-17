--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/create-split-antimeridian-function.sql runOnChange:true splitStatements:false

CREATE OR REPLACE FUNCTION split_antimeridian(geom geometry)
RETURNS geometry
LANGUAGE plpgsql
IMMUTABLE
STRICT AS $$
DECLARE
    shifted geometry := ST_ShiftLongitude(geom);
    line geometry := ST_SetSRID('LINESTRING(180 -90,180 90)'::geometry,4326);
BEGIN
    IF ST_MaxX(shifted) - ST_MinX(shifted) > 180 THEN
        RETURN ST_CollectionExtract(ST_Split(shifted, line), 3);
    END IF;
    RETURN shifted;
END;
$$;
