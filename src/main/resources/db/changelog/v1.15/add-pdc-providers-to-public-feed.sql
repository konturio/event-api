--liquibase formatted sql

--changeset event-api-migrations:v1.15/add-pdc-providers-to-public-feed.sql runOnChange:true

update feeds
set providers = '{gdacsAlert,gdacsAlertGeometry,kontur.events,pdcSqs,pdcMapSrv}'
where alias = 'kontur-public'