--liquibase formatted sql

--changeset event-api-migrations:v1.23.0/add-external-event-id-to-kontur-events.sql runOnChange:false
alter table kontur_events add column if not exists external_event_id text;
create unique index if not exists kontur_events_event_external_id_idx
    on kontur_events (event_id, external_event_id)
    where external_event_id is not null;
update kontur_events ke
set external_event_id = no.external_event_id
from normalized_observations no
where ke.observation_id = no.observation_id
  and no.provider in ('wildfire.calfire','wildfire.inciweb','wildfire.perimeters.nifc','wildfire.locations.nifc')
  and ke.external_event_id is null;
