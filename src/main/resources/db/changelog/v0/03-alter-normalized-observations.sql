--liquibase formatted sql

--changeset event-api-migrations:03-alter-normalized-observations runOnChange:false

alter table normalized_observations rename external_id TO external_event_id;