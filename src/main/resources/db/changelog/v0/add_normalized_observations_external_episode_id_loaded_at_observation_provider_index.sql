--liquibase formatted sql

--changeset event-api-migrations:v0/add_normalized_observations_external_episode_id_loaded_at_observation_provider_index.sql runOnChange:false

CREATE INDEX normalized_observations_external_episode_id_loaded_at_observation_provider_index
    ON normalized_observations (external_episode_id, loaded_at, observation_id, provider);
