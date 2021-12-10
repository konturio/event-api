--liquibase formatted sql

--changeset event-api-migrations:v1.0/add-updated-at-fields.sql runOnChange:false

alter table normalized_observations
    add column if not exists normalized_at timestamp with time zone,
    alter column normalized_at set default current_timestamp;


alter table kontur_events
    add column if not exists recombined_at timestamp with time zone,
    alter column recombined_at set default current_timestamp;

alter table feed_data
    add column if not exists composed_at timestamp with time zone,
    alter column composed_at set default current_timestamp,
    add column if not exists enriched_at timestamp with time zone;
