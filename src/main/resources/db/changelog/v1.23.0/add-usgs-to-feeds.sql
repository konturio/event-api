--liquibase formatted sql

--changeset event-api-migrations:v1.24.0/add-usgs-to-feeds.sql runOnChange:true

update feeds
set providers = '{"em-dat","tornado.canada-gov","tornado.australian-bm","tornado.osm-wiki","tornado.des-inventar-sendai","storms.noaa","wildfire.frap.cal","wildfire.sa-gov","wildfire.qld-des-gov","wildfire.victoria-gov","wildfire.nsw-gov","wildfire.calfire","wildfire.perimeters.nifc","wildfire.locations.nifc","cyclones.nhc-at.noaa","cyclones.nhc-ep.noaa","cyclones.nhc-cp.noaa","usgs.earthquake"}'::text[]
where alias = 'kontur-private';

update feeds
set providers = '{"wildfire.calfire","wildfire.perimeters.nifc","wildfire.locations.nifc","storms.noaa","gdacsAlert","gdacsAlertGeometry","cyclones.nhc-at.noaa","cyclones.nhc-ep.noaa","cyclones.nhc-cp.noaa","usgs.earthquake"}'::text[],
    description = 'Feed from California Department of Forestry and Fire Protection (CAL FIRE), National Interagency Fire Center (NIFC), National Oceanic and Atmospheric Administration (NOAA, Storm Events Database), Global Disaster Alert and coordination system (GDACS), National Hurricane Center (NHC), US Geological Survey (USGS)'
where alias = 'micglobal';
