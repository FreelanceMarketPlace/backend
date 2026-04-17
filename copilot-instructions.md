# Copilot/AI Instructions (project bootstrap)

- Tham chiếu tài liệu gốc ở doc/ (đã tóm tắt trong doc/AI_REMINDER.md).
- Luôn tuân thủ rule làm việc: Git Flow, branch naming, Conventional Commits, PR/review.
- Không bao giờ commit secrets: .env, API keys, credentials; chỉ commit .env.example (placeholder).
- Backend định hướng Spring Boot microservices; auth JWT (access 1h, refresh 7d), email verification token TTL 24h.
- Domain chính: Job/Proposal/Offer/Contract/Milestone/Chat/Payment(Escrow+Wallet)/Dispute/Review/Admin.

## Quy ước cấu trúc code cho MỖI service (monolith-style)
- Mỗi service được tổ chức như một repo Spring Boot “monolith” tiêu chuẩn theo layer:
	- domain/entity/model: các @Document/@Entity, enum, value-object
	- repository: Spring Data repositories
	- service: business logic, transaction/flow, orchestration
	- controller: REST API layer
	- dto/request/response: input/output models, validation
	- config: @Configuration, @ConfigurationProperties
	- exception (nếu có): custom exceptions + handler (ưu tiên dùng common nếu phù hợp)
- Ưu tiên package theo layer (không tách package theo feature như auth/*) để consistent giữa các service.
- Tên package gợi ý: com.nhom611.<service>.{domain,repository,service,controller,dto,config,exception}.
- Khi cần tách theo feature, vẫn giữ layer là trục chính (vd: dto/auth/* chỉ là con của dto, không thay layer bằng feature).
