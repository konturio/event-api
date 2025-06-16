--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/recalculate-inciweb.sql runOnChange:true

with inciweb_event_ids as (
    select distinct event_id
    from kontur_events
    where provider = 'wildfire.inciweb'
)
delete from feed_data fd
using inciweb_event_ids iei
where fd.event_id = iei.event_id;

with inciweb_event_ids as (
    select distinct event_id
    from kontur_events
    where provider = 'wildfire.inciweb'
)
delete from feed_event_status fes
using inciweb_event_ids iei
where fes.event_id = iei.event_id;

delete from kontur_events
where provider = 'wildfire.inciweb';

delete from normalized_observations
where provider = 'wildfire.inciweb';

update data_lake
set normalized = false
where provider = 'wildfire.inciweb';
