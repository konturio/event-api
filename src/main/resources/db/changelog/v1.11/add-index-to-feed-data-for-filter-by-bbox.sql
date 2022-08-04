--liquibase formatted sql

--changeset event-api-migrations:v1.11/add-index-to-feed-data-for-filter-by-bbox.sql runOnChange:false

create index feed_data_feed_id_latest_severity_collected_geometry_idx
    on feed_data
        using gist(feed_id, latest_severity, collected_geometry)
    where (is_latest_version AND enriched);
