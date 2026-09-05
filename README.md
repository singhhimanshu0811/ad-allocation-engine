# RouteIQ Device Service

Spring Boot REST microservice using JSON over HTTP.

## Requirements

- Java 17+
- Maven 3.9+

## Run

```bash
mvn clean spring-boot:run
```

The service runs on:

```text
http://localhost:3033
```

## APIs

All requests require device authentication headers:

```http
X-Device-Id: device-123
X-Device-Key: device-secret
```

Requests with missing or invalid credentials receive `401 Unauthorized`.
Credentials are cached in memory for one hour by default. Configure the cache duration with `DEVICE_AUTH_CACHE_TTL_MS`.

Configured task intervals are:

- `POST_GEO`: 60 seconds
- `SERVE_AD`: 900 seconds (15 minutes)

Override them with `POST_GEO_POLL_INTERVAL_SECONDS` and `SERVE_AD_POLL_INTERVAL_SECONDS`.

### 1. Get device config

```http
POST /api/devices/config
Content-Type: application/json
```

Request:

```json
{
  "deviceId": "device-123"
}
```

Response:

```text
deviceId: device-123
enabled: true
pollIntervalSeconds: 30
```

### 2. Get device task

```http
GET /adservice/api/devices/task?deviceId=device-123
```

Returns the configured task for the device, or `404 Not Found` when no task exists.

Response:

```json
{
  "task": "POST_GEO",
  "pollIntervalSeconds": 60
}
```

### 3. Add or update device task

```http
PUT /adservice/api/devices/task
Content-Type: application/json
```

```json
{
  "deviceId": "device-123",
  "task": "SERVE_AD"
}
```

The task must be `POST_GEO` or `SERVE_AD`. Its polling interval is populated from configuration.

### 4. Save device route

```http
POST /adservice/api/devices/routes
Content-Type: application/json
```

```json
{
  "deviceId": "device-123",
  "fromLocation": "Bengaluru",
  "toLocation": "Mysuru"
}
```

Response:

```json
{
  "routeId": 1
}
```

Any previously active route for that device is deactivated, and the new route becomes active.

### 5. Save geo locations

Geo locations are stored against the device's currently active route. If the device has no active route, the request returns `409 Conflict`.

```http
POST /api/devices/geo
Content-Type: application/json
```

Request:

```json
{
  "deviceId": "device-123",
  "locations": [
    {
      "lat": 12.9716,
      "lang": 77.5946,
      "speed": 12.5,
      "heading": 180.0,
      "generatedAt": "2026-09-02T10:00:00Z"
    },
    {
      "lat": 12.9720,
      "lang": 77.5950,
      "speed": 13.0,
      "heading": 45.0,
      "generatedAt": "2026-09-02T10:01:00Z"
    }
  ]
}
```

Response:

```text
saved okay
```

### 6. Get images for geo location

```http
POST /api/devices/images
Content-Type: application/json
```

Request:

```json
{
  "deviceId": "device-123",
  "location": {
    "lat": 12.9716,
    "lang": 77.5946,
    "speed": 12.5,
    "heading": 180.0,
    "generatedAt": "2026-09-02T10:00:00Z"
  }
}
```

Response:

```json
[
  {
    "imageUrl": "https://example.com/images/device-123/image-1.jpg",
    "timestamp": "2026-09-02T10:00:00Z"
  },
  {
    "imageUrl": "https://example.com/images/device-123/image-2.jpg",
    "timestamp": "2026-09-02T10:00:00Z"
  }
]
```

### 7. Heartbeat

```http
POST /api/devices/heartbeat
Content-Type: application/json
```

Request:

```json
{
  "deviceId": "device-123",
  "heartbeat": true
}
```

Response:

```text
OKay
```

## Notes

- `lang` is retained exactly as requested and represents longitude.
- `heading` is expressed in degrees from 0 (inclusive) to 360 (exclusive).
- `generatedAt` is the client-side timestamp for when the location was generated.
- Device configuration, geo locations, and heartbeats are stored in PostgreSQL.
- Image metadata has a PostgreSQL table and repository; image ingestion/storage is not exposed by the current API.
- All tables include `created_at`, `updated_at`, and `version` columns. The `version` column is managed by JPA for optimistic locking.
- Heartbeats older than seven days are removed by a scheduled application job every 12 hours by default. Configure the schedule with `HEARTBEAT_CLEANUP_FIXED_DELAY_MS` and retention with `HEARTBEAT_RETENTION_DAYS`.

## PostgreSQL configuration

The application runs Flyway migrations at startup. Override these environment variables for a non-default database:

```text
DB_URL=jdbc:postgresql://localhost:5432/device_service
DB_USERNAME=postgres
DB_PASSWORD=postgres
```
