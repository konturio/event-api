--liquibase formatted sql

--changeset event-api-migrations:v1.23.0/create-cross-merge-feed.sql runOnChange:true
insert into feeds (feed_id, alias, name, description, providers, enrichment, enrichment_postprocessors, enrichment_request)
values (
    uuid_generate_v4(),
    'test-cross-provider-merge',
    'Test Cross Provider Merge',
    'The feed contains global data from multiple providers.',
    '{"wildfire.inciweb","wildfire.perimeters.nifc","wildfire.locations.nifc","wildfire.calfire"}',
    '{}',
    '{}',
    null
);

insert into feed_event_status (feed_id, event_id, actual)
select distinct on (event_id) f.feed_id, ke.event_id, false
from kontur_events ke
join feeds f on f.alias = 'test-cross-provider-merge'
where ke.provider in ('wildfire.inciweb','wildfire.perimeters.nifc','wildfire.locations.nifc','wildfire.calfire')
on conflict do nothing;
