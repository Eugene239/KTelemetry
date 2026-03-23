#!/bin/bash
set -e

clickhouse-client --query="CREATE DATABASE IF NOT EXISTS telemetry"

clickhouse-client --query="
CREATE TABLE IF NOT EXISTS telemetry.events
(
    event_time DateTime,
    event_time_zone_id Nullable(String),
    event_utc_offset_minutes Nullable(Int16),

    event_type LowCardinality(String),
    event_name LowCardinality(String),
    level LowCardinality(String),

    app_id String,
    app_version LowCardinality(String),
    app_name LowCardinality(String),
    app_build_number LowCardinality(String),
    app_environment LowCardinality(String),

    user_id String,
    anonymous_id String,
    user_name LowCardinality(String),
    user_email LowCardinality(String),
    user_roles Array(String),

    device_id String,
    device_type LowCardinality(String),
    device_os LowCardinality(String),
    device_os_version LowCardinality(String),
    device_manufacturer LowCardinality(String),
    device_model LowCardinality(String),
    device_screen_width Nullable(UInt16),
    device_screen_height Nullable(UInt16),
    device_locale LowCardinality(String),
    device_orientation LowCardinality(String),
    network_type LowCardinality(String),
    battery_level Nullable(UInt8),
    memory_free Nullable(UInt64),
    memory_total Nullable(UInt64),
    storage_free Nullable(UInt64),
    is_foreground Nullable(UInt8),
    is_rooted Nullable(UInt8),

    session_id String,
    session_start_time Nullable(DateTime),

    feature LowCardinality(String),
    screen_name LowCardinality(String),
    previous_screen LowCardinality(String),
    breadcrumb_type LowCardinality(String),
    duration_ms Nullable(UInt64),
    tags Array(String),
    feature_flags String,
    payload String,

    error_type LowCardinality(String),
    error_message String,
    error_stacktrace String,
    error_is_fatal UInt8 DEFAULT 0,
    error_thread LowCardinality(String),
    error_threads String
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(event_time)
ORDER BY (event_type, event_time, app_id, session_id)
TTL event_time + INTERVAL 1 YEAR;
"
