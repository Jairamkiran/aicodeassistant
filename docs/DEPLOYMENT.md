# Deployment Guide

This guide covers running the AI Software Engineering Assistant locally with
Docker Compose and deploying it to Kubernetes. Configuration keys are documented
in [`CONFIGURATION.md`](CONFIGURATION.md).

## Deployables

| Component | Image | Port | Role |
| --- | --- | --- | --- |
| `app` | `app/Dockerfile` | 8080 | Modular monolith: the synchronous REST + SSE API. |
| `indexer-worker` | `indexer-worker/Dockerfile` | 8081 | Headless Kafka-driven indexing saga. |
| `frontend` | `frontend/Dockerfile` | 8080 (nginx) | Static SPA + `/api` reverse proxy. |

Backing services: PostgreSQL (with pgvector), Redis, Kafka (KRaft), Ollama, and
the observability stack (Prometheus, Grafana, Loki, Tempo).

## Local — Docker Compose

```bash
# 1. Infrastructure + observability only:
docker compose up -d

# 2. Everything, apps included (builds the three app images):
docker compose --profile apps up -d --build
```

Once up:

| URL | What |
| --- | --- |
| http://localhost:3001 | Web UI (nginx → app) |
| http://localhost:8080/swagger-ui.html | API docs (Swagger UI) |
| http://localhost:8080/actuator/health | App health |
| http://localhost:3000 | Grafana (anonymous viewer) |
| http://localhost:9090 | Prometheus |

The compose `frontend` service proxies `/api` to the `app` service, so the whole
product is reachable from a single origin at :3001.

## Production — Kubernetes

Manifests live in [`k8s/`](../k8s/) (see its README). They assume the backing
infrastructure is provided separately (managed services or other charts) and
reachable at the service names in `k8s/configmap.yaml`.

```bash
kubectl apply -f k8s/namespace.yaml
cp k8s/secret.example.yaml k8s/secret.yaml   # fill in real values first
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/worker-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/ingress.yaml
```

Highlights of the manifests:

- **`SPRING_PROFILES_ACTIVE=prod`** (via config map) activates
  `application-prod.yml`: locked-down actuator, hidden health details, Secure
  refresh cookie, graceful shutdown, tuned pools/threads.
- **Probes**: startup/liveness/readiness use the Spring Boot actuator
  liveness/readiness groups.
- **Security context**: non-root, dropped Linux capabilities, read-only root
  filesystem (app), seccomp `RuntimeDefault`.
- **Autoscaling**: HPAs on `app` and `frontend` (CPU target).
- **Ingress**: `/api` → app, `/` → frontend, with proxy buffering off so SSE
  chat streams correctly.

### Images

CI builds all three images. Tag/push them to your registry and set the image
references in the deployments (defaults: `ghcr.io/jairamkiran/aicodeassistant-*`).

## Migrations

Flyway runs automatically on `app` startup (`V1`–`V12`). The worker validates
the app-owned schema (it does not migrate). Never edit an applied migration —
add a new one.

## Health & readiness

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness` (fails until DB/Kafka/Redis are up)
- Metrics scrape: `/actuator/prometheus`

## CI/CD

`.github/workflows/ci.yml` runs on every push/PR: backend build + unit tests +
Spotless/Checkstyle/PMD/modularity, the frontend job (install/lint/test/build),
the Testcontainers integration tests, and all three image builds.
