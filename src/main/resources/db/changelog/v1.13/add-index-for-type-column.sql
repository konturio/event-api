--liquibase formatted sql

--changeset event-api-migrations:v1.13/add-index-for-type-column.sql runOnChange:false

create index feed_data_feed_id_severity_type_idx
    on feed_data using gist (feed_id, severity, type)
    where (is_latest_version AND enriched);