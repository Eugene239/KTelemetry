# KTelemetry

Telemetry collection service built with Kotlin, Ktor, and ClickHouse.

## Project Structure

- `core-model`: Kotlin Multiplatform module containing shared data models
- `server-ktor`: Ktor backend server for receiving and storing telemetry events

## Prerequisites

- JDK 17 or higher
- Gradle 8.5+
- Docker and Docker Compose (for local development)

## Quick Start

Build and run the server:
```bash
./gradlew :server-ktor:run
```

The server will be available at `http://localhost:8080`

### Configuration

The server can be configured via:
1. `local.properties` file (for local development) - create this file in the project root
2. Environment variables (takes precedence over local.properties)

Configuration priority: `local.properties` → environment variables

**local.properties** (create this file for local development - REQUIRED):
```properties
host=0.0.0.0
port=8080
clickhouse.url=http://localhost:8123
clickhouse.user=default
clickhouse.password=
```

**Environment Variables** (alternative to local.properties):
- `HOST`: Server host (REQUIRED)
- `PORT`: Server port (REQUIRED)
- `CLICKHOUSE_URL`: ClickHouse HTTP endpoint (REQUIRED)
- `CLICKHOUSE_USER`: ClickHouse username (REQUIRED)
- `CLICKHOUSE_PASSWORD`: ClickHouse password (optional, defaults to empty string)

**Important**: All configuration parameters are required (except `clickhouse.password` which can be empty). 
The server will fail to start with a clear error message if any required parameter is missing.

Note: `local.properties` is ignored by git (already in .gitignore) so you can safely store local configuration there.

## API Endpoints

### POST /telemetry/events

Accepts a list of telemetry events and stores them in ClickHouse.

**Request Body:**
```json
[
  {
    "eventTime": 1234567890,
    "eventType": "user_action",
    "eventName": "button_click",
    "level": "INFO",
    "app": {
      "appId": "my-app",
      "appVersion": "1.0.0"
    },
    "user": {
      "userId": "user123"
    },
    "device": {
      "deviceId": "device456",
      "os": "Android"
    },
    "session": {
      "sessionId": "session789"
    },
    "context": {
      "feature": "checkout",
      "tags": ["payment", "mobile"]
    }
  }
]
```

**Response:** `202 Accepted`

### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "ok"
}
```

## Building

Build all modules:
```bash
./gradlew build
```

Build fat JAR for server:
```bash
./gradlew :server-ktor:jar
```

## License

MIT

