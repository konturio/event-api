--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-embedding-column.sql runOnChange:true

create extension if not exists vector;

alter table feed_data add column if not exists embedding vector(768);

create index if not exists feed_data_embedding_idx on feed_data using ivfflat (embedding vector_l2_ops);
