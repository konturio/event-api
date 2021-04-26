--liquibase formatted sql

--changeset event-api-migrations:v0/add-feed-data-types-column.sql runOnChange:false

CREATE OR REPLACE FUNCTION collectTypesFromEpisodes(episodes jsonb) RETURNS text[]
AS $$
    select array_agg(t.type::text)
    from (select distinct jsonb_array_elements(episodes) ->> 'type' as type) as t
    $$
    LANGUAGE SQL
    STRICT
    IMMUTABLE
    PARALLEL SAFE;

alter table feed_data
    add column collected_types text[] GENERATED ALWAYS AS (collectTypesFromEpisodes(episodes)) STORED;

CREATE INDEX if not exists feed_data_feed_id_types ON feed_data USING GIN (feed_id, collected_types);


