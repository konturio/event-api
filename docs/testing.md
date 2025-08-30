# Running tests

All unit and integration tests run with Maven by default:

```bash
mvn test
```

Most tests spin up PostgreSQL and Redis containers using Testcontainers.
They run by default together with unit tests.
If Docker isn't available you can skip them:

```bash
mvn -DskipITs=true test
```

Unit tests always run regardless of Docker availability.

Maven honours proxy variables.
The JVM arguments from `.mvn/jvm.config` enable usage of `HTTP_PROXY` and `HTTPS_PROXY`.
`make` targets append proxy command-line arguments when these variables are set, avoiding global configuration changes.
