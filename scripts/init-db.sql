-- Auth Service – DB bootstrap (idempotent).
-- Hibernate (ddl-auto=update) creates tables automatically.
-- This script only ensures the schema and extension exist.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- users and user_roles tables are created/managed by Hibernate.
-- Nothing else required for a fresh auth_db.
