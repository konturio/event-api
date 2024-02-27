--liquibase formatted sql

--changeset event-api-migrations:v1.17.2/update-pdc-feed-enrichment-config.sql runOnChange:true

update feeds
set enrichment_request = '{
        polygonStatistic (
            polygonStatisticRequest: {
                polygon: "%s"
            }
        )
        {
            analytics {
                functions(args:[
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
                    population
                }
            }
        }
    }'
where alias = 'pdc';