--liquibase formatted sql

--changeset event-api-migrations:v0/add_geometry_column_in_feed_data runOnChange:false splitStatements:false

CREATE FUNCTION collectGeometryFromEpisodes(jsonb) RETURNS geometry
AS $$
    select ST_Collect(ST_GeomFromGeoJSON(feature.geometries))
    from (select jsonb_array_elements(e -> 'geometries' -> 'features') -> 'geometry' as geometries
          from jsonb_array_elements($1) e) feature;
    $$
    LANGUAGE SQL
    STRICT
    IMMUTABLE PARALLEL SAFE;

ALTER TABLE feed_data ADD COLUMN collected_geometry geometry GENERATED ALWAYS AS (collectGeometryFromEpisodes(episodes)) STORED;
CREATE INDEX ON feed_data USING GIST (collected_geometry);