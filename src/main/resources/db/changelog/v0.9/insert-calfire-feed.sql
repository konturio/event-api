--liquibase formatted sql

--changeset event-api-migrations:v0.9/insert-calfire-feed runOnChange:false

INSERT INTO feeds (feed_id, alias, description, providers)
VALUES (uuid_generate_v4(), 'calfire', 'Real-time wildfires from California (CalFire)', '{"wildfire.calfire"}');