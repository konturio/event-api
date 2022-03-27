--liquibase formatted sql

--changeset event-api-migrations:v1.5/update-provides-list-for-kontur-public-v1-feed.sql runOnChange:false

update feeds set providers = providers || '{"kontur.events"}' where alias = 'kontur-public-v1';
