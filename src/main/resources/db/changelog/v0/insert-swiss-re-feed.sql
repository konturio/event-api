--liquibase formatted sql

--changeset event-api-migrations:insert-swiss-re-feed.sql runOnChange:false

INSERT INTO feeds (feed_id, alias,description, providers)
VALUES (uuid_generate_v4(), 'swissre-02', 'Swiss Re version 02 historical hazards', '{"tornado.canada-gov", "tornado.australian-bm", "tornado.noaa"}');