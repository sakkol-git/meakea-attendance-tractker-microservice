# Docker Compose and container orchestration

> **Note:** Docker Compose orchestrates multi-container applications locally and on self-hosted servers, defining services, private networks, persistent storage volumes, and restart policies in a single declarative YAML manifest.

## Purpose and core concept

Docker Compose simplifies multi-container lifecycle management. Instead of running complex, multi-argument `docker run` commands manually, developers define dependencies in a `compose.yaml` file and control the entire application stack using unified commands (`docker compose up -d`, `docker compose down`).

In HRFlow, Docker Compose serves as both the local development orchestration tool and the production self-hosted deployment runtime.

## Problem being solved

Deploying multi-tier software without container orchestration creates severe reproducibility and operational failures:

- Manual dependency installation: Requiring operators to manually install PostgreSQL 16, Caddy, and Uptime Kuma on host machines leads to version drift, conflicting dependencies, and brittle deployments.
- Exposed database ports: Running uncontained databases on local ports often binds them to `0.0.0.0`, exposing internal database credentials to local networks and public internet interfaces.
- Uncontrolled startup order: If an application container starts before its database is healthy, the application crashes on startup.

## Simple mental model

Docker Compose is like an architectural blueprint and master switchboard for a building. Instead of hiring independent plumbers, electricians, and carpenters to wire individual appliances one by one, the blueprint connects all utilities to a central control panel.

Flipping the master switch powers on all systems in the correct sequence, connects their water lines and power cables through internal conduits, and isolates hazardous equipment inside locked rooms.

## How it works

Compose manages container topologies through declarative service definitions:

1. Service specification: Each container is declared as a service with an image or build context, environment variables, exposed ports, and volume mounts.
1. Private network isolation: Compose automatically creates a private bridge network (e.g., `hrflow_default`). Containers resolve each other using service names (such as `postgres` or `app`) as DNS hostnames.
1. Persistent volumes: Named volumes decouple database data files from the container lifecycle, ensuring that stopping or upgrading containers never deletes database state.
1. Health checks and dependencies: Health checks (`test: ["CMD-SHELL", "pg_isready"]`) verify operational readiness; `depends_on: { condition: service_healthy }` ensures downstream services start only when dependencies are fully ready.

## Why HRFlow needs it

HRFlow targets private self-hosted servers with constrained memory (16 GiB) and storage:

- Low operational overhead: Compose eliminates the steep learning curve, cluster management overhead, and resource footprint of Kubernetes.
- Clean isolation: PostgreSQL ports remain private on the internal Docker network; only Caddy exposes HTTP/HTTPS ports to the host.
- Single command management: Operators can pull updated images, restart unhealthy services, and inspect aggregated logs using standard Compose commands.

## Minimal example

A robust, production-ready Compose manifest:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: hrflow-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: hrflow
      POSTGRES_USER: hrflow
      POSTGRES_PASSWORD: ${DB_PASSWORD:?Database password required}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U hrflow -d hrflow"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - internal-net

  app:
    image: hrflow/identity-org:REVIEWED_VERSION
    container_name: identity-org
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/hrflow
      SPRING_DATASOURCE_USERNAME: hrflow
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    networks:
      - internal-net

volumes:
  postgres_data:

networks:
  internal-net:
```

## Application-specific implementation

Production Compose topology with Caddy reverse proxy:

```yaml
services:
  caddy:
    image: caddy:2.8-alpine
    container_name: hrflow-caddy
    restart: unless-stopped
    ports:
      - "127.0.0.1:8080:80"
    volumes:
      - ./infra/caddy/Caddyfile:/etc/caddy/Caddyfile:ro
    depends_on:
      - app
    networks:
      - internal-net

  app:
    build:
      context: .
      dockerfile: services/identity-org/Dockerfile
    container_name: identity-org
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: selfhost
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: hrflow
    networks:
      - internal-net

  postgres:
    image: postgres:16-alpine
    container_name: hrflow-postgres
    restart: unless-stopped
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - internal-net

volumes:
  postgres_data:

networks:
  internal-net:
```

## Common mistakes and pitfalls

Avoid these dangerous operational mistakes:

> **Pitfall:** Exposing database ports publicly using `ports: ["5432:5432"]`. This binds PostgreSQL to all network interfaces, exposing database authentication to internet scanners. If local host access is required, bind explicitly to loopback: `ports: ["127.0.0.1:5432:5432"]`.

- Running `docker compose down -v`: The `-v` flag deletes named persistent volumes, permanently erasing all database tables, employee records, and audit history. Never use `-v` in production.
- Storing unencrypted secrets in Compose files: Hardcoding passwords or JWT secrets inside `compose.yaml` exposes credentials in version control. Always use environment variables or a protected `.env` file.
- Missing log rotation limits: Unbounded container stdout logging can fill the host hard drive over months. Always configure `logging.options.max-size` and `max-file`.

## Testing and debugging

Validation commands:

- Syntax validation: Run `docker compose -f infra/docker/docker-compose.yaml config --quiet` to verify YAML syntax and environment variable interpolation without starting containers.
- Container health check: Run `docker compose ps` to inspect container states, restart counts, and health status indicators.
- Log inspection: Run `docker compose logs -f --tail=100 app` to observe application startup logs and Flyway migration progress.

## Trade-offs and alternatives

Container orchestration comparison:

- Docker Compose (HRFlow choice): Lightweight, single configuration file, low memory footprint, simple to operate for small teams.
- Kubernetes (K8s / K3s): Automated horizontal pod autoscaling, rolling zero-downtime updates across multiple physical nodes; requires dedicated DevOps engineers and high memory overhead.
- Bare-metal systemd services: Lowest memory usage; lacks containerized dependency isolation and complicates environment replication.

## When not to use it

Do not use Docker Compose for:

- Multi-node physical clusters: Compose runs on a single host machine and cannot schedule containers across multiple physical servers.
- Dynamic autoscaling: Compose does not automatically scale container counts based on real-time CPU or traffic spikes.

## Related documentation

- [Reverse proxy and HTTPS primer](REVERSE-PROXY-HTTPS.md)
- [Backups and restores primer](BACKUPS-RESTORES.md)
- [Week 01 foundation build guide](../01-FOUNDATION/BUILD-GUIDE.md)
- [Operations host runbook](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md)
