#!/usr/bin/env python3
"""Check the small Maven dependency policy shared by HRFlow services."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAVEN = {'m': 'http://maven.apache.org/POM/4.0.0'}
OWNER_SERVICES = ('identity-org', 'attendance', 'leave', 'request', 'notification', 'reporting')

OWNER_RUNTIME = {
    'org.springframework.boot:spring-boot-starter-actuator',
    'org.springframework.boot:spring-boot-starter-security',
    'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server',
    'org.springframework.boot:spring-boot-starter-data-jpa',
    'org.springframework.boot:spring-boot-starter-flyway',
    'org.springframework.boot:spring-boot-starter-kafka',
    'org.springframework.boot:spring-boot-starter-validation',
    'org.springframework.boot:spring-boot-starter-webmvc',
    'org.flywaydb:flyway-database-postgresql',
    'org.postgresql:postgresql',
    'io.confluent:kafka-avro-serializer',
    'io.micrometer:micrometer-registry-prometheus',
    'org.projectlombok:lombok',
    'org.mapstruct:mapstruct',
}
OWNER_TEST = {
    'org.springframework.boot:spring-boot-starter-test',
    'org.springframework.boot:spring-boot-starter-data-jpa-test',
    'org.springframework.boot:spring-boot-starter-security-test',
    'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test',
    'org.springframework.boot:spring-boot-starter-webmvc-test',
    'org.springframework.boot:spring-boot-testcontainers',
    'org.testcontainers:testcontainers-postgresql',
    'org.testcontainers:testcontainers-kafka',
    'org.testcontainers:testcontainers-junit-jupiter',
    'com.tngtech.archunit:archunit',
}
GATEWAY_REQUIRED = {
    'org.springframework.boot:spring-boot-starter-actuator',
    'org.springframework.boot:spring-boot-starter-security',
    'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server',
    'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux',
    'io.micrometer:micrometer-registry-prometheus',
}
GATEWAY_FORBIDDEN = {
    'org.springframework.boot:spring-boot-starter-data-jpa',
    'org.springframework.boot:spring-boot-starter-flyway',
    'org.postgresql:postgresql',
}
DEPRECATED = 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'


def read_pom(path: Path) -> ET.Element:
    try:
        return ET.parse(path).getroot()
    except (OSError, ET.ParseError) as error:
        raise ValueError(f'{path.relative_to(ROOT)}: cannot read valid XML: {error}') from error


def dependency_ids(project: ET.Element) -> set[str]:
    dependencies = project.findall('./m:dependencies/m:dependency', MAVEN)
    return {
        f"{dependency.findtext('m:groupId', namespaces=MAVEN)}:"
        f"{dependency.findtext('m:artifactId', namespaces=MAVEN)}"
        for dependency in dependencies
    }


def parent_artifact(project: ET.Element) -> str | None:
    return project.findtext('./m:parent/m:artifactId', namespaces=MAVEN)


def module_paths(project: ET.Element) -> set[str]:
    return {module.text or '' for module in project.findall('./m:modules/m:module', MAVEN)}


def missing(required: set[str], actual: set[str]) -> list[str]:
    return sorted(required - actual)


def main() -> int:
    errors: list[str] = []
    owner_parent_path = ROOT / 'services/owner-service-parent/pom.xml'
    owner_parent = read_pom(owner_parent_path)
    owner_dependencies = dependency_ids(owner_parent)

    for dependency in missing(OWNER_RUNTIME | OWNER_TEST, owner_dependencies):
        errors.append(f'services/owner-service-parent/pom.xml: missing {dependency}')

    root_project = read_pom(ROOT / 'pom.xml')
    if 'services/owner-service-parent' not in module_paths(root_project):
        errors.append('pom.xml: missing services/owner-service-parent module')
    gateway_path = ROOT / 'services/api-gateway/pom.xml'
    gateway_dependencies = dependency_ids(read_pom(gateway_path))
    for dependency in missing(GATEWAY_REQUIRED, gateway_dependencies):
        errors.append(f'services/api-gateway/pom.xml: missing {dependency}')
    for dependency in sorted(GATEWAY_FORBIDDEN & gateway_dependencies):
        errors.append(f'services/api-gateway/pom.xml: gateway must not depend on {dependency}')

    for service in OWNER_SERVICES:
        path = ROOT / f'services/{service}/pom.xml'
        project = read_pom(path)
        if parent_artifact(project) != 'owner-service-parent':
            errors.append(f'services/{service}/pom.xml: parent must be owner-service-parent')

    processor_paths = owner_parent.findall(
        './m:build/m:plugins/m:plugin[m:artifactId="maven-compiler-plugin"]/'
        'm:configuration/m:annotationProcessorPaths/m:path', MAVEN,
    )
    processors = {
        f"{path.findtext('m:groupId', namespaces=MAVEN)}:"
        f"{path.findtext('m:artifactId', namespaces=MAVEN)}"
        for path in processor_paths
    }
    for processor in missing({
        'org.projectlombok:lombok',
        'org.projectlombok:lombok-mapstruct-binding',
        'org.mapstruct:mapstruct-processor',
    }, processors):
        errors.append(f'services/owner-service-parent/pom.xml: missing annotation processor {processor}')

    reporting_dependencies = dependency_ids(read_pom(ROOT / 'services/reporting/pom.xml'))
    if 'org.apache.commons:commons-csv' not in reporting_dependencies:
        errors.append('services/reporting/pom.xml: missing org.apache.commons:commons-csv')

    all_poms = list(ROOT.glob('pom.xml')) + list((ROOT / 'services').glob('*/pom.xml'))
    all_poms += list((ROOT / 'libs').glob('*/pom.xml'))
    for path in all_poms:
        dependencies = dependency_ids(read_pom(path))
        if DEPRECATED in dependencies:
            errors.append(f'{path.relative_to(ROOT)}: replace deprecated {DEPRECATED}')

    if errors:
        print('Maven dependency policy failed:', file=sys.stderr)
        for error in errors:
            print(f'- {error}', file=sys.stderr)
        return 1

    print('Maven dependency policy passed for the gateway and six owner services.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
