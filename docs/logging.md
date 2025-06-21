# Logging Guidelines

This document defines the meaning of each log level in Event API and provides guidance on when to use them. Following these rules helps us to understand application state and react appropriately.

## Log Levels

| Level | Purpose | Expected Action |
| ----- | ------- | --------------- |
| `ERROR` | A problem that requires immediate attention and affects correct behaviour. Use only when the application cannot proceed or data may be lost. | Triggers alerts and must be investigated. |
| `WARN` | Something unexpected happened but processing can continue. Use for situations that should be looked at, yet do not break main workflow. | Review during troubleshooting and fix if necessary. |
| `INFO` | Normal operation messages such as startup logs or important lifecycle events. Use to provide high‑level insight into what the application is doing. | Recorded for auditing and routine monitoring. |
| `DEBUG` | Detailed information useful for debugging. Use for state changes, external calls, and noncritical computations. | Logged in development or when debugging issues. |
| `TRACE` | Very fine‑grained messages. Generally disabled in production. | Enable only when diagnosing complex problems. |

## Examples

- When processing of a message fails permanently, log an `ERROR` with details and the message identifier.
- When a retryable error occurs, log `WARN` and include the retry information.
- Successful updates or scheduled tasks starting should be logged as `INFO`.
- Values computed for decision making can be logged at `DEBUG` if they help troubleshooting.

Consistent usage of these levels reduces noise in logs and ensures alerts correspond to real issues.
