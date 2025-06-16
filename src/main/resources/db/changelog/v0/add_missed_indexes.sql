--liquibase formatted sql

--changeset event-api-migrations:add_missed_indexes runOnChange:false

create index on data_lake (normalized, loaded_at);
create index on normalized_observations (recombined);




