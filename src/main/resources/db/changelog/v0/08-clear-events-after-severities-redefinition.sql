--liquibase formatted sql

--changeset event-api-migrations:08-clear-events-after-severities-redefinition runOnChange:false

delete from normalized_observations;
delete from feed_data;