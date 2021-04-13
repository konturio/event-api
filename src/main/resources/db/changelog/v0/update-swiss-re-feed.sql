--liquibase formatted sql

--changeset event-api-migrations:update-swiss-re-feed.sql runOnChange:true

update feeds set providers = '{"tornado.canada-gov", "tornado.australian-bm", "tornado.osm-wiki", "storms.noaa", "wildfire.frap.cal", "tornado.des-inventar-sendai"}' where alias = 'swissre-02';