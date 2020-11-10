--liquibase formatted sql

--changeset event-api-migrations:v0/add_kontur_events_observation_id_index.sql runOnChange:false

create index kontur_events_observation_id_index
    on kontur_events (observation_id);