--liquibase formatted sql

--changeset event-api-migrations:v0/add-feeds-providers-index.sql runOnChange:false

CREATE INDEX if not exists feeds_providers ON feeds USING GIN (providers);