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

### Networking through a proxy

Maven and the Java runtime read standard `HTTP_PROXY` and `HTTPS_PROXY`
environment variables. The project ships a `.mvn` directory that configures
Maven to forward those settings automatically. If your network requires a
proxy, export the variables before running the build:

```bash
export HTTP_PROXY=http://proxy:8080
export HTTPS_PROXY=http://proxy:8080
mvn verify
```

