--liquibase formatted sql

--changeset event-api-migrations:clear-outdated-tornado-noaa-data.sql runOnChange:false

with tornado_noaa_events as (
    select event_id
    from kontur_events
    where provider = 'tornado.noaa'
)
delete from feed_data fd using tornado_noaa_events
where fd.event_id = tornado_noaa_events.event_id;


with tornado_noaa_events as (
    select event_id
    from kontur_events
    where provider = 'tornado.noaa'
)
delete from feed_event_status fes using tornado_noaa_events
where fes.event_id = tornado_noaa_events.event_id;

delete from kontur_events where provider = 'tornado.noaa';

delete from normalized_observations where provider = 'tornado.noaa';

delete from data_lake where provider = 'tornado.noaa';