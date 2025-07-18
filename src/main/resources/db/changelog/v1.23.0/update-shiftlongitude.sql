--liquibase formatted sql

--changeset event-api-migrations:v1.26.0/update-shiftlongitude.sql runOnChange:true

drop function if exists buildShakemapPolygons;

drop function if exists buildPgaMask;

create function buildShakemapPolygons(jsonb) returns jsonb
    language sql
    volatile
    strict
    parallel safe
as $_$
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
               (l.props ->> 'value')::float as mmi,
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
        select mmi,
               lag(mmi) over(order by mmi) as lower_mmi
        from (
            select distinct mmi
            from poly_attr
            order by mmi
        ) v
    ),
    ext as (
        select min(mmi) as min_mmi,
               max(mmi) as max_mmi
        from poly_attr
    ),
    neighbors_equal as (
        select a.id,
               bool_and(coalesce(b.mmi, a.mmi) = a.mmi) as all_equal
        from poly_attr a
        left join poly_attr b on a.id <> b.id and ST_Touches(a.geom, b.geom)
        group by a.id
    ),
    step1 as (
        select a.id,
               a.geom,
               case
                   when ne.all_equal and a.mmi = (select max_mmi from ext) then a.mmi
                   when ne.all_equal and a.mmi = (select min_mmi from ext) then null
                   when ne.all_equal then v.lower_mmi
                   else a.mmi
               end as mmi,
               a.props
        from poly_attr a
        join neighbors_equal ne on ne.id = a.id
        left join vals v on v.mmi = a.mmi
    ),
    neighbors_null as (
        select s.id,
               bool_and(n.mmi is null) as all_null
        from step1 s
        left join step1 n on s.id <> n.id and ST_Touches(s.geom, n.geom)
        group by s.id
    ),
    final as (
        select s.geom,
               (s.props - 'Class' - 'country' - 'areaType') as props,
               case
                   when s.mmi is null and nn.all_null then (select min_mmi from ext)
                   else s.mmi
               end as value
        from step1 s
        left join neighbors_null nn on nn.id = s.id
    )
    select jsonb_build_object(
        'type','FeatureCollection',
        'features', jsonb_agg(
            jsonb_build_object(
                'type','Feature',
                'geometry', ST_AsGeoJSON(
                    CASE
                        WHEN ST_XMax(f.geom) > 180 OR ST_XMin(f.geom) < -180 THEN ST_ShiftLongitude(f.geom)
                        ELSE f.geom
                    END
                )::jsonb,
                'properties', f.props || jsonb_build_object('value', f.value)
            )
        )
    )
    from final f
    where f.value is not null;
$_$;

create function buildPgaMask(jsonb) returns geometry
    language sql
    volatile
    strict
    parallel safe
as $_$
    with meta as (
        select
            ($1 #>> '{domain,axes,x,start}')::float8  as x0,
            ($1 #>> '{domain,axes,x,stop}')::float8   as xN,
            ($1 #>> '{domain,axes,x,num}')::int       as nx,
            ($1 #>> '{domain,axes,y,start}')::float8  as y0,
            ($1 #>> '{domain,axes,y,stop}')::float8   as yN,
            ($1 #>> '{domain,axes,y,num}')::int       as ny,
            $1  #>  '{ranges,PGA,values}'            as vals
    ),
    vals as (
        select (v.value)::float8 as ln_pga,
               v.ordinality-1    as idx
        from meta,
             jsonb_array_elements(meta.vals) with ordinality as v(value, ordinality)
    ),
    filtered as (
        select idx from vals where ln_pga > ln(0.40)
    ),
    grid as (
        select
            idx / m.nx as j,
            idx % m.nx as i,
            m.,
            (m.xN - m.x0)::float8 / (m.nx - 1) as dx,
            (m.yN - m.y0)::float8 / (m.ny - 1) as dy
        from filtered
        cross join meta m
    ),
    cells as (
        select
            ST_SnapToGrid(
                ST_MakeEnvelope(
                    x0 + i*dx - 0.5*dx,
                    y0 + j*dy - 0.5*dy,
                    x0 + i*dx + 0.5*dx,
                    y0 + j*dy + 0.5*dy,
                    4326
                ),
                dx/100.0, dy/100.0
            )::geometry(Polygon,4326) as geom
        from grid
    ),
    unioned as (
        select ST_Union(geom)::geometry(MultiPolygon,4326) as geom
        from cells
    )
    select CASE
               WHEN ST_XMax(geom) > 180 OR ST_XMin(geom) < -180 THEN ST_ShiftLongitude(geom)
               ELSE geom
           END
    from unioned;
$_$;
