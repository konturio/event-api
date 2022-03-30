--liquibase formatted sql

--changeset event-api-migrations:v1.5/update-providers-list-for-kontur-public-feed.sql runOnChange:false

update feeds set providers = '{"gdacsAlert","gdacsAlertGeometry"}' where alias = 'kontur-public-v1';
update feeds set providers = providers || '{"kontur.events"}' where alias = 'kontur-public';
