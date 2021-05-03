--liquibase formatted sql

--changeset event-api-migrations:update-swiss-re-feed.sql runOnChange:true

update feeds set providers = '{"tornado.canada-gov", "tornado.australian-bm", "tornado.osm-wiki", "storms.noaa", "wildfire.frap.cal", "tornado.des-inventar-sendai", "wildfire.sa-gov", "wildfire.qld-des-gov", "wildfire.victoria-gov", "wildfire.nsw-gov", "tornado.japan-ma"}' where alias = 'swissre-02';