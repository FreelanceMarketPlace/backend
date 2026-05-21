# job-service

Job Posting APIs (create/list/detail/update/close).

## Run
- `./scripts/mvn-env.sh -pl services/job-service -am spring-boot:run`
- Default port: `8082`

## Env
- `MONGODB_URI` (default in code: `mongodb://localhost:27017/nhom611_job`)
- `AUTH_JWT_SECRET` (HS256, >= 32 chars)
- `CORS_ALLOWED_ORIGINS` (comma-separated)

## MVP endpoints
Public:
- `GET /jobs` (search/filter/pagination)
- `GET /jobs/{jobId}`

Employer (Bearer + role=EMPLOYER):
- `POST /jobs`
- `PATCH /jobs/{jobId}` (owner only, job must be `OPEN`)
- `POST /jobs/{jobId}/close` (owner only, `OPEN -> CANCELLED`)
- `GET /employer/jobs`
