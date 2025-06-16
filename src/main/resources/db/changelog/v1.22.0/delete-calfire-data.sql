--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/delete-calfire-data.sql runOnChange:false

-- remove unused CalFire provider data
delete from feed_data
where feed_id in (select feed_id from feeds where alias in ('calfire', 'test-calfire'));

delete from feed_event_status
where feed_id in (select feed_id from feeds where alias in ('calfire', 'test-calfire'));

delete from kontur_events
where provider = 'wildfire.calfire';

delete from normalized_observations
where provider = 'wildfire.calfire';

delete from data_lake
where provider = 'wildfire.calfire';

delete from feeds
where alias in ('calfire', 'test-calfire');
