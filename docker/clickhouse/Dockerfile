FROM clickhouse/clickhouse-server:24.8-alpine

COPY config/logging.xml /etc/clickhouse-server/config.d/logging.xml
COPY config/performance.xml /etc/clickhouse-server/config.d/performance.xml
COPY init-clickhouse.sh /docker-entrypoint-initdb.d/init.sh
