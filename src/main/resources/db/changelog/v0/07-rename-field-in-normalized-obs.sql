--liquibase formatted sql

--changeset event-api-migrations:07-rename-field-in-normalized-obs runOnChange:false

alter table normalized_observations
rename column updated_at to source_updated_at;