--liquibase formatted sql

--changeset event-api-migrations:v1.14/update-index-severity.sql runOnChange:true

create index feed_data_severity_updated_at
    on feed_data(severity, updated_at)
    where is_latest_version AND enriched;