--liquibase formatted sql

--changeset event-api-migrations:v0/add_normalized_observations_external_id_index.sql runOnChange:false

create index normalized_observations_external_id_index
    on normalized_observations (external_event_id, provider);