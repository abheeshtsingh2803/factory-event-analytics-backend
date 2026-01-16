[![Backend: Spring Boot](https://img.shields.io/badge/backend-Spring%20Boot-6DB33F?logo=spring)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/database-PostgreSQL-336791?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/container-Docker-2496ED?logo=docker)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

# 🏭 Factory Event Analytics Backend

A **Spring Boot** backend system that ingests high-volume factory machine events, validates and deduplicates them safely under concurrency, and provides real-time analytics on production defects and machine health.

This project was designed to simulate **real-world industrial telemetry ingestion**, where data arrives in large batches, may contain duplicates or late updates, and must be processed **correctly, consistently, and fast**.          

---

## 📑 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Project Structure](#project-structure)
- [API Design Principles](#api-design-principles)
- [Dedupe / Update Logic](#application-setup) 
- [Thread Safety](#application-setup)
- [Data Model](#application-setup)
- [Performance Strategy](#application-setup)
- [Edge Cases & Assumptions](#application-setup)
- [Application Setup](#application-setup)
- [API Endpoints](#api-endpoints)
- [Future Enhancements](#future-enhancements)
- [License](#license)
---

## 📌 Overview

The Factory Event Analytics Backend is a Spring Boot–based system designed to ingest high-volume machine events from factories, validate and deduplicate them safely under concurrency, and provide real-time analytics on production defects and machine health.

The system simulates real-world industrial telemetry ingestion where:
- Events arrive in large batches
- Duplicates and updates are common
- Data can be invalid or delayed
- Concurrency must not corrupt data
- Analytics must remain accurate

---

## 🏗 Architecture


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

---

## 🧰 Tech Stack

- **Backend:** Spring Boot (Java 17)
- **ORM:** Spring Data JPA (Hibernate)
- **Database:** PostgreSQL
- **Build Tool:** Gradle
- **API Testing:** Postman
- **Logging:** SLF4J + Logback
- **Containerization:** Docker (planned)

---

## ✨ Features

- Batch event ingestion
- Validation of event data
- Deduplication & updates
- Thread-safe processing
- Machine health analytics
- Top defect lines analytics
- Swagger UI
- Performance benchmark (1000 events)
- 8+ mandatory test cases

---

## 📂 Project Structure


```
src/main/java/org/factory
 ├── controller
 ├── service
 ├── repository
 ├── model
 ├── dto
 └── util

src/test/java/org/factory
 ├── service
 └── concurrency
```

---

## 🔌 API Design Principles

- **Versioned APIs**: `/api/v1/...`
- **DTO-based contracts**
- **No direct entity exposure**
- **Centralized exception handling** using `@ControllerAdvice`
- **Proper HTTP status codes**
- **ID-based relationships** instead of name-based lookups
---

## 🔁 Dedupe / Update Logic

Factories often send the same event multiple times, sometimes with updated data or delayed delivery.
To handle this correctly, the system applies a deterministic deduplication and update strategy based on:
1. Event identity
2. Payload comparison (hashing)
3. Timestamp ordering

---

### 1. Event Identity

Each event is uniquely identified by: `eventId`

The database enforces: `UNIQUE(event_id)`

So there can only be one stored record per eventId.

### 2. Payload Comparison (Hashing)

To efficiently detect identical events, the full payload is converted into a hash:
`
String payloadHash = HashUtil.hash(request.toString());
`
This allows fast comparison without field-by-field checks.

If:          `newHash == existingHash`

Then the event is considered an identical duplicate and is deduped.

### 3. Timestamp Ordering (Choosing the “Winning” Record)

Each event also has a timestamp used for ordering:          `receivedTime = eventTime`


When a new event arrives with the same `eventId`, the system compares timestamps:

| Case |	Decision |
| ---- | --------- |
| Older timestamp |	Ignore (deduped) |
| Same payload |	Deduped |
| Newer timestamp |	Update existing record |

This ensures that:           `The most recent version of an event always “wins.”`

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
| ------- | ----------- |
| Deterministic |	Same input → same result |
| Efficient | Hash comparison is fast |
| Safe | Prevents overwriting newer data |
| Accurate | Ensures latest data is stored |
| Scalable | Works under high concurrency |

### 7. Edge Cases Handled
| Scenario | Outcome |
| -------- | ------- |
| Duplicate event |	Deduped |
| Out-of-order delivery | Older ignored |
| Partial updates |	Newer wins |
| Concurrent inserts | DB constraint + try/catch |
| Identical replays |	Deduped |

### 8. Tradeoffs & Assumptions
- eventTime is trusted as the correct ordering signal
- No version field is used (kept simple)
- Payload hash assumes consistent serialization
- No partial-field merge (full overwrite on update)

--- 

## Thread Safety
### How the system avoids corruption under concurrency

Thread safety is ensured through:

1. Database Constraints
```
UNIQUE(event_id)
```
- Prevents duplicate inserts.

2. Transactional Service Layer
``
@Transactional
``
- Ensures atomic operations.

3. Graceful Conflict Handling
```
try {
    repository.save(event);
} catch (DataIntegrityViolationException e) {
    response.deduped++;
}
```
### Result
- No duplicate rows
- No corrupted updates
- Safe concurrent ingestion

---

## Data Model

Table: 'events'

| Column |	Type |	Description |
| ------ | ------------- | -------------- |
| id |	BIGINT |	Primary key |
| event_id |	VARCHAR |	Unique event |
| event_time |	TIMESTAMP |	Event timestamp |
| received_time |	TIMESTAMP |	Ordering
| machine_id |	VARCHAR |	Machine |
| factory_id |	VARCHAR |	Factory |
| line_id	VARCHAR |	Production | line |
| duration_ms |	BIGINT |	Duration |
| defect_count |	INT |	Defects |
| payload_hash |	TEXT |	Dedup hash |

### Business Rules 

- `event_id` is unique

- `defect_count = -1` is ignored in analytics

## Performance Strategy
### Processing 1000 events in under 1 second

Optimizations used:

| Strategy |	Benefit |
| -------- | -------------- |
| Batch ingestion |	Fewer HTTP calls |
| Indexed lookups |	Fast queries |
| Hash comparison |	Quick dedupe |
| Simple schema |	Low DB overhead |
| No heavy joins |	Fast analytics |

### Result
- 1000 events processed in < 1s
- Stable under concurrency

## Edge Cases & Assumptions
| Case |	Decision |
| ---- | --------- |
| Invalid duration |	Rejected |
| Future events |	Rejected |
| Duplicate events |	Deduped |
| Older updates |	Ignored |
| DefectCount = -1 |	Ignored |
| Same timestamp |	Deduped |
| Partial batch failure |	Others still processed |

### Tradeoffs
- Uses eventTime for ordering
- No Kafka for simplicity
- Full overwrite on update (no merge)

## ▶ Application Setup

### 1️⃣ Clone the repository

```bash
git clone https://github.com/your-username/factory-event-analytics-backend
cd factory-event-analytics-backend
```
2️⃣ Configure Database

Use environment variables for credentials:

```bash
spring.datasource.url=jdbc:postgresql://localhost:5432/soundstream
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

3️⃣ Run the application
```
./gradlew bootRun
```

Application will start at:
```
http://localhost:8080
```

## 🔗 API Endpoints
| Method | Endpoint                                                               | Description                                                 |
| ------ | ---------------------------------------------------------------------- | ----------------------------------------------------------- |
| POST   | `/events/batch`                                                        | Ingests a batch of machine events with validation, deduplication, and safe updates |
| GET    | `/stats`                               | Returns defect and health statistics for a specific machine in a time range |
| GET    | `/stats/top-defect-lines`        | Lists production lines with the highest defect rates for a factory |


## 🚀 Future Enhancements

- Kafka for ingestion
- Redis caching
- Async processing
- Prometheus metrics
- OpenTelemetry tracing
- CI/CD pipeline
- Load testing
- Sharded DB

## 📜 License

This project is licensed under the MIT License.
See the LICENSE file for more details.



---
