--liquibase formatted sql

--changeset event-api-migrations:v1.6/add-enrichment-postprocessors-to-feeds.sql runOnChange:false

alter table feeds add column enrichment_postprocessors text[] default '{}';

update feeds set enrichment_postprocessors = '{"wildfire_type"}' where alias = 'kontur-public';