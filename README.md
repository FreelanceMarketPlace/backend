# Nhom6_11 Backend

Monorepo Maven multi-module cho các Spring Boot services.

## Prerequisites
- Java 17+
- Maven 3.9+
- MongoDB (local) hoặc chạy bằng docker-compose (xem infra/)

## Run infra (optional)
Nếu dùng docker-compose, vào thư mục repo và chạy:

`docker compose -f infra/docker-compose.yml up -d`

## Build
Build toàn bộ:

`mvn -DskipTests package`

## Run user-service
JWT secret là bắt buộc (HS256) — có thể lấy theo mẫu ở `.env.example`.

Ví dụ chạy nhanh:

`AUTH_JWT_SECRET='change-me-to-a-long-random-secret-change-me' mvn -pl services/user-service -am spring-boot:run`

### Reuse `.env` cho nhiều lần chạy Maven
Nếu bạn đã tạo `.env` ở root repo, có thể dùng script wrapper để khỏi phải gán biến môi trường mỗi lần:

`./scripts/mvn-env.sh -pl services/user-service -am spring-boot:run`

## Quick test (curl)
Tạo thư mục tạm để lưu cookie/log:

`mkdir -p .tmp`

Ghi chú khi test bằng Postman:
- Với các endpoint public như `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` hãy để **Authorization = No Auth** (đừng gửi `Authorization: Bearer ...`).
- Chỉ gửi Bearer token cho endpoint cần auth như `GET /me`.

1) Register

`curl -s -X POST http://localhost:8081/auth/register \
	-H 'Content-Type: application/json' \
	-d '{"email":"test@example.com","password":"Password123","role":"EMPLOYER"}' | jq`

2) Login (lưu cookie refresh)

`curl -i -s -X POST http://localhost:8081/auth/login \
	-H 'Content-Type: application/json' \
	-d '{"email":"test@example.com","password":"Password123"}' \
	-c .tmp/cookies.txt`

3) Refresh (đọc cookie, nhận access token mới)

`curl -i -s -X POST http://localhost:8081/auth/refresh \
	-b .tmp/cookies.txt \
	-c .tmp/cookies.txt`

4) /me (dùng Bearer access token)
Lấy access token từ response login/refresh rồi gọi:

`curl -s http://localhost:8081/me -H 'Authorization: Bearer <ACCESS_TOKEN>' | jq`

5) Logout (revoke refresh + clear cookie)

`curl -i -s -X POST http://localhost:8081/auth/logout -b .tmp/cookies.txt -c .tmp/cookies.txt`

## Users API (MVP)
Fetch danh sách users (yêu cầu Bearer access token):

`curl -s 'http://localhost:8081/users?page=0&size=20' -H 'Authorization: Bearer <ACCESS_TOKEN>' | jq`

