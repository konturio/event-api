--liquibase formatted sql

--changeset event-api-migrations:04-update-pdc-feed runOnChange:true

update feeds set providers = '{"hpSrvMag", "hpSrvSearch", "pdcSqs", "pdcMapSrv"}' where alias = 'pdc-v0';