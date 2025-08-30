# Running tests

Run all unit and integration tests with Maven's verify phase:

```bash
mvn verify
```

Most tests spin up PostgreSQL and Redis containers using Testcontainers.
If Docker isn't available you can skip integration tests:

```bash
mvn verify -DskipITs=true
```

Run only unit tests with:

```bash
mvn test
```

Unit tests always run regardless of Docker availability.

Note on Makefile:
`make test` runs `mvn test -DskipITs=true` (unit tests only).
`make verify` runs `mvn verify` (integration tests included).
To skip integration tests when using the Makefile:

```bash
make verify MAVEN_ARGS=-DskipITs=true
```

Maven honors `HTTP_PROXY` and `HTTPS_PROXY` variables.
The Makefile sets JVM options (`-Djava.net.useSystemProxies=true`, `-Djava.net.preferIPv4Stack=true`, `-Djava.net.preferIPv6Addresses=false`) so Maven respects those proxies without a `.mvn/jvm.config` file.
Make targets append proxy command-line arguments when these variables are set, avoiding global configuration changes.
