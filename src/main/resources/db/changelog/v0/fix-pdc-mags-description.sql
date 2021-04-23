--liquibase formatted sql

--changeset event-api-migrations:fix-pdc-mags-description.sql runOnChange:false

update normalized_observations set episode_description = null where provider = 'hpSrvMag';

with mag_events as (
    select
        distinct event_id
    from kontur_events
    where provider = 'hpSrvMag'
)
delete from feed_data fd using mag_events me
where fd.event_id = me.event_id;

with mag_events as (
    select
        distinct event_id
    from kontur_events
    where provider = 'hpSrvMag'
)
update feed_event_status set actual = false
where event_id in (select event_id from mag_events)