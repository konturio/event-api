--liquibase formatted sql

--changeset event-api-migrations:v0/add-em-dat-feed.sql runOnChange:false

INSERT INTO feeds (feed_id, alias, description, providers)
VALUES (uuid_generate_v4(), 'emdat', 'EM-DAT', '{"em-dat"}');

