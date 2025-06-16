--liquibase formatted sql

--changeset event-api-migrations:v0.9/insert-calfire-feed runOnChange:false

insert into feeds (feed_id, alias, description, providers)
values (uuid_generate_v4(), 'calfire', 'Real-time wildfires from California (CalFire)', '{"wildfire.calfire"}');
