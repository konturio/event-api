--liquibase formatted sql

--changeset event-api-migrations:v0.9/rename-technical-feeds.sql runOnChange:false

update feeds set alias = 'test-pdc-v0' where alias = 'pdc-v0';
update feeds set alias = 'test-em-dat' where alias = 'em-dat';
update feeds set alias = 'test-firms' where alias = 'firms';
update feeds set alias = 'test-gdacs' where alias = 'gdacs';
update feeds set alias = 'test-calfire' where alias = 'calfire';
update feeds set alias = 'test-nifc' where alias = 'nifc';
