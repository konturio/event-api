--liquibase formatted sql

--changeset event-api-migrations:v1.5.1/remove-unused-source-uri-field-from-normalized-observations.sql runOnChange:false

alter table normalized_observations drop column source_uri;
