FROM clickhouse/clickhouse-server:24.3

COPY config.d/kafka.xml /etc/clickhouse-server/config.d/kafka.xml
COPY config.d/users.xml /etc/clickhouse-server/users.d/users.xml
COPY initdb.d/01_create_tables.sql /docker-entrypoint-initdb.d/01_create_tables.sql

EXPOSE 8123
EXPOSE 9000
