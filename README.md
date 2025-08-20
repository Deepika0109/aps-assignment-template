# APS Assignment – CQRS + Event-Driven + OpenSearch

This project extends the basic ClickUp-like tasks service with:
- **CQRS**: writes go to Postgres (command/write model), reads use **OpenSearch** (query/read model).
- **Event-driven** propagation via **RabbitMQ**.
- **Transactional Outbox** in the write service for reliable event publishing.

## Modules
- `library`: shared DTOs & domain events
- `primary-app`: Ktor HTTP API (create/update/delete + GET). Persists to Postgres, enqueues events into the Outbox table and publishes to RabbitMQ in background.
- `secondary-app`: Event consumer. Updates OpenSearch read model on task events.

## Endpoints (primary-app)
- `POST /api/v2/task` – create
- `GET /api/v2/task/{id}` – **reads from OpenSearch first**, falls back to Postgres if not indexed yet
- `PUT /api/v2/task/{id}` – update
- `DELETE /api/v2/task/{id}` – delete

Matches the assignment spec for fields: `name`, `description`, `due_date`, `assignees`, `created_at`.

## Run (Docker Compose)
```bash
docker compose up --build
```
- Postgres: `localhost:5432` (postgres/postgres)
- RabbitMQ: `localhost:15672` (guest/guest)
- OpenSearch: `localhost:9200`
- Dashboards: `localhost:5601`
- primary-app: `localhost:8080`
- secondary-app: `localhost:8081`

## Local Dev
- Java 17, Kotlin 1.9.x, Ktor 2.3.x
- `./gradlew :primary-app:run` and `./gradlew :secondary-app:run`

## Design Notes
- **Outbox** table ensures events are saved atomically with DB write; a background job publishes them to RabbitMQ, marking as published on success.
- **Idempotency**: consumer upserts documents in OpenSearch; delete events remove them from the index.
- **Fallback**: `GET` will return from Postgres if OpenSearch is not yet caught up.

## Caveats
- Simplified error handling & mapping.
- Assignees stored as JSON in a text column for brevity.
- OpenSearch mapping kept minimal.
