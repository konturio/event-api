# Multithreaded Pipeline Jobs

This document outlines the proposed redesign of scheduled jobs to improve throughput by introducing multithreading.

## Goals
- Allow independent job steps to run in parallel when possible.
- Keep transactional boundaries clear for database operations.
- Provide simple configuration to tune the level of concurrency.

## Proposed Design
1. **Job Interfaces**
   - Each job remains a `Runnable` implementation.
   - Jobs that can be executed concurrently expose a new method `getConcurrency()` returning the desired thread count.
2. **Scheduler Changes**
   - `WorkerScheduler` will submit jobs to a common `ExecutorService` with a thread pool sized from configuration.
   - The scheduler will check `getConcurrency()` and create that many asynchronous tasks.
3. **Transactional Work**
   - Database updates remain wrapped in Spring transactions. Each thread will open and close its own transaction.
   - Long read‑only operations avoid holding locks for the duration of the whole job.
4. **Error Handling**
   - Job failures in one thread do not stop other threads. All exceptions are logged and collected for metrics.
   - When at least one thread fails, the job result is marked as failed.
5. **Configuration**
   - A new property `scheduler.worker.poolSize` defines the global thread pool size.
   - Individual jobs may override the default by returning a specific concurrency value.

## Rationale
Parallel job execution allows IO‑bound tasks (such as external API calls) to finish faster. Keeping transactions isolated per thread ensures data consistency while still enabling concurrency.

