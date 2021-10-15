--liquibase formatted sql

--changeset event-api-migrations:v0/add-feed-data-enrichment-attempts-field.sql

alter table feed_data add column enrichment_attempts bigint;
