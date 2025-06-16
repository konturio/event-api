--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/create-cross-provider-merge-feed.sql runOnChange:true

insert into feeds (
    feed_id,
    alias,
    description,
    providers,
    enrichment,
    enrichment_request,
    enrichment_postprocessors
)
values (
    uuid_generate_v4(),
    'test-cross-provider-merge',
    'The feed contains global data from multiple providers.',
    '{"wildfire.inciweb","wildfire.perimeters.nifc","wildfire.locations.nifc","wildfire.calfire"}',
    '{}',
    null,
    '{}'
);
