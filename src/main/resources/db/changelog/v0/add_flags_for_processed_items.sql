--liquibase formatted sql

--changeset event-api-migrations:v0/add_flags_for_processed_items.sql runOnChange:false
ALTER TABLE data_lake ADD COLUMN normalized boolean;
UPDATE data_lake dl SET normalized  = EXISTS (SELECT 1 FROM normalized_observations nr WHERE dl.observation_id = nr.observation_id);

ALTER TABLE normalized_observations ADD COLUMN recombined boolean;
UPDATE normalized_observations no SET recombined  = EXISTS(SELECT 1 FROM kontur_events ke WHERE ke.observation_id = no.observation_id);
