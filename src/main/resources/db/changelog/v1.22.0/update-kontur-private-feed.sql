--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/update-kontur-private-feed.sql runOnChange:true

update feeds
set enrichment = '{"population","gdp","industrialAreaKm2","buildingCount","highwayLength","forestAreaKm2","volcanoesCount","hotspotDaysPerYearMax","populationByCountry","osmGapsPrecentageByCountry"}',
    enrichment_request = '{
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
           {name:"populationByCountry", id:"populationByCountry"},
           {name:"osmGapsPrecentageByCountry", id:"osmGapsPrecentageByCountry"}
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
where alias = 'kontur-private';
