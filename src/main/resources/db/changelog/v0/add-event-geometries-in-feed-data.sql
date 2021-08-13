--liquibase formatted sql

--changeset event-api-migrations:v0/add-event-geometries-in-feed-data.sql runOnChange:true splitStatements:false

CREATE OR REPLACE FUNCTION collectEventGeometries(jsonb) RETURNS JSONB
AS $$
    select jsonb_build_object('type', 'FeatureCollection', 'features',
           json_agg(ST_AsGeoJSON(t.*)::json))
    from (
        select
            f.feature -> 'properties' -> 'areaType' as areaType,
            st_union(st_makevalid(st_geomfromgeojson(NULLIF(feature -> 'geometry', 'null'::jsonb)))) as geom
        from (
            select jsonb_array_elements(e -> 'geometries' -> 'features') as feature
            from jsonb_array_elements($1) e
        ) f
        group by areaType
    ) t(areaType, geom)
    where geom is not null;
    $$
    LANGUAGE SQL
    STRICT
    IMMUTABLE PARALLEL SAFE;

ALTER TABLE feed_data DROP COLUMN IF EXISTS geometries;

ALTER TABLE feed_data ADD COLUMN IF NOT EXISTS geometries JSONB GENERATED ALWAYS AS (collectEventGeometries(episodes)) STORED;

