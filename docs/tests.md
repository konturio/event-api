# Testing Guidelines

Run all tests with `mvn verify`.
Docker is required for the Testcontainers-based integration tests.
Those tests are skipped automatically when Docker is not available.
To skip them manually, pass the `skip.integration.tests` property:

```bash
mvn verify -Dskip.integration.tests=true
```
Source maps are uploaded to Sentry only when `sentry.skip` is `false`.
The default configuration sets it to `true`, so no extra flags are needed for local runs.

