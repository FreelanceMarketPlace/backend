# Copilot/AI Instructions (project bootstrap)

- Tham chiếu tài liệu gốc ở doc/ (đã tóm tắt trong doc/AI_REMINDER.md).
- Luôn tuân thủ rule làm việc: Git Flow, branch naming, Conventional Commits, PR/review.
- Không bao giờ commit secrets: .env, API keys, credentials; chỉ commit .env.example (placeholder).
- Backend định hướng Spring Boot microservices; auth JWT (access 1h, refresh 7d), email verification token TTL 24h.
- Domain chính: Job/Proposal/Offer/Contract/Milestone/Chat/Payment(Escrow+Wallet)/Dispute/Review/Admin.
