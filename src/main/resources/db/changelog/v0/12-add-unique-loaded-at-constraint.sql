--liquibase formatted sql

--changeset event-api-migrations:12-add-unique-loaded-at-constraint runOnChange:false

CREATE UNIQUE INDEX feed_data_updated_at_unique
    ON feed_data (updated_at);

CREATE UNIQUE INDEX normalized_observations_loaded_at_unique
    ON normalized_observations (loaded_at);

CREATE UNIQUE INDEX data_lake_loaded_at_unique
    ON data_lake (loaded_at);
