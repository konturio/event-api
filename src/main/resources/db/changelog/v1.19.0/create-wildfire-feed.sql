--liquibase formatted sql

--changeset event-api-migrations:v1.19.0/create-wildfire-feed.sql runOnChange:true

insert into feeds (feed_id, alias, name, description, providers, enrichment, enrichment_postprocessors, enrichment_request)
values (
    uuid_generate_v4(),
    'wildfires',
    'Wildfires',
    'Feed with wildfires from California Department of Forestry and Fire Protection (CAL FIRE), National Interagency Fire Center (NIFC), Fire Information for Resource Management System (FIRMS)',
    '{"wildfire.calfire","wildfire.perimeters.nifc","wildfire.locations.nifc","firms.modis-c6","firms.suomi-npp-viirs-c2","firms.noaa-20-viirs-c2"}',
    '{"population", "gdp", "industrialAreaKm2", "buildingCount", "highwayLength", "forestAreaKm2", "volcanoesCount", "hotspotDaysPerYearMax"}',
    '{"wildfireType"}',
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

