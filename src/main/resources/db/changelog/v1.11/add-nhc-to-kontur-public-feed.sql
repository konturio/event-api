--liquibase formatted sql

--changeset event-api-migrations:v1.11/add-nhc-to-kontur-public-feed.sql runOnChange:false

update feeds set providers = providers || '{"cyclones.nhc-at.noaa", "cyclones.nhc-ep.noaa", "cyclones.nhc-cp.noaa"}'
             where alias = 'kontur-public';
