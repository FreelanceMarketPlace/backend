# backend (multi-service)

Backend monorepo theo kiểu multi-module Maven: mỗi folder trong `services/` là một service Spring Boot.

## Yêu cầu
- Java 17
- Docker + Docker Compose

## Chạy infra (MongoDB + Redis + Kafka)

```bash
docker compose -f infra/docker-compose.yml up -d
```

## Build toàn bộ

```bash
mvn -q -DskipTests package
```

## Chạy từng service

Ví dụ chạy User Service:

```bash
mvn -pl services/user-service spring-boot:run
```

Ports mặc định:
- user-service: 8081
- job-service: 8082
- contract-service: 8083
- payment-service: 8084
- chat-service: 8085
- notification-service: 8086

## Health check
- `GET /health` (từ module `libs/common`, được scan bởi các service)
- `GET /actuator/health`

## Ghi chú
- Quy tắc làm việc và tóm tắt tài liệu nằm ở `doc/AI_REMINDER.md`.
- Không commit `.env` / secrets; dùng `.env.example` làm mẫu.
