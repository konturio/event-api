--liquibase formatted sql

--changeset event-api-migrations:17-insert-gdacs-feed runOnChange:false

INSERT INTO feeds (feed_id, alias, description, providers)
VALUES (uuid_generate_v4(), 'gdacs', 'Global Disaster Alert and Coordination System feed', '{"gdacs"}');