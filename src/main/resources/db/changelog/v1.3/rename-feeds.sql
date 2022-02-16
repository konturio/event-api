--liquibase formatted sql

--changeset event-api-migrations:v1.3/rename-feeds.sql runOnChange:false

update feeds set alias = 'kontur-public-v1' where alias = 'kontur-public';

update feeds set alias = 'kontur-public' where alias = 'disaster-ninja-02';
