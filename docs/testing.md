# Running tests

Unit tests run with Maven:

```bash
mvn test
```

To run both unit and integration tests use Maven verify:

```bash
mvn verify
```

Most tests spin up PostgreSQL and Redis containers using Testcontainers.
They run by default together with unit tests.
If Docker isn't available you can skip them:

```bash
mvn \
  -DskipITs=true \
  -Ddocker.tests.exclude='**/io/kontur/eventapi/pdc/composition/PdcEpisodeCompositionTest.java' \
  test
```

The -DskipITs=true flag disables integration tests run by the Maven Failsafe plugin (e.g., classes matching *IT.java).
Unit tests always run regardless of Docker availability.

Maven honours proxy variables.
The JVM arguments from `.mvn/jvm.config` enable usage of `HTTP_PROXY` and `HTTPS_PROXY`.
The repository also ships `.mvn/settings.xml` that points to Maven Central, avoiding custom mirrors when running CI.
