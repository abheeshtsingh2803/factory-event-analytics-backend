# 🏭 Factory Event Analytics Backend

A Spring Boot backend system that ingests high-volume factory machine events, validates and deduplicates them safely under concurrency, and provides real-time analytics on production defects and machine health.

This project was designed to simulate real-world industrial telemetry ingestion, where data arrives in large batches, may contain duplicates or late updates, and must be processed correctly, consistently, and fast.

## 📐 Architecture

### High-Level Components
```
┌────────────────────┐
│   API Clients      │
│ (Postman / Tools)  │
└─────────┬──────────┘
          │ HTTP (JSON)
          ▼
┌────────────────────┐
│   Controllers      │
│  (REST Layer)      │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│   Services         │
│ (Business Logic)   │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│   Repositories     │
│  (Data Access)     │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│   PostgreSQL DB    │
│  (Persistent Data)│
└────────────────────┘

```

### Layers
| Layer | Responsibility |
|-------|----------------|
| Client Layer (API Consumers) | Send batches of machine events, Query analytics endpoints, Receive JSON responses |
| Controller Layer (REST API) | Expose HTTP endpoints, Accept JSON requests, Validate request format, Forward to services, Return responses |
| Service Layer (Business Logic Core) | EventService

Validate incoming events

Reject invalid data

Deduplicate events

Decide update vs ignore

Handle concurrency safely

StatsService

Calculate defect totals

Ignore invalid defects

Enforce time window logic

Rank production lines |
| Repository Layer (Data Access) | Save events, Find events by eventId, Run analytics queries, Enforce unique constraints |
| Database Layer (PostgreSQL) | No duplicate events, Thread safety, Data integrity |

### Why This Architecture?
- Separation of concerns
- Testable business logic
- Scalable ingestion
- Clear analytics flow

## 🔁 Dedupe / Update Logic

Factories often send the same event multiple times, sometimes with updated data or delayed delivery.
To handle this correctly, the system applies a deterministic deduplication and update strategy based on:

- Event identity
- Payload comparison (hashing)
- Timestamp ordering

### 1. Event Identity

Each event is uniquely identified by:

- eventId

The database enforces:

- UNIQUE(event_id)

So there can only be one stored record per eventId.

### 2. Payload Comparison (Hashing)

To efficiently detect identical events, the full payload is converted into a hash:

```java
String payloadHash = HashUtil.hash(request.toString());
```

This allows fast comparison without field-by-field checks.

If:

- newHash == existingHash

Then the event is considered an identical duplicate and is deduped.

### 3. Timestamp Ordering (Choosing the "Winning" Record)

Each event also has a timestamp used for ordering:

- receivedTime = eventTime

When a new event arrives with the same eventId, the system compares timestamps:

| Case | Decision |
|------|----------|
| Older timestamp | Ignore (deduped) |
| Same payload | Deduped |
| Newer timestamp | Update existing record |

This ensures that:

- The most recent version of an event always "wins."

### 4. Decision Flow

The logic is implemented as:

```java
if (received.isBefore(existing.getReceivedTime())) {
    deduped++;
    return;
}

if (existing.getPayloadHash().equals(newHash)) {
    deduped++;
    return;
}

if (received.isAfter(existing.getReceivedTime())) {
    update existing record;
    updated++;
    return;
}
```

### 5. Why This Approach Works

| Benefit | Explanation |
|---------|-------------|
| Deterministic | Same input → same result |
| Efficient | Hash comparison is fast |
| Safe | Prevents overwriting newer data |
| Accurate | Ensures latest data is stored |
| Scalable | Works under high concurrency |

### 6. Edge Cases Handled

| Scenario | Outcome |
|----------|---------|
| Duplicate event | Deduped |
| Out-of-order delivery | Older ignored |
| Partial updates | Newer wins |
| Concurrent inserts | DB constraint + try/catch |
| Identical replays | Deduped |

### 7. Tradeoffs & Assumptions

