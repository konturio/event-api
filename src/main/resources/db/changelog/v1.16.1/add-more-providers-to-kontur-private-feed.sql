--liquibase formatted sql

--changeset event-api-migrations:v1.16.1/add-more-providers-to-kontur-private-feed.sql runOnChange:true

update feeds
set providers = '{"pdcSqs", "pdcMapSrv", "em-dat", "tornado.canada-gov", "tornado.australian-bm",
                "tornado.osm-wiki", "tornado.des-inventar-sendai", "storms.noaa", "wildfire.frap.cal",
                "wildfire.sa-gov", "wildfire.qld-des-gov", "wildfire.victoria-gov", "wildfire.nsw-gov",
                "wildfire.calfire", "wildfire.perimeters.nifc", "wildfire.locations.nifc", "cyclones.nhc-at.noaa",
                "cyclones.nhc-ep.noaa", "cyclones.nhc-cp.noaa"}'::text[]
where alias = 'kontur-private';

