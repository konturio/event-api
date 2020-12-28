--liquibase formatted sql

--changeset event-api-migrations:add_missed_indexes runOnChange:false

CREATE INDEX ON data_lake (normalized, loaded_at);
CREATE INDEX ON normalized_observations (recombined);




