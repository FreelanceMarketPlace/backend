# user-service

Auth + user/profile APIs.

## Run
- `./scripts/mvn-env.sh -pl services/user-service -am spring-boot:run`
- Default port: `8081`

## Env
- `MONGODB_URI` (default in code: `mongodb://localhost:27017/nhom611_user`)
- `AUTH_JWT_SECRET` (HS256, >= 32 chars)
- `CORS_ALLOWED_ORIGINS` (comma-separated)

## MVP endpoints
Public:
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

Auth required (Bearer access token):
- `GET /me`
- `GET /users`

Postman note: với `/auth/*` hãy để **No Auth**.
