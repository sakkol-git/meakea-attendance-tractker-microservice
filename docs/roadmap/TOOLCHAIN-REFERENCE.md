# Toolchain evidence and version lock

## Observed source and installed tools

| Item | Observed value | Evidence | Execution requirement |
| --- | --- | --- | --- |
| Java target | 21 | pom.xml and service poms | Active local Java 17 must be replaced/selected before compiling |
| Maven | 3.9.16 | mvn --version | Use root reactor/service -pl; preserve wrapper if adopted |
| Boot | Root/services inherit 4.1.1 | Actual poms | Baseline aligned; real owner/Java-21/integration startup still required |
| Cloud | 2025.1.3 scaffold | api-gateway pom | Official matrix supports Boot 4.1.x from 2025.1.2; retain 2025.1.3 and verify reactive startup |
| Avro | 1.11.4 | Root property | Fix actual schema sourceDirectory before generation |
| Web | Nuxt ^4.6.1 manifest plus package-lock | web/package.json | npm ci uses lock, not an unreviewed upgrade |
| Postman CLI | 1.75.0 | postman --version | Optional local collection execution; no login/account assumed |
| Node/npm | 26.3.0 / 11.16.0 | node/npm --version | Check engines of selected Nuxt/validator/test tools first |

## Future lock procedure

Week 1 records the actual compatible Maven plugins, actions/image digests and local Redocly/Prism/Vitest pins in proposed docs/validation/TOOLCHAIN-LOCK.md and matching lockfiles. Inspect official compatibility/release information, install an exact reviewed version, run its help/check, record checksum/version and commit the lock. Unknown pins are not presented as installed or verified. Scanner/network/runner unavailability blocks its gate, not a successful scan.

The cached spring-boot-dependencies/4.1.1 POM resolves Jupiter 6.0.3, Spring Framework 7.0.9 and Testcontainers 2.0.5; these are dependency-management values, not proof all artifacts/tests are installed. Use the Boot-managed engine, not a separately forced JUnit 5 dependency. Testcontainers 2 uses testcontainers-postgresql and org.testcontainers.postgresql.PostgreSQLContainer without the old generic parameter. [Boot testing](https://docs.spring.io/spring-boot/reference/testing/index.html), [Cloud compatibility](https://spring.io/projects/spring-cloud/), [reactive Gateway starter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/starter.html), [JdbcClient reference](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html) and [Redocly lint](https://redocly.com/docs/cli/commands/lint) were checked during this documentation pass. Installed versions remain subject to the explicit local spike; this task does not change application dependencies.

## Exact commands after their creation steps

```bash
npm --prefix docs/validation ci
docs/validation/node_modules/.bin/redocly lint contracts/openapi.yaml
docs/validation/node_modules/.bin/prism mock contracts/openapi.yaml
mvn -pl services/identity-org -am test
mvn spotless:check pmd:check
npm --prefix web run test -- --run
```

The Nuxt test script is added with its first feature fixture using the locked compatible runner. It is absent in the audited scaffold and therefore is not run or claimed to exist today. One Maven test command uses actual selected class/method from inspection as documented in DAILY-COMMANDS. A grep/config/syntax check is not a domain assertion or a cloud deployment.

## Service baseline update

[Service configuration](../SERVICE-CONFIGURATION.md) records the current parent/JDBC/reactive alignment and environment setup. Older audits capture pre-alignment source, not instructions to undo this baseline. The schema generator now reads libs/hrflow-contracts/events. Configuration/security slice tests do not replace the planned real PostgreSQL/Kafka and Java-21 gates.
