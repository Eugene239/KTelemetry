# KTelemetry

Telemetry collection service built with Kotlin, Ktor, and ClickHouse.

## Project Structure

- `core-model` — Kotlin Multiplatform shared data models
- `server-ktor` — Ktor backend server
- `android/android-library` — Android client SDK
- `android/android-demo-app` — Android demo application

## CI/CD

Three independent publish workflows (manual dispatch):

| Workflow | Source | Artifact |
|---|---|---|
| `publish-server.yml` | `server-ktor/`, `core-model/`, `docker/server/` | Docker → GHCR → Coolify webhook |
| `publish-clickhouse.yml` | `docker/clickhouse/` | Docker → GHCR → Coolify webhook |
| `publish-android.yml` | `android/android-library/`, `core-model/` | Maven → GitHub Packages |

## Android SDK

### Installation

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/eugene239/KTelemetry")
            credentials {
                username = providers.gradleProperty("gpr.user").getOrElse(System.getenv("GITHUB_ACTOR") ?: "")
                password = providers.gradleProperty("gpr.token").getOrElse(System.getenv("GITHUB_TOKEN") ?: "")
            }
        }
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.epavlov.ktelemetry:core-model:0.1.+")
    implementation("io.epavlov.ktelemetry:android-library-android:0.1.+")
}
```

### Usage

```kotlin
// Application.onCreate()
TelemetryClient.getInstance().initialize(this, TelemetryConfig(
    serverUrl = "https://api.yourdomain.com",
    apiKey = "your-telemetry-api-key",
    appId = "my-app",
    appVersion = "1.0.0",
))

// Track events
TelemetryClient.getInstance().trackEvent("button_click", "user_action")
TelemetryClient.getInstance().trackScreen("HomeScreen")
TelemetryClient.getInstance().trackBreadcrumb("user_tapped_buy", "user")
TelemetryClient.getInstance().trackError(exception)
```

## Server

### Local development

```bash
cp local.properties.example local.properties   # edit with your values
./gradlew :server-ktor:run
```

Only `clickhouse.url` is required. `host` defaults to `0.0.0.0`, `port` to `8080`, `clickhouse.user` to `default`, `clickhouse.password` to empty. Set `telemetry.apiKey` to require `X-API-Key` on telemetry ingest requests.

#### Connecting to a remote ClickHouse

To run the server locally against a ClickHouse deployed on a remote host (e.g. Coolify), set the remote URL and credentials in `local.properties`:

```properties
clickhouse.url=https://play.ktelemetry.online
clickhouse.user=telemetry
clickhouse.password=<your-password>
```

Then start the server as usual:

```bash
./gradlew :server-ktor:run
# health check: http://localhost:8080/health
```

#### Local ClickHouse via Docker

```bash
cd docker && cp .env.example .env && docker compose up -d clickhouse
```

```properties
clickhouse.url=http://localhost:8123
```

ClickHouse Play UI: `http://localhost:8123/play`

### Configuration

| Property | Env variable | Default | Required |
|---|---|---|---|
| `host` | `HOST` | `0.0.0.0` | no |
| `port` | `PORT` | `8080` | no |
| `clickhouse.url` | `CLICKHOUSE_URL` | — | **yes** |
| `clickhouse.user` | `CLICKHOUSE_USER` | `default` | no |
| `clickhouse.password` | `CLICKHOUSE_PASSWORD` | _(empty)_ | no |
| `telemetry.apiKey` | `TELEMETRY_API_KEY` | _(empty, auth disabled)_ | no |

Properties are resolved in order: `local.properties` → environment variable → default.

### API

- `POST /telemetry/events` — accepts `List<TelemetryEvent>`, returns `202 Accepted` (or `401` if API key auth is enabled and `X-API-Key` is missing/invalid)
- `GET /health` — returns status, uptime, version, ClickHouse database sizes

### Docker images

```bash
docker pull ghcr.io/eugene239/ktelemetry/server:master
docker pull ghcr.io/eugene239/ktelemetry/clickhouse:master
```

### Full local stack

```bash
cd docker && cp .env.example .env && docker compose up -d
```

## Deploying on Coolify

Deploy as **Docker Compose** (`docker/docker-compose.yml`) or as separate Docker Image resources.

Assign domains per service:
- `clickhouse` → `play.yourdomain.com` (port 8123)
- `backend` → `api.yourdomain.com` (port 8080)

### GitHub Secrets

| Secret | Used by | Description |
|---|---|---|
| `DEPLOY_WEBHOOK_SERVER` | publish-server | Coolify webhook URL for backend |
| `DEPLOY_WEBHOOK_CLICKHOUSE` | publish-clickhouse | Coolify webhook URL for ClickHouse |
| `DEPLOY_WEBHOOK_TOKEN` | both | (optional) Bearer token |

## License

MIT
