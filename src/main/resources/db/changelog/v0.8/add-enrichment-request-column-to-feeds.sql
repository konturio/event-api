--liquibase formatted sql

--changeset event-api-migrations:v0.8/add-enrichment-request-column-to-feeds.sql runOnChange:false

alter table feeds add column if not exists enrichment_request text;

update feeds set enrichment_request = '{
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
        {name:"percentageXWhereNoY", id:"osmGapsPercentage", x:"populated_area_km2", y:"count"}
      ]) {
        id,
        result
      },
      population {
        population
      }
    }
  }
}'
where alias = 'disaster-ninja-02';


update feeds set enrichment_request = '{
  polygonStatistic (
    polygonStatisticRequest: {
    	polygon: "%s"
    }
  )
  {
    analytics {
      functions(args:[
        {name:"sumX", id:"populatedAreaKm2", x:"populated_area_km2"},
        {name:"percentageXWhereNoY", id:"osmGapsPercentage", x:"populated_area_km2", y:"count"}
      ]) {
        id,
        result
      },
      population {
        population
      }
    }
  }
}'
where alias = 'gdacs';