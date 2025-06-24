# Running tests

All unit and integration tests run with Maven by default:

```bash
mvn test
```

Tests needing PostgreSQL and Redis require Docker. They run by default, but you can skip them when Docker isn't available:

```bash
mvn -DskipITs=true -Ddocker.tests.exclude='**/PdcEpisodeCompositionTest.*' test
```

Unit tests always run regardless of Docker availability.

Maven honours proxy variables. The JVM arguments from `.mvn/jvm.config` enable usage of `HTTP_PROXY` and `HTTPS_PROXY`.
The repository also ships `.mvn/settings.xml` that points to Maven Central, avoiding custom mirrors when running CI.
