# Running tests

All unit and integration tests run with Maven by default:

```bash
mvn test
```

Integration tests rely on PostgreSQL and Redis containers.
They run by default together with unit tests.
If Docker isn't available you may skip them:

```bash
mvn -DskipITs=true -Ddocker.tests.exclude='**/PdcEpisodeCompositionTest.*' test
```

Unit tests always run regardless of Docker availability.

Maven honours proxy variables. The JVM arguments from `.mvn/jvm.config` enable usage of `HTTP_PROXY` and `HTTPS_PROXY`.
The repository also ships `.mvn/settings.xml` that points to Maven Central, avoiding custom mirrors when running CI.
