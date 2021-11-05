--liquibase formatted sql

--changeset event-api-migrations:v0.8/add-populated-area-km2-enrichment.sql runOnChange:false

update feeds set enrichment = '{"population", "osmGapsPercentage", "populatedAreaKm2", "industrialAreaKm2", "forestAreaKm2", "volcanoesCount", "hotspotDaysPerYearMax"}' where alias = 'disaster-ninja-02';
update feeds set enrichment = '{"population", "osmGapsPercentage", "populatedAreaKm2"}' where alias = 'gdacs';
