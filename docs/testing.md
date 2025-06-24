# Running tests

All unit and integration tests run with Maven by default:

```bash
mvn test
```

Tests needing PostgreSQL and Redis require Docker. They run by default, but you can skip them when Docker isn't available:

```bash
mvn -DskipITs=true -Ddocker.tests.exclude='**/pdc/composition/PdcEpisodeCompositionTest.java' test
```

Unit tests always run regardless of Docker availability.

The build honors system proxy settings. Maven automatically picks up `HTTP_PROXY` and `HTTPS_PROXY` environment variables. The JVM arguments from `.mvn/jvm.config` enable usage of these settings during tests.
