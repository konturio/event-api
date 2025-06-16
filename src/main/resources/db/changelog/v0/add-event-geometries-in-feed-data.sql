--liquibase formatted sql

--changeset event-api-migrations:v0/add-event-geometries-in-feed-data.sql runOnChange:true splitStatements:false

create or replace function collectEventGeometries(jsonb) returns jsonb
as $$
    select jsonb_build_object('type', 'FeatureCollection', 'features',
           json_agg(ST_AsGeoJSON(t.*)::json))
    from (
        select
            f.feature -> 'properties' -> 'areaType' as areaType,
            ST_Union(ST_MakeValid(ST_GeomFromGeoJSON(nullif(feature -> 'geometry', 'null'::jsonb)))) as geom
        from (
            select jsonb_array_elements(e -> 'geometries' -> 'features') as feature
            from jsonb_array_elements($1) e
        ) f
        group by areaType
    ) t(areaType, geom)
    where geom is not null;
    $$
    language sql
    strict
    immutable parallel safe;

alter table feed_data drop column IF EXISTS geometries;

alter table feed_data add column if not exists geometries jsonb;

