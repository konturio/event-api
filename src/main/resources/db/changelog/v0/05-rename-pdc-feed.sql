--liquibase formatted sql

--changeset event-api-migrations:05-rename-pdc-feed runOnChange:false

update feeds set alias = 'pdc-v0' where alias = 'pdc';