--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-jtwc-provider-to-feeds.sql runOnChange:true

update feeds
set providers = providers || '{"cyclones.jtwc.mil"}'
where alias in ('kontur-private', 'kontur-public', 'micglobal');
