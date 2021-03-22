--liquibase formatted sql

--changeset event-api-migrations:insert-swissre-02-feed.sql runOnChange:false

INSERT INTO feeds (feed_id, alias, description, providers)
VALUES (uuid_generate_v4(), 'swissre-02', 'Swiss Re version 02 historical hazards', '{"canada-gov", "australian-bm"}');