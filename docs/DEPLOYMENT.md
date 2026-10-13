# Deployment

## Docker image

The `Dockerfile` builds in two stages:

1. **Build stage** (`maven:3.9-eclipse-temurin-21`) runs `mvn clean package`.
2. **Runtime stage** (`openjdk:21-ea-21-jdk-slim`) copies the jar and runs `java -jar app.jar` on port 8081.

```bash
docker build -t mybudgetappserver .
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/mybudgetapp \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=... \
  mybudgetappserver
```

## Docker Compose (local full stack)

`docker-compose.yml` starts:

| Service    | Image                  | Port        |
| ---------- | ---------------------- | ----------- |
| `postgres` | `postgres:17`          | 5432        |
| `backend`  | `mybudgetappserver`    | 8081        |
| `frontend` | `mybudgetappfrontend`  | 5173 → 80   |

Compose doesn't build the backend or frontend images, so build them first. Postgres data is kept in the `postgres-data` volume.

## CI/CD (GitLab)

`.gitlab-ci.yml` runs on every push to `main`:

| Stage    | Job              | What it does |
| -------- | ---------------- | ------------ |
| `build`  | `build-backend`  | Builds the Docker image (Docker-in-Docker) and pushes `$CI_REGISTRY_IMAGE/mybudgetapp_server:latest` to the GitLab Container Registry |
| `deploy` | `deploy-backend` | SSHes to the EC2 host as `ubuntu` and runs `kubectl set image deployment/backend -n mybudgetapp backend=<image>` |

Required CI/CD variables:

| Variable          | Purpose                                         |
| ----------------- | ----------------------------------------------- |
| `DEPLOY_SSH_KEY`  | Private key for SSH into the EC2 host           |
| `EC2_HOST`        | Hostname or IP of the EC2 host running Kubernetes |

`CI_REGISTRY*` variables are provided by GitLab automatically.

Because the image tag is always `latest`, `set image` may leave the pod spec unchanged. If pods don't restart after a deploy, run `kubectl -n mybudgetapp rollout restart deployment/backend`.

## Kubernetes (`devsecops/`)

| File                          | Resources |
| ----------------------------- | --------- |
| `namespace.yaml`              | Namespace `mybudgetapp` |
| `postgres.yaml`               | Secret `postgresmybudgetapp-secret`, a PVC, and a Postgres StatefulSet with its Service |
| `postgresmybudgetapp-pvc.yaml`| A standalone PVC for Postgres |
| `mybudgetapp.yaml`            | Backend Deployment and Service `backend`, connected to the in-cluster Postgres through `postgresmybudgetapp-secret` |
| `backend.yaml`                | Alternative backend Deployment, connected to an AWS RDS instance; reads its password from Secret `db-credentials`. It has no namespace, so it deploys to the current namespace |
| `frontend.yaml`               | Frontend Deployment and Service; reads `API_URL` from the ConfigMap |
| `configmap.yaml`              | ConfigMap `frontend-config` (`API_URL`) |
| `ingress.yaml`                | NGINX Ingress for host `mybudgetapp.local`: `/api` → backend:8081, `/` → frontend:80 |
| `ingress_web.yaml`            | Traefik Ingress `app-ingress` (any host, `web` entrypoint) with the same routes. Use this on k3s, which ships Traefik |

Use **either** `mybudgetapp.yaml` (Postgres inside the cluster) **or** `backend.yaml` (RDS). Both create a Deployment named `backend`.

### First-time setup

```bash
kubectl apply -f devsecops/namespace.yaml
kubectl apply -f devsecops/postgres.yaml          # skip if using RDS
kubectl -n mybudgetapp create secret docker-registry gitlab-registry-secret \
  --docker-server=registry.gitlab.com --docker-username=<user> --docker-password=<token>
kubectl apply -f devsecops/mybudgetapp.yaml       # or backend.yaml for RDS
kubectl apply -f devsecops/configmap.yaml -f devsecops/frontend.yaml
kubectl apply -f devsecops/ingress.yaml
```

Pods pull images from the private GitLab registry, which needs the `gitlab-registry-secret` image pull secret (and `gitlab-registry-frontend-secret` for the frontend).

Flyway runs migrations automatically when the backend starts, so the database needs no manual setup beyond creating it.

> 🔒 `postgres.yaml`, `application.properties` and `docker-compose.yml` contain a hard-coded database password. For production, move it into a Kubernetes Secret created outside the repo, and rotate it.
