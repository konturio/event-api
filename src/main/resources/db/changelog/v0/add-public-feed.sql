--liquibase formatted sql

--changeset event-api-migrations:v0/add-public-feed.sql runOnChange:false

insert into feeds (feed_id, alias, description, providers)
values (uuid_generate_v4(), 'kontur-public', 'Public feed','{"gdacsAlert","gdacsAlertGeometry"}');

insert into feed_event_status (feed_id, event_id, actual)
select distinct on (event_id) fs.feed_id, event_id, false
from kontur_events, feeds fs
where provider in ('gdacsAlert', 'gdacsAlertGeometry') and alias = 'kontur-public'
on conflict do nothing;
