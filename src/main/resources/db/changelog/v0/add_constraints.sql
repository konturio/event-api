--liquibase formatted sql

--changeset event-api-migrations:v0/add_constraints runOnChange:false
ALTER TABLE feed_event_status ALTER COLUMN actual SET NOT NULL;
ALTER TABLE data_lake ALTER COLUMN normalized SET NOT NULL;
ALTER TABLE normalized_observations ALTER COLUMN recombined SET NOT NULL;
