--liquibase formatted sql

--changeset event-api-migrations:v1.17.2/refactor-feeds.sql runOnChange:true

update feeds
set providers = '{"em-dat",
                "tornado.canada-gov", "tornado.australian-bm", "tornado.osm-wiki", "tornado.des-inventar-sendai",
                "storms.noaa",
                "wildfire.frap.cal", "wildfire.sa-gov", "wildfire.qld-des-gov", "wildfire.victoria-gov", "wildfire.nsw-gov",
                "wildfire.calfire", "wildfire.perimeters.nifc", "wildfire.locations.nifc",
                "cyclones.nhc-at.noaa", "cyclones.nhc-ep.noaa", "cyclones.nhc-cp.noaa"}'::text[]
where alias = 'kontur-private';

insert into feeds (feed_id, alias, name, description, providers, enrichment_postprocessors, enrichment, enrichment_request)
values (
    uuid_generate_v4(), 'pdc', 'PDC', 'Pacific Disaster Center feed', '{"pdcSqs", "pdcMapSrv", "pdcSqsNasa", "pdcMapSrvNasa"}',
    '{"loss"}', '{"population", "populatedAreaKm2", "gdp", "buildingCount", "highwayLength", "osmGapsPercentage", "industrialAreaKm2"}',
    '{
        polygonStatistic (
            polygonStatisticRequest: {
                polygon: "%s"
            }
        )
        {
            analytics {
                functions(args:[
                    {name:"sumX", id:"population", x:"population"},
                    {name:"sumX", id:"populatedAreaKm2", x:"populated_area_km2"},
                    {name:"percentageXWhereNoY", id:"osmGapsPercentage", x:"populated_area_km2", y:"count"},
                    {name:"sumX", id:"buildingCount", x:"building_count"},
                    {name:"sumX", id:"highwayLength", x:"highway_length"},
                    {name:"sumX", id:"industrialAreaKm2", x:"industrial_area"}
                ]) {
                    id,
                    result
                },
                population {
                    gdp
                }
            }
        }
    }'
);

