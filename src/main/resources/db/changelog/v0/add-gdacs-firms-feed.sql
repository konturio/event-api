--liquibase formatted sql

--changeset event-api-migrations:v0/add-gdacs-firms-feed.sql runOnChange:false
INSERT INTO feeds (feed_id, alias, description, providers)
VALUES (uuid_generate_v4(), 'gdacs-firms', 'GDACS+FIRMS',
        '{"firms.modis-c6","firms.suomi-npp-viirs-c2","firms.noaa-20-viirs-c2","gdacsAlert","gdacsAlertGeometry"}');

insert into feed_event_status (feed_id, event_id, actual)
select distinct on (event_id) fs.feed_id, event_id, false
from kontur_events,
    feeds fs
where provider in
      ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2', 'gdacsAlert', 'gdacsAlertGeometry')
  and alias = 'gdacs-firms';
