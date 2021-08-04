--liquibase formatted sql

--changeset event-api-migrations:feed-analytics.sql runOnChange:true

update feeds set enrichment = '{"population", "peopleWithoutOsmBuildings", "areaWithoutOsmBuildingsKm2", "peopleWithoutOsmRoads", "areaWithoutOsmRoadsKm2", "peopleWithoutOsmObjects", "areaWithoutOsmObjectsKm2", "osmGapsPercentage", "industrialAreaKm2", "forestAreaKm2", "volcanoesCount", "hotspotDaysPerYearMax"}' where alias = 'gdacs-firms';