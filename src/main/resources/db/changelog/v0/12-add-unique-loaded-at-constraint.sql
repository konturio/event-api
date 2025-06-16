--liquibase formatted sql

--changeset event-api-migrations:12-add-unique-loaded-at-constraint runOnChange:false

create unique index feed_data_updated_at_unique
    on feed_data (updated_at);

create unique index normalized_observations_loaded_at_unique
    on normalized_observations (loaded_at);

create unique index data_lake_loaded_at_unique
    on data_lake (loaded_at);
