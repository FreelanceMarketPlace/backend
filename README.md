# Nhom6_11 Backend

Backend monorepo (Maven multi-module) cho hệ thống Freelancer ↔ Employer. Mỗi service được tổ chức code theo cấu trúc “monolith-style” theo layer (domain/repository/service/controller/dto/config) để dễ đọc và đồng nhất.

## Repo structure
- `libs/common`: shared controller `/health`, `ApiError`, exception handler.
- `services/*`: các Spring Boot services (tách theo domain).
- `infra/docker-compose.yml`: local infra (MongoDB, Redis, Kafka).

## Services (local ports)
- `user-service` (services/user-service): `8081`
- `job-service` (services/job-service): `8082`
- `contract-service` (services/contract-service): `8083`
- `payment-service` (services/payment-service): `8084`
- `chat-service` (services/chat-service): `8085`
- `notification-service` (services/notification-service): `8086`

Health checks (mặc định):
- `GET /health`
- `GET /actuator/health`

## Prerequisites
- Java 17+
- Maven 3.8+
- Docker + Docker Compose (khuyến nghị cho local DB)

## Local database / infra
Infra được cung cấp bằng Docker Compose trong `infra/docker-compose.yml`:
- MongoDB 7 (port `27017`)
- Redis 7 (port `6379`)
- Kafka + Zookeeper (port `9092`, `2181`)

Chạy toàn bộ infra:
- `docker compose -f infra/docker-compose.yml up -d`

Chỉ chạy MongoDB (phù hợp khi chỉ cần DB):
- `docker compose -f infra/docker-compose.yml up -d mongodb`

Reset dữ liệu local (xoá volume) nếu cần:
- `docker compose -f infra/docker-compose.yml down -v`

### MongoDB database name
Mỗi service có thể dùng DB/collection riêng. Hiện tại các service đọc URI từ `spring.data.mongodb.uri` và được map từ env `MONGODB_URI` (xem `application.properties`/`application.yml` của từng service).

## Configuration
- Không commit secrets. Dùng `.env.example` làm mẫu và tạo `.env` (đã bị `.gitignore`).
- Wrapper script `./scripts/mvn-env.sh` sẽ tự `source .env` và chạy Maven.

## Build
Build toàn repo:
- `./scripts/mvn-env.sh -DskipTests package`

Build riêng user-service:
Build riêng 1 service (ví dụ user-service):
- `./scripts/mvn-env.sh -pl services/user-service -am -DskipTests package`

Ví dụ job-service:
- `./scripts/mvn-env.sh -pl services/job-service -am -DskipTests package`

## Run (dev)
Chạy 1 service bất kỳ (ví dụ user-service):
- `./scripts/mvn-env.sh -pl services/user-service -am spring-boot:run`

Chạy job-service:
- `./scripts/mvn-env.sh -pl services/job-service -am spring-boot:run`

Tài liệu chi tiết theo service:
- user-service: [services/user-service/README.md](services/user-service/README.md)
- job-service: [services/job-service/README.md](services/job-service/README.md)

