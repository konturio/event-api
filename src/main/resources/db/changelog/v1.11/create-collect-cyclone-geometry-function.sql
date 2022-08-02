--liquibase formatted sql

--changeset event-api-migrations:v1.11/create-collect-cyclone-geometry-function.sql runOnChange:true

drop function if exists collectcyclonegeometries;

create function collectcyclonegeometries(jsonb) returns jsonb
    volatile
    strict
    parallel safe
    language sql
as
$$
    with features as (
        select
            f.feature -> 'properties' as props,
            st_makevalid(st_geomfromgeojson(NULLIF(f.feature -> 'geometry', 'null'::jsonb))) as geom
        from (
            select jsonb_array_elements(e -> 'geometries' -> 'features') as feature
            from jsonb_array_elements($1) e
            order by ((jsonb_array_elements(e -> 'geometries' -> 'features') -> 'properties') ->> 'timestamp')::timestamptz desc
        ) f
        where f.feature -> 'geometry' != 'null'::jsonb and f.feature -> 'properties' ->> 'areaType' = 'position'
    ),
    positions as (
        select f.props as props, f.geom as geom
        from features f
        where f.geom is not null
    ),
    trackline as (
        select jsonb_build_object('area_type', 'track', 'wind_speed_kmph', p.props ->> 'windSpeedKph', 'isObserved',
            (LEAD(p.props) OVER(ORDER BY (p.props ->> 'timestamp'))) ->> 'isObserved'),
               ST_MakeLine(p.geom, LEAD(p.geom) OVER(ORDER BY (p.props ->> 'timestamp'))) AS geom
        from positions p
    ),
    alerts34 as (
        select jsonb_build_object('area_type', 'alertArea', 'wind_speed_kmph', '62') as props, ST_Union(y.geom) as geom
        from
            (
                select
                    ST_Segmentize(
                        ST_ConvexHull(
                            ST_Collect(
                                ST_Buffer(geom::geography, ST_M(geom))::geometry,
                                ST_Buffer(lag(geom) over (order by path)::geography, lag(ST_M(geom)) over (order by path))::geometry
                                )
                            )::geography,
                        10000
                        )::geometry as geom
                from
                    (
                        select
                            (ST_DumpPoints(ST_Segmentize(ST_MakeLine(ST_Force3DM(p.geom,
                                greatest((p.props ->> '34_kt_NE')::float, (p.props ->> '34_kt_NW')::float,
                                        (p.props ->> '34_kt_SE')::float, (p.props ->> '34_kt_SW')::float)::float))::geography,
                                1000)::geometry)).*
                        from
                            positions p
                    ) z
            ) y
    ),
    alerts50 as (
        select jsonb_build_object('area_type', 'alertArea', 'wind_speed_kmph', '92') as props, ST_Union(y.geom) as geom
        from
            (
                select
                    ST_Segmentize(
                        ST_ConvexHull(
                            ST_Collect(
                                ST_Buffer(geom::geography, ST_M(geom))::geometry,
                                ST_Buffer(lag(geom) over (order by path)::geography, lag(ST_M(geom)) over (order by path))::geometry
                                )
                            )::geography,
                        10000
                        )::geometry as geom
                from
                    (
                        select
                            (ST_DumpPoints(ST_Segmentize(ST_MakeLine(ST_Force3DM(p.geom,
                                greatest((p.props ->> '50_kt_NE')::float, (p.props ->> '50_kt_NW')::float,
                                        (p.props ->> '50_kt_SE')::float, (p.props ->> '50_kt_SW')::float)::float))::geography,
                                1000)::geometry)).*
                        from
                            positions p
                    ) z
            ) y
    ),
    alerts64 as (
        select jsonb_build_object('area_type', 'alertArea', 'wind_speed_kmph', '118') as props, ST_Union(y.geom) as geom
        from
            (
                select
                    ST_Segmentize(
                        ST_ConvexHull(
                            ST_Collect(
                                ST_Buffer(geom::geography, ST_M(geom))::geometry,
                                ST_Buffer(lag(geom) over (order by path)::geography, lag(ST_M(geom)) over (order by path))::geometry
                                )
                            )::geography,
                        10000
                        )::geometry as geom
                from
                    (
                        select
                            (ST_DumpPoints(ST_Segmentize(ST_MakeLine(ST_Force3DM(p.geom,
                                greatest((p.props ->> '64_kt_NE')::float, (p.props ->> '64_kt_NW')::float,
                                        (p.props ->> '64_kt_SE')::float, (p.props ->> '64_kt_SW')::float)::float))::geography,
                                1000)::geometry)).*
                        from
                            positions p
                    ) z
            ) y
    )
    select jsonb_build_object(
               'type', 'FeatureCollection',
               'features', json_agg(json_build_object(
                'type', 'Feature',
                'geometry', ST_AsGeoJSON(geom)::jsonb,
                'properties', props)))
    from (
         select * from positions
         union select * from alerts34
         union select * from alerts50
         union select * from alerts64
         union select * from trackline
         ) f(props, geom)
    where geom is not null
$$;
