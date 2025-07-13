--liquibase formatted sql

--changeset event-api-migrations:v1.23.0/create-shakemap-polygons-function.sql runOnChange:true

drop function if exists buildShakemapPolygons;

create function buildShakemapPolygons(jsonb) returns jsonb
    language sql
    volatile
    strict
    parallel safe
as $$
    with cnt as (
        select $1 -> 'bbox' as bbox,
               $1 -> 'features' as features
    ),
    bbox as (
        select (ST_Dump(
            ST_Boundary(
                ST_MakeEnvelope(
                    (bbox ->> 0)::double precision,
                    (bbox ->> 1)::double precision,
                    (bbox ->> 2)::double precision,
                    (bbox ->> 3)::double precision,
                    4326
                )
            )
        )).geom as geom
        from cnt
    ),
    lines as (
        select jsonb_array_elements(features) -> 'properties' as properties,
               (ST_Dump(
                   ST_GeomFromGeoJSON(jsonb_array_elements(features) -> 'geometry')
               )).geom as geom
        from cnt
    ),
    splitted_bbox as (
        select (ST_Dump(
                ST_Split(b.geom, ST_Collect(l.geom))
               )).geom as geom
        from bbox b
        join lines l on true
        group by b.geom
    ),
    merged as (
        select ST_Collect(geom) as geom
        from (
            select geom from splitted_bbox
            union all
            select geom from lines
        ) as all_geom
    ),
    polys as (
        select (ST_Dump(ST_Polygonize(geom))).geom::geometry(Polygon,4326) as geom
        from merged
    ),
    poly_attr as (
        select row_number() over () as id,
               p.geom as geom,
               (l.props ->> 'value')::float as pga,
               l.props as props
        from polys p
        cross join lateral (
            select properties as props
            from lines
            order by p.geom <-> geom
            limit 1
        ) l
    ),
    vals as (
        select pga,
               lag(pga) over(order by pga) as lower_pga
        from (
            select distinct pga
            from poly_attr
            order by pga
        ) v
    ),
    ext as (
        select min(pga) as min_pga,
               max(pga) as max_pga
        from poly_attr
    ),
    neighbors_equal as (
        select a.id,
               bool_and(coalesce(b.pga, a.pga) = a.pga) as all_equal
        from poly_attr a
        left join poly_attr b on a.id <> b.id and ST_Touches(a.geom, b.geom)
        group by a.id
    ),
    step1 as (
        select a.id,
               a.geom,
               case
                   when ne.all_equal and a.pga = (select max_pga from ext) then a.pga
                   when ne.all_equal and a.pga = (select min_pga from ext) then null
                   when ne.all_equal then v.lower_pga
                   else a.pga
               end as pga,
               a.props
        from poly_attr a
        join neighbors_equal ne on ne.id = a.id
        left join vals v on v.pga = a.pga
    ),
    neighbors_null as (
        select s.id,
               bool_and(n.pga is null) as all_null
        from step1 s
        left join step1 n on s.id <> n.id and ST_Touches(s.geom, n.geom)
        group by s.id
    )
    select jsonb_build_object(
        'type','FeatureCollection',
        'features', jsonb_agg(
            jsonb_build_object(
                'type','Feature',
                'geometry', ST_AsGeoJSON(s.geom)::jsonb,
                'properties',
                    (s.props - 'Class' - 'country' - 'areaType') ||
                    jsonb_build_object('value',
                        case
                            when s.pga is null and nn.all_null then (select min_pga from ext)
                            else s.pga
                        end)
            )
        )
    )
    from step1 s
    left join neighbors_null nn on nn.id = s.id;
$$;
