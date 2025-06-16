--liquibase formatted sql

--changeset event-api-migrations:v1.2/create-collect-event-geometry-function.sql runOnChange:true

drop function if exists collectEventGeometries;

create or replace function collectEventGeometries(jsonb) returns jsonb
as $$
    with features as (
        select
            f.feature -> 'properties' as props,
            ST_MakeValid(ST_GeomFromGeoJSON(nullif(f.feature -> 'geometry', 'null'::jsonb))) as geom
        from (
            select jsonb_array_elements(e -> 'geometries' -> 'features') as feature
            from jsonb_array_elements($1) e
            order by (e ->> 'startedAt')::timestamptz desc, (e ->> 'endedAt')::timestamptz desc
        ) f
        where f.feature -> 'geometry' != 'null'::jsonb
    ),
    areas as (
        select f.props as props, ST_SetSRID(ST_Union(f.geom), 4326) as geom
        from features f
        where f.props ->> 'areaType' in ('exposure', 'alertArea', 'globalArea')
        group by f.props
    ),
    startPoint as (
        select f.props as props, f.geom as geom
        from features f
        where f.props ->> 'areaType' = 'startPoint'
        limit 1
    ),
    other as (
        select f.props as props, f.geom as geom
        from features f
        where (f.props ->> 'areaType') is null or f.props ->> 'areaType' not in ('exposure', 'alertArea', 'globalArea', 'startPoint', 'centerPoint')
    ),
    centerPoint as (
        select jsonb_build_object('areaType', 'centerPoint') as props, (
            case
                when exists(select * from areas) or exists(select * from startPoint) or exists(select * from other) then (
                    select ST_Centroid(ST_Collect(f.geom)) as geom
                    from (
                        select * from areas
                        union select * from startPoint
                        union select * from other
                    ) f
                )
                else (
                    select f.geom as geom
                    from features f
                    where f.props ->> 'areaType' = 'centerPoint'
                    limit 1
                )
                end) as geom
    )
    select jsonb_build_object(
        'type', 'FeatureCollection',
        'features', json_agg(json_build_object(
            'type', 'Feature',
            'geometry', ST_AsGeoJSON(geom)::jsonb,
            'properties', props)))
    from (
        select * from areas
        union select * from startPoint
        union select * from other
        union select * from centerPoint
    ) f(props, geom)
    where geom is not null
$$
language sql
strict
immutable parallel safe;
