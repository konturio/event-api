--liquibase formatted sql

--changeset event-api-migrations:v1.12.1/update-indexes-severity-type.sql runOnChange:false


drop index feed_data_feed_id_latest_severity_collected_geometry_idx;

create index feed_data_feed_id_severity_collected_geometry_idx
    on feed_data using gist (feed_id, severity, collected_geometry)
    where (is_latest_version AND enriched);