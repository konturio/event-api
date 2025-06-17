# Event API Microservice Architecture

This document describes the proposed architecture for the Event API service. It focuses on the main building blocks and how they interact. The design is split into iterations so the system can be delivered incrementally.

## Overview

The Event API aggregates information about natural events from various providers. The service normalizes and enriches the raw data and exposes a unified REST API for consumers. Components communicate through AWS SQS queues and share a PostgreSQL database with PostGIS extensions.

```
@startuml
!include ../design/architecture.puml
@enduml
```

## Components

- **Ingestion services** – small workers that pull data from external providers (PDC, GDACS, FIRMS, etc.) and put raw payloads into the `data_lake` table. Each worker reads provider specific configuration and is scheduled separately.
- **Normalization service** – transforms raw records from the `data_lake` into a common event schema. Normalized events are stored in `normalized_observations`.
- **Enrichment service** – adds additional metadata such as geometries or severity levels to normalized events.
- **Combination service** – merges observations that describe the same phenomenon into a single event record.
- **Event API** – Spring Boot application exposing REST endpoints (`/v1/**`). It reads pre‑processed events from the database, provides filtering capabilities and publishes updates to AWS SQS when new data arrives.
- **Database** – PostgreSQL with PostGIS for spatial queries. Liquibase is used for migrations.
- **Message queue** – AWS SQS used to notify downstream services about new events and handle retries.

## Iterative Implementation

1. **Baseline** – deploy the Event API with a single ingestion service and database migrations. Expose read‑only endpoints and verify that data flows from provider to REST API.
2. **Normalization and enrichment** – introduce normalization and enrichment services. Add background jobs that process the data lake and populate normalized tables.
3. **Combination logic** – implement the event combination and episode composition jobs. Ensure that pagination and filtering work correctly on the API level.
4. **Scalability and monitoring** – containerize all services, configure horizontal scaling and add metrics collection. Document SQS queues and retry policies.
5. **Additional providers** – incrementally add more ingestion services using the same pattern.

Each iteration should produce a deployable artifact so the team can gather feedback and adjust the design.

