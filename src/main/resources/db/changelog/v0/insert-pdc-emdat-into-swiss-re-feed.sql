--liquibase formatted sql

--changeset event-api-migrations:insert-pdc-emdat-into-swiss-re-feed.sql runOnChange:false


insert into feed_event_status (feed_id, event_id, actual)
select distinct on (event_id) fs.feed_id, event_id, false
from kontur_events, feeds fs
where provider in ('hpSrvMag', 'hpSrvSearch', 'pdcSqs', 'pdcMapSrv', 'em-dat')
  and alias = 'swissre-02';
