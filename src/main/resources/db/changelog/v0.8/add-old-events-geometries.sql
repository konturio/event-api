--liquibase formatted sql

--changeset event-api-migrations:v0.8/add-old-events-geometries.sql runOnChange:false

update feed_data set geometries = collectEventGeometries(episodes) where geometries is null;