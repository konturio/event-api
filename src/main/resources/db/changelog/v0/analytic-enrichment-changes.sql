--liquibase formatted sql

--changeset event-api-migrations:analytic-enrichment-changes.sql runOnChange:false

alter table feeds
    add column if not exists enrichment text[],
    alter column enrichment set default '{}';

alter table feed_data
    add column if not exists enriched boolean,
    alter column enriched set default true,
    add column if not exists event_details jsonb,
    alter column event_details set default null;

update feed_data set episodes = to_jsonb((
    select array_agg(jsonb_set(episode, '{episodeDetails}', 'null', true))
    from jsonb_array_elements(episodes) episode
)) where true;

