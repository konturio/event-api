--liquibase formatted sql

--changeset event-api-migrations:v1.6/add-loss-feed.sql runOnChange:false

insert into feeds (feed_id, alias, description, providers, enrichment, enrichment_postprocessors, enrichment_request)
VALUES (
    uuid_generate_v4(),
    'test-loss',
    'Test feed for loss estimation',
    '{"gdacsAlert", "gdacsAlertGeometry", "pdcMapSrv", "pdcSqs"}',
    '{"population", "gdp", "industrialAreaKm2", "buildingCount", "highwayLength"}',
    '{"loss"}',
    '
{
  polygonStatistic(
    polygonStatisticRequest: {
      polygon: "%s"
    }
  )
  {
    analytics {
      functions(args: [
        { name: "sumX", id: "buildingCount", x: "building_count" },
        { name: "sumX", id: "highwayLength", x: "highway_length" },
        { name: "sumX", id: "industrialAreaKm2", x: "industrial_area" }
      ])
      {
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
