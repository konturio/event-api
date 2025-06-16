--liquibase formatted sql

--changeset event-api-migrations:v0/add_constraints runOnChange:false
alter table feed_event_status alter column actual set not null;
alter table data_lake alter column normalized set not null;
alter table normalized_observations alter column recombined set not null;
