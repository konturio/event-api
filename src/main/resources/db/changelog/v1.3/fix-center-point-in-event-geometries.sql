--liquibase formatted sql

--changeset event-api-migrations:v1.3/fix-center-point-in-event-geometries.sql runOnChange:false

update feed_data
set geometries = collectEventGeometries(episodes)
where geometries is not null and (geometries ->> 'features') is null and composed_at > '2022-01-25'