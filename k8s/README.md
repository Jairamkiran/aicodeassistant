# Kubernetes manifests

Plain Kubernetes manifests for deploying the AI Software Engineering Assistant.
They assume the backing infrastructure (PostgreSQL + pgvector, Redis, Kafka,
Ollama, and the observability stack) is provided by managed services or separate
charts and reachable at the service names in `configmap.yaml`.

## Contents

| File | Purpose |
| --- | --- |
| `namespace.yaml` | The `aicodeassistant` namespace. |
| `configmap.yaml` | Non-secret configuration (URLs, profile). |
| `secret.example.yaml` | Template for the required secrets — **copy, fill, do not commit**. |
| `app-deployment.yaml` | Monolith `app` Deployment + Service (2 replicas, probes, non-root). |
| `worker-deployment.yaml` | `indexer-worker` Deployment (headless). |
| `frontend-deployment.yaml` | nginx-served SPA Deployment + Service. |
| `hpa.yaml` | Horizontal Pod Autoscalers for `app` and `frontend`. |
| `ingress.yaml` | Ingress routing `/api` → app, `/` → frontend (SSE-friendly, TLS). |

## Apply

```bash
kubectl apply -f k8s/namespace.yaml
cp k8s/secret.example.yaml k8s/secret.yaml   # then fill in real values
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/worker-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/ingress.yaml
```

Images default to `ghcr.io/jairamkiran/aicodeassistant-*:latest` (built by CI);
override with your registry/tag. Health probes use the Spring Boot
liveness/readiness actuator groups already enabled in the app.
