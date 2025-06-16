--liquibase formatted sql

--changeset event-api-migrations:v0/add-feeds-providers-index.sql runOnChange:false

create index if not exists feeds_providers on feeds using gin (providers);
