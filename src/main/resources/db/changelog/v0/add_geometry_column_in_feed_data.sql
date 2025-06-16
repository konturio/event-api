--liquibase formatted sql

--changeset event-api-migrations:v0/add_geometry_column_in_feed_data runOnChange:false splitStatements:false

create function collectGeometryFromEpisodes(jsonb) returns geometry
as $$
    select ST_Collect(ST_GeomFromGeoJSON(feature.geometries))
    from (select jsonb_array_elements(e -> 'geometries' -> 'features') -> 'geometry' as geometries
          from jsonb_array_elements($1) e) feature;
    $$
    language sql
    strict
    immutable parallel safe;

alter table feed_data add column collected_geometry geometry generated always as (collectGeometryFromEpisodes(episodes)) stored;
create index on feed_data using gist (collected_geometry);
