--liquibase formatted sql

--changeset event-api-migrations:v1.5.1/filling-urls-from-source-uri-fot-normalized-observations.sql runOnChange:false

update normalized_observations set urls = urls || source_uri
  where source_uri is not null and (urls is null or urls = '{}');
