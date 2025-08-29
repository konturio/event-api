# Running tests

Run unit tests:

```bash
mvn test
```

Run unit + integration tests:

```bash
mvn verify
```

Integration tests spin up PostgreSQL and Redis containers using Testcontainers.
If Docker isn't available, you can skip them:

```bash
mvn -DskipITs=true \
    -Ddocker.tests.exclude='**/io/kontur/eventapi/pdc/composition/PdcEpisodeCompositionTest.java' \
    test
```

Unit tests always run regardless of Docker availability.
The `-DskipITs=true` flag disables integration tests run by the Maven Failsafe plugin (e.g., classes matching `*IT.java`).

Maven honors proxy variables.
JVM arguments in `.mvn/jvm.config` enable using `HTTP_PROXY` and `HTTPS_PROXY`.
`.mvn/settings.xml` points to Maven Central, avoiding custom mirrors in CI.
