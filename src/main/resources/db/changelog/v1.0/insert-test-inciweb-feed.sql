--liquibase formatted sql

--changeset event-api-migrations:v1.0/insert-test-inciweb-feed runOnChange:false

insert into feeds (feed_id, alias, description, providers)
values (uuid_generate_v4(), 'test-inciweb', 'Real-time wildfires of the USA from Incident Information System (InciWeb)', '{"wildfire.inciweb"}');
