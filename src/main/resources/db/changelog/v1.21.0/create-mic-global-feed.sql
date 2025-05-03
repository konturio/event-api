--liquibase formatted sql

--changeset event-api-migrations:v1.19.0/create-wildfire-feed.sql runOnChange:true

insert into feeds (feed_id, alias, name, description, providers, enrichment, enrichment_postprocessors, enrichment_request)
values (
           uuid_generate_v4(),
           'micglobal',
           'MIC Global',
           'Feed from California Department of Forestry and Fire Protection (CAL FIRE), National Interagency Fire Center (NIFC), National Oceanic and Atmospheric Administration (NOAA, Storm Events Database), Global Disaster Alert and coordination system (GDACS), National Hurricane Center (NHC)',
           '{"wildfire.calfire","wildfire.perimeters.nifc","wildfire.locations.nifc","storms.noaa","gdacsAlert","gdacsAlertGeometry","cyclones.nhc-at.noaa","cyclones.nhc-ep.noaa","cyclones.nhc-cp.noaa"}',
           '{"population"}',
           '{}',
           '{
               polygonStatistic (
                   polygonStatisticRequest: {
                       polygon: "%s"
                   }
               )
               {
                   analytics {
                       population {
                           population
                           gdp
                       }
                   }
               }
           }'
       );