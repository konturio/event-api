--liquibase formatted sql

--changeset event-api-migrations:feed-analytics.sql runOnChange:true

update feeds set enrichment = '{"population", "peopleWithoutOsmBuildings", "areaWithoutOsmBuildingsKm2", "peopleWithoutOsmRoads", "areaWithoutOsmRoadsKm2", "peopleWithoutOsmObjects", "areaWithoutOsmObjectsKm2", "osmGapsPercentage", "industrialAreaKm2", "forestAreaKm2", "volcanoesCount", "hotspotDaysPerYearMax", "urbanCorePopulation", "urbanCoreAreaKm2", "totalPopulatedAreaKm2"}' where alias = 'disaster-ninja-02';
update feeds set enrichment = '{"population", "peopleWithoutOsmBuildings", "areaWithoutOsmBuildingsKm2", "peopleWithoutOsmRoads", "areaWithoutOsmRoadsKm2", "peopleWithoutOsmObjects", "areaWithoutOsmObjectsKm2", "osmGapsPercentage", "urbanCorePopulation", "urbanCoreAreaKm2", "totalPopulatedAreaKm2"}' where alias = 'gdacs';