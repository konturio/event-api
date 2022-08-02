--liquibase formatted sql

--changeset event-api-migrations:v1.11/insert-nhc-feed.sql runOnChange:false

INSERT INTO feeds (feed_id, alias, description, providers)
VALUES (uuid_generate_v4(), 'test-nhc', 'Real-time cyclones from NHC',
        '{"cyclones.nhc-at.noaa", "cyclones.nhc-ep.noaa", "cyclones.nhc-cp.noaa"}');
