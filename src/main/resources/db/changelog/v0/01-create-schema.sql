--liquibase formatted sql

--changeset event-api-migrations:01-create-schema
CREATE TABLE IF NOT EXISTS hazards_data (
    hazard_id text,
    create_date timestamptz,
    upload_date timestamptz,
    provider varchar(16),
    data text,

    CONSTRAINT hazard_episode_number UNIQUE (hazard_id, provider)
);