CREATE TABLE IF NOT EXISTS outbox_events (
  id UUID PRIMARY KEY,
  aggregate_id UUID NOT NULL,
  type VARCHAR(100) NOT NULL,          -- "TaskCreated" | "TaskUpdated" | "TaskDeleted"
  payload TEXT NOT NULL,               -- JSON
  occurred_at VARCHAR(64) NOT NULL,    -- timestamp
  published BOOLEAN NOT NULL DEFAULT FALSE,
  published_at VARCHAR(64) NULL
);
sn