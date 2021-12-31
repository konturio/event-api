--liquibase formatted sql

--changeset event-api-migrations:v1.1/mark-enrichment-skipped-events.sql runOnChange:false

update feed_data set enrichment_skipped = true
where enriched
    and not enrichment_skipped
    and enrichment_attempts > 1
    and (event_details is null
        or exists(select * from jsonb_array_elements(episodes) ep where (ep ->> 'episodeDetails') is null));