- eventTime is trusted as the correct ordering signal
- No version field is used (kept simple)
- Payload hash assumes consistent serialization
- No partial-field merge (full overwrite on update)

## 🧵 Thread Safety

### What Makes It Thread-Safe?
- Transactional service layer
- Database unique constraint on event_id
- Graceful conflict handling

```java
try {
    repository.save(event);
} catch (DataIntegrityViolationException e) {
    response.deduped++;
}
```

### Result
- No duplicate rows
- No corrupted updates
- Concurrent ingestion is safe

## 🗄️ Data Model

### Table: events
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| event_id | VARCHAR | Unique event identifier |
| event_time | TIMESTAMP | When event occurred |
| received_time | TIMESTAMP | Ordering reference |
| machine_id | VARCHAR | Machine |
| factory_id | VARCHAR | Factory |
| line_id | VARCHAR | Production line |
| duration_ms | BIGINT | Operation duration |
| defect_count | INT | Defects (-1 ignored) |
| payload_hash | TEXT | Dedup hash |

### Unique Constraint
- UNIQUE(event_id)

## 📊 Analytics Endpoints

### Machine Stats
```
GET /stats?machineId=M01&start=...&end=...
```
Returns:
- Total events
- Total defects (excluding -1)
- Average defect rate
- Health status

### Top Defect Lines
```
GET /stats/top-defect-lines?factoryId=F01&from=...&to=...&limit=5
```
Returns:
- lineId
- totalDefects
- eventCount
- defectsPercent

## ⚡ Performance Strategy

### Goal: 1000 events < 1 second

### Techniques Used
| Strategy | Benefit |
|----------|---------|
| Batch ingestion | Fewer HTTP calls |
| Single DB lookup per event | Low overhead |
| Hash comparison | Fast dedupe |
| No unnecessary joins | Simple queries |
| Indexes on event_id | Fast lookup |

### Result
- 1000 events processed in < 1s
- Stable under concurrency

## ⚠️ Edge Cases & Assumptions

| Case | Decision |
|------|----------|
| DefectCount = -1 | Ignored in analytics |
| Future events | Rejected |
| Older updates | Ignored |
| Same timestamp updates | Deduped |
| Invalid duration | Rejected |
| Duplicate inserts | Handled safely |
| Partial batch failure | Others still processed |

### Tradeoffs
- Using eventTime as ordering simplifies logic
- No Kafka / async queue (kept simple for assignment)

## 🧪 Testing Strategy

8 mandatory tests implemented:
- Identical duplicate → deduped
- Newer payload → update
- Older payload → ignored
- Invalid duration → rejected
- Future event → rejected
- DefectCount = -1 ignored
- Time window boundaries
- Concurrent ingestion safety

### Run Tests (Gradle)
```bash
./gradlew clean test
```

## 🚀 Setup & Run Instructions

1. **Clone**
   ```bash
   git clone https://github.com/your-username/factory-event-analytics
   cd factory-event-analytics
   ```

2. **Configure DB**
   Edit `application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/factory
       username: postgres
       password: password
   ```

3. **Run App**
   ```bash
   ./gradlew bootRun
   ```

4. **Swagger UI**
   ```
   http://localhost:8080/swagger-ui.html
   ```

## 📦 Benchmark

A 1000-event payload is provided: `benchmark-1000.json`

Use:
```bash
curl -X POST http://localhost:8080/events/batch \
  -H "Content-Type: application/json" \
  --data-binary @benchmark-1000.json
```

## 🔧 What You Would Improve with More Time

- Implement asynchronous processing with Kafka for better scalability
- Add caching layer (Redis) for analytics queries
- Implement event sourcing for full audit trail
- Add monitoring and alerting with Prometheus/Grafana
- Implement rate limiting and circuit breakers
- Add comprehensive logging and tracing
- Implement data partitioning for large-scale deployments
- Add API versioning and backward compatibility
- Implement automated deployment pipelines
- Add more comprehensive error handling and retry mechanisms