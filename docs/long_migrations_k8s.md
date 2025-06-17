# Running Long Migrations in Kubernetes

The service uses Liquibase to apply database migrations on startup. When a migration takes longer than about ten seconds the container may be restarted by Kubernetes because the main process has not started yet. Two common approaches to avoid this issue are:

## 1. Init container

Define an init container that runs `java -jar event-api.jar --spring.liquibase.enabled=true` (or a dedicated migration command) before the main application container starts. The init container has its own image and resources and completes before the main container is launched. This way Kubernetes waits for the migrations to finish.

Example snippet:

```yaml
initContainers:
  - name: db-migrate
    image: registry.example.com/event-api:latest
    command: ["java","-jar","event-api.jar","--spring.liquibase.enabled=true","--exit"]
```

## 2. Helm pre-install/pre-upgrade hook

Create a separate Job template annotated with `helm.sh/hook: pre-install,pre-upgrade`. Helm will run it before deploying or upgrading the main release. The Job pod can run the same migration command without affecting the main deployment.

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: event-api-migrate
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
spec:
  template:
    spec:
      restartPolicy: OnFailure
      containers:
        - name: migrate
          image: registry.example.com/event-api:latest
          command: ["java","-jar","event-api.jar","--spring.liquibase.enabled=true","--exit"]
```

Both approaches allow the migration to run to completion without the deployment being restarted. Choose init containers when the migrations must run each time the pod starts. Use Helm hooks when migrations should run only during chart upgrades.
