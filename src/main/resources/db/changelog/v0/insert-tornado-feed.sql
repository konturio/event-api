--liquibase formatted sql

--changeset event-api-migrations:insert-tornado-feed.sql runOnChange:false

INSERT INTO feeds (feed_id, alias, description, providers)
VALUES (uuid_generate_v4(), 'tornado', 'Tornadoes', '{"canada-gov", "australian-bm"}');