--liquibase formatted sql

--changeset event-api-migrations:v1.14.4/update-schema-for-collectcyclonegeometry-function.sql runOnChange:true

create or replace function collectcyclonegeometries(jsonb) returns jsonb
    volatile
    strict
    parallel safe
    language sql
as
'
    with features as (
        select
            f.feature -> ''properties'' as props,
            public.st_makevalid(public.st_geomfromgeojson(NULLIF(f.feature -> ''geometry'', ''null''::jsonb))) as geom
        from (
            select jsonb_array_elements(e -> ''geometries'' -> ''features'') as feature
            from jsonb_array_elements($1) e
            order by ((jsonb_array_elements(e -> ''geometries'' -> ''features'') -> ''properties'') ->> ''timestamp'')::timestamptz desc
        ) f
        where f.feature -> ''geometry'' != ''null''::jsonb and f.feature -> ''properties'' ->> ''areaType'' = ''position''
    ),
    positions as (
        select f.props as props, f.geom as geom
        from features f
        where f.geom is not null
    ),
    trackline as (
        select jsonb_build_object(''areaType'', ''track'', ''windSpeedKph'', (p.props ->> ''windSpeedKph'')::numeric, ''isObserved'',
            ((LEAD(p.props) OVER(ORDER BY (p.props ->> ''timestamp''))) ->> ''isObserved'')::boolean),
               public.ST_MakeLine(p.geom, LEAD(p.geom) OVER(ORDER BY (p.props ->> ''timestamp''))) AS geom
        from positions p
    ),
    alerts34 as (
        select jsonb_build_object(''areaType'', ''alertArea'', ''windSpeedKph'', 63) as props, public.ST_Union(y.geom) as geom
        from
            (
                select
                    public.ST_MakeValid(public.ST_Segmentize(
                        public.ST_ConvexHull(
                            public.ST_Collect(
                                public.ST_Buffer(geom::geography, public.ST_M(geom))::geometry,
                                public.ST_Buffer(lag(geom) over (order by path)::geography, lag(public.ST_M(geom)) over (order by path))::geometry
                                )
                            )::geography,
                        10000
                        )::geometry) as geom
                from
                    (
                        select
                            (public.ST_DumpPoints(public.ST_Segmentize(
                                public.ST_MakeLine(
                                        public.ST_SetSRID(public.ST_MakePointM(public.ST_X(p.geom), public.ST_Y(p.geom),
                                             greatest(coalesce(p.props ->> ''34_kt_NE'', ''0'')::float,
                                                      coalesce(p.props ->> ''34_kt_NW'', ''0'')::float,
                                                      coalesce(p.props ->> ''34_kt_SE'', ''0'')::float,
                                                      coalesce(p.props ->> ''34_kt_SW'', ''0'')::float,
                                                      0::float)::float * 1000),
                                        public.ST_SRID(geom))
                                )::geography,
                            1000)::geometry)).*
                        from
                            positions p
                    ) z
            ) y
    ),
    alerts50 as (
        select jsonb_build_object(''areaType'', ''alertArea'', ''windSpeedKph'', 93) as props, public.ST_Union(y.geom) as geom
        from
            (
                select
                    public.ST_MakeValid(public.ST_Segmentize(
                        public.ST_ConvexHull(
                            public.ST_Collect(
                                public.ST_Buffer(geom::geography, public.ST_M(geom))::geometry,
                                public.ST_Buffer(lag(geom) over (order by path)::geography, lag(public.ST_M(geom)) over (order by path))::geometry
                                )
                            )::geography,
                        10000
                        )::geometry) as geom
                from
                    (
                        select
                            (public.ST_DumpPoints(public.ST_Segmentize(
                                public.ST_MakeLine(
                                        public.ST_SetSRID(public.ST_MakePointM(public.ST_X(geom), public.ST_Y(geom),
                                             greatest(coalesce(p.props ->> ''50_kt_NE'', ''0'')::float,
                                                      coalesce(p.props ->> ''50_kt_NW'', ''0'')::float,
                                                      coalesce(p.props ->> ''50_kt_SE'', ''0'')::float,
                                                      coalesce(p.props ->> ''50_kt_SW'', ''0'')::float,
                                                      0::float)::float * 1000),
                                        public.ST_SRID(geom))
                                )::geography,
                            1000)::geometry)).*
                        from
                            positions p
                    ) z
            ) y
    ),
    alerts64 as (
        select jsonb_build_object(''areaType'', ''alertArea'', ''windSpeedKph'', 119) as props, public.ST_Union(y.geom) as geom
        from
            (
                select
                    public.ST_MakeValid(public.ST_Segmentize(
                        public.ST_ConvexHull(
                            public.ST_Collect(
                                public.ST_Buffer(geom::geography, public.ST_M(geom))::geometry,
                                public.ST_Buffer(lag(geom) over (order by path)::geography, lag(public.ST_M(geom)) over (order by path))::geometry
                                )
                            )::geography,
                        10000
                        )::geometry) as geom
                from
                    (
                        select
                            (public.ST_DumpPoints(public.ST_Segmentize(
                                public.ST_MakeLine(
                                    public.ST_SetSRID(public.ST_MakePointM(public.ST_X(geom), public.ST_Y(geom),
                                        greatest(coalesce(p.props ->> ''64_kt_NE'', ''0'')::float,
                                                 coalesce(p.props ->> ''64_kt_NW'', ''0'')::float,
                                                 coalesce(p.props ->> ''64_kt_SE'', ''0'')::float,
                                                 coalesce(p.props ->> ''64_kt_SW'', ''0'')::float,
                                                 0::float)::float * 1000),
                                    public.ST_SRID(geom))
                                )::geography,
                            1000)::geometry)).*
                        from
                            positions p
                    ) z
            ) y
    )
    select jsonb_build_object(
               ''type'', ''FeatureCollection'',
               ''features'', json_agg(json_build_object(
                ''type'', ''Feature'',
                ''geometry'', public.ST_AsGeoJSON(geom)::jsonb,
                ''properties'', props)))
    from (
         select * from positions
         union select * from alerts34
         union select * from alerts50
         union select * from alerts64
         union select * from trackline
         ) f(props, geom)
    where geom is not null
';
