--liquibase formatted sql

--changeset event-api-migrations:v1.13/add-name-to-feeds.sql runOnChange:false

alter table feeds
    add if not exists feeds text;

update feeds set
    name = 'Test CalFire'
where alias = 'test-calfire';

update feeds set
    name = 'Test PDC v0'
where alias = 'test-pdc-v0';

update feeds set
    name = 'Test EM-DAT'
where alias = 'test-em-dat';

update feeds set
    name = 'Test FIRMS'
where alias = 'test-firms';

update feeds set
    name = 'Test GDACS'
where alias = 'test-gdacs';

update feeds set
    name = 'Test NIFC'
where alias = 'test-nifc';

update feeds set
    name = 'Test Cyclones'
where alias = 'test-cyclone';

update feeds set
    name = 'Swiss Re'
where alias = 'swissre-02';

update feeds set
    name = 'Test InciWeb'
where alias = 'test-inciweb';

update feeds set
    name = 'Kontur Public v1'
where alias = 'kontur-public-v1';

update feeds set
    name = 'Kontur Public'
where alias = 'kontur-public';

update feeds set
    name = 'Test Loss'
where alias = 'test-loss';

