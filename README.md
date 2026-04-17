# Nhom6_11 Backend

Backend monorepo (Maven multi-module) cho hệ thống Freelancer ↔ Employer. Mỗi service được tổ chức code theo cấu trúc “monolith-style” theo layer (domain/repository/service/controller/dto/config) để dễ đọc và đồng nhất.

## Repo structure
- `libs/common`: shared controller `/health`, `ApiError`, exception handler.
- `services/*`: các Spring Boot services (tách theo domain).
- `infra/docker-compose.yml`: local infra (MongoDB, Redis, Kafka).

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

Chỉ chạy MongoDB (phù hợp khi đang làm user-service):
- `docker compose -f infra/docker-compose.yml up -d mongodb`

Reset dữ liệu local (xoá volume) nếu cần:
- `docker compose -f infra/docker-compose.yml down -v`

### MongoDB database name
Mỗi service có thể dùng DB riêng.
- `user-service` mặc định dùng `mongodb://localhost:27017/nhom611_user` (set qua `MONGODB_URI`).

## Configuration
- Không commit secrets. Dùng `.env.example` làm mẫu và tạo `.env` (đã bị `.gitignore`).
- Wrapper script `./scripts/mvn-env.sh` sẽ tự `source .env` và chạy Maven.

## Build
Build toàn repo:
- `./scripts/mvn-env.sh -DskipTests package`

Build riêng user-service:
- `./scripts/mvn-env.sh -pl services/user-service -am -DskipTests package`

## Run (dev)
Chạy user-service:
- `./scripts/mvn-env.sh -pl services/user-service -am spring-boot:run`

## API (MVP)
Các endpoint để test nhanh bằng Postman/curl (user-service):
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /me` (cần Bearer access token)
- `GET /users` (cần Bearer access token)

Ghi chú Postman: với các endpoint public dưới `/auth/*`, hãy để **Authorization = No Auth** (đừng gửi `Authorization: Bearer ...`).

