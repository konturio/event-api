--liquibase formatted sql

--changeset event-api-migrations:01-create-schema
CREATE TABLE IF NOT EXISTS event_data_lake (
    hazard_id text,
    create_date timestamptz,
    update_date timestamptz,
    upload_date timestamptz,
    provider varchar(16),
    data text,

    CONSTRAINT hazard_episode_number UNIQUE (hazard_id, provider, update_date)
);

CREATE INDEX ON event_data_lake (provider, update_date);