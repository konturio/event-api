--liquibase formatted sql

--changeset event-api-migrations:v1.0/add-updated-at-fields-indexes.sql runOnChange:false

create index if not exists normalized_observations_normalized_at_idx
    on normalized_observations (normalized_at);

create index if not exists kontur_events_recombined_at_idx
    on kontur_events (recombined_at);

create index if not exists feed_data_composed_at_idx
    on feed_data (composed_at);

create index if not exists feed_data_enriched_at_idx
    on feed_data (enriched_at);