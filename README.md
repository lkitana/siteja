# Si-Teja (Sistem Tebak Tokoh) — Backend

Spring Boot backend for the Si-Teja guessing game. All candidate/question/trait
data lives in MySQL (via JPA/Hibernate) — nothing is hardcoded in Java. The
Shannon Entropy and Bayes' Theorem calculations live entirely in
`TejaEngineService`.

## 1. Create the database

```sql
CREATE DATABASE siteja CHARACTER SET utf8mb4;
```

## 2. Configure connection (optional — defaults work for local MySQL on root/no-password)

Set these environment variables if your setup differs from the defaults:

| Variable      | Default     |
|---------------|-------------|
| `DB_HOST`     | `localhost` |
| `DB_PORT`     | `3306`      |
| `DB_NAME`     | `siteja`    |
| `DB_USER`     | `root`      |
| `DB_PASSWORD` | *(empty)*   |

## 3. Run

```bash
mvn spring-boot:run
```

On first startup, Flyway automatically runs the migrations in
`src/main/resources/db/migration/`:

- `V1__create_schema.sql` — creates `karakter`, `question`, `karakter_trait` tables
- `V2__seed_karakter_data.sql` — seeds 5 candidate characters and 5 trait
  questions with curated probabilities

No Java recompilation is needed to add, remove, or re-balance characters and
questions — just add a new Flyway migration (e.g. `V3__add_more_karakter.sql`)
with the relevant `INSERT` statements.

## 4. Test

```bash
mvn test
```

Tests run against an in-memory H2 database (MySQL-compatibility mode), not
your real MySQL instance — see `src/test/resources/application-test.properties`.

## API

| Method | Path                          | Description                          |
|--------|-------------------------------|---------------------------------------|
| POST   | `/api/teja/start`             | Start a new session                   |
| GET    | `/api/teja/next-question`     | Get the next best question (Entropy)  |
| POST   | `/api/teja/answer`            | Submit an answer (Bayes update)       |
| GET    | `/api/teja/top-candidates`    | Poll current standings                |
