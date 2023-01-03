--liquibase formatted sql

--changeset event-api-migrations:v1.15/add-normalized-observations-origin-column.sql runOnChange:true

alter table normalized_observations add column origin text;