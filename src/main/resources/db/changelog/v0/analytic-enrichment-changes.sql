--liquibase formatted sql

--changeset event-api-migrations:analytic-enrichment-changes.sql runOnChange:true

alter table feeds add column if not exists enrichment text[] default '{}';

alter table feed_data
    add column if not exists enriched boolean default true,
    add column if not exists event_details jsonb default null;

