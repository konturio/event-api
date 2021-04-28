--liquibase formatted sql

--changeset event-api-migrations:v0/add-feed-data-types-column.sql runOnChange:false

CREATE OR REPLACE FUNCTION collectTypesFromEpisodes(episodes jsonb) RETURNS text[]
AS $$
    select array_agg(t.type)
    from (select distinct jsonb_array_elements(episodes) ->> 'type' as type) as t
    $$
    LANGUAGE SQL
    STRICT
    IMMUTABLE
    PARALLEL SAFE;

alter table feed_data
    add column episode_types text[] GENERATED ALWAYS AS (collectTypesFromEpisodes(episodes)) STORED;

