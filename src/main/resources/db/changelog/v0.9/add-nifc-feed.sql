--liquibase formatted sql

--changeset event-api-migrations:v0.9/add-public-feed.sql runOnChange:false

INSERT INTO feeds (feed_id, alias, description, providers)
VALUES (uuid_generate_v4(), 'nifc', 'Real-time wildfires from California','{"wildfire.perimeters.nifc","wildfire.locations.nifc"}');
