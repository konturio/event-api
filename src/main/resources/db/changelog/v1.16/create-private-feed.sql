--liquibase formatted sql

--changeset event-api-migrations:v1.16/create-private-feed.sql runOnChange:true

insert into feeds (
    feed_id,
    alias,
    name,
    description,
    providers,
    enrichment_postprocessors,
    enrichment,
    enrichment_request
)
values (
    uuid_generate_v4(),
    'kontur-private',
    'Kontur Private',
    'Kontur Private Feed',
    '{"pdcSqs", "pdcMapSrv"}',
    '{"loss", "wildfireType"}',
    '{"population", "gdp", "industrialAreaKm2", "buildingCount", "highwayLength", "forestAreaKm2", "volcanoesCount", "hotspotDaysPerYearMax"}',
    '{
      polygonStatistic (
        polygonStatisticRequest: {
            polygon: "%s"
        }
      )
      {
        analytics {
          functions(args:[
            {name:"sumX", id:"populatedAreaKm2", x:"populated_area_km2"},
            {name:"sumX", id:"industrialAreaKm2", x:"industrial_area"},
            {name:"sumX", id:"forestAreaKm2", x:"forest"},
            {name:"sumX", id:"volcanoesCount", x:"volcanos_count"},
            {name:"maxX", id:"hotspotDaysPerYearMax", x:"wildfires"},
            {name:"percentageXWhereNoY", id:"osmGapsPercentage", x:"populated_area_km2", y:"count"},
            {name:"sumX", id:"buildingCount", x:"building_count"},
            {name:"sumX", id:"highwayLength", x:"highway_length"},
          ]) {
            id,
            result
          },
          population {
            population
            gdp
          }
        }
      }
    }'
);