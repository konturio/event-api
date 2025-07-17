--liquibase formatted sql

--changeset event-api-migrations:v1.25.0/create-pga-mask-function.sql runOnChange:true

drop function if exists buildPgaMask;

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
            m.*,
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
    )
    select ST_Union(geom)::geometry(MultiPolygon,4326) from cells;
$_$;
