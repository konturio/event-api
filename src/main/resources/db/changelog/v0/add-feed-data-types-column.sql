--liquibase formatted sql

--changeset event-api-migrations:v0/add-feed-data-types-column.sql runOnChange:false

create or replace function collectTypesFromEpisodes(episodes jsonb) returns text[]
as $$
    select array_agg(t.type)
    from (select distinct jsonb_array_elements(episodes) ->> 'type' as type) as t
    $$
    language sql
    strict
    immutable
    parallel safe;

alter table feed_data
    add column episode_types text[] generated always as (collectTypesFromEpisodes(episodes)) stored;

