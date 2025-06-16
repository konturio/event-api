--liquibase formatted sql

--changeset event-api-migrations:insert-swiss-re-feed.sql runOnChange:false

insert into feeds (feed_id, alias,description, providers)
values (uuid_generate_v4(), 'swissre-02', 'Swiss Re version 02 historical hazards', '{"tornado.canada-gov", "tornado.australian-bm", "tornado.osm-wiki", "tornado.noaa", "wildfire.frap.cal"}');
