# Week 1 Evidence

## W01-S01

- Baseline commit: BASELINE_ID (replace with your real ID)
- Command: git status --short
- Result: baseline saved, no generated files or secrets staged

## W01-S02

- Command: df -h .
- Result: AVAILABLE_SPACE free ( 41 GiB). **PASS**

## W01-S03

- Command: java -version
- Result: openjdk version "21.0.13.0.2" 2026-10-22 **PASS**

## W01-S04

- Command: docker info
- Result: Server responded. Memory: 7.748GiB. No containers started.

## W01-S05

- mvn -o validate: exit 0, 10 projects, BUILD SUCCESS
- mvn -o compile: exit 0, 10 projects, BUILD SUCCESS, javac release 21
- Boot version root: 4.1.1, services: inherit 4.1.1 (no mismatch found)
- Spring Cloud: 025.1.3 (api-gateway only), JUnit: 6.0.3:test
- Smallest next fix: confirm the Spring Cloud release train supports Boot 4.1.1 before adding more gateway dependencies.

## W01-S06

- Command: `mvn -f services/identity-org/pom.xml test-compile`
- Result: Spring Data JPA baseline, DiagnosticCheck entity and Flyway migration V1 baseline compiled successfully. **PASS**

## W01-S07

- Command: `mvn -f services/api-gateway/pom.xml test`
- Result: Reactive Spring Cloud Gateway loads, 5/5 tests pass in 3.064 s (ApiGatewayApplicationTest, SecurityConfigurationTests). No downstream business routes exposed. **PASS**

## W01-S17

- Command: `mvn -f services/api-gateway/pom.xml test`
- Result: SecurityProblems and AccessTokenDecoder verify RFC 7807 problem details for unauthorized/forbidden responses without leaking stack traces. **PASS**
