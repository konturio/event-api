--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-merge-columns-to-feed-data.sql runOnChange:true
alter table feed_data
    add column if not exists merge_done boolean default true,
    add column if not exists external_event_ids text[] default '{}'::text[],
    add column if not exists providers text[] default '{}'::text[];

--changeset event-api-migrations:v1.22.0/add-external-event-id-to-kontur-events.sql runOnChange:true
alter table kontur_events
    add column if not exists external_event_id text;
create unique index if not exists kontur_events_event_external_id_uq on kontur_events(event_id, external_event_id);
