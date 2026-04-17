# AI Reminder — Nhom6_11_TMDT (Freelancer ↔ Employer platform)

## Nguồn tài liệu đã scan
- doc/Lớp 6 - Nhóm 11.pdf: tài liệu phân tích/đặc tả use case + thiết kế DB + UI/UX + lựa chọn công nghệ.
- doc/Lớp 6 - Nhóm 11.docx.pdf: quy tắc làm việc (Git Flow, branch, commit, PR, bảo mật repo).
- Text đã trích để đọc nhanh (không phải tài liệu gốc): doc/.pdftext/*.txt.

## Mục tiêu hệ thống (tóm tắt)
Xây dựng hệ thống kết nối Freelancer và Nhà tuyển dụng (Employer) theo mô hình marketplace:
- Employer đăng job, nhận proposal, gửi offer.
- Freelancer apply job, gửi proposal, nhận/từ chối offer.
- Khi offer được chấp nhận → tạo contract + milestones.
- Có cơ chế chat theo contract.
- Có cơ chế thanh toán/ví + escrow theo milestone.
- Có tranh chấp (dispute) theo milestone với vai trò Supporter/Admin.
- Có rating & review (blind review/reveal).

## Tác nhân / Role
- Employer
- Freelancer
- Admin (tài khoản tạo thủ công)
- Supporter (nhân viên hỗ trợ)
- Payment Gateway (cổng thanh toán, ví dụ VNPAY)

## Công nghệ/kiến trúc (theo tài liệu)
- Backend: Spring Boot (Java), định hướng microservices.
- Frontend: ReactJS.
- Database: MongoDB (tài liệu có mô tả bảng kiểu SQL để dễ hiểu thiết kế; khi triển khai Mongo cần mapping hợp lý).
- Message broker: Apache Kafka (luồng bất đồng bộ).
- Cache/session/token store: Redis.
- Payment gateway: VNPAY.
- Tooling: Docker/Compose, Git, Postman, Figma.

Ghi chú: phần mô tả microservices ở Chương 6 có ví dụ theo ngữ cảnh e-commerce (Product/Order/…); cần map lại sang domain Freelancer/Employer (Job/Proposal/Offer/Contract/Payment/Chat/Notification/Dispute).

## Auth & Security (các con số/ràng buộc quan trọng)
- JWT: Access Token TTL 1h, Refresh Token TTL 7 ngày.
- Email verification token TTL 24h.
- Password lưu dạng bcrypt hash.
- Users.status: pending/active/banned.
- Không commit secrets: .env, API keys, credentials.

## Thiết kế dữ liệu (bảng/collection chính theo tài liệu)
Tài liệu mô tả theo “bảng” và FK; các ràng buộc quan trọng:

### users
- user_id (UUID), email (unique), password_hash, role (employer/freelancer/admin/supporter), status (pending/active/banned), timestamps.

### profiles (1-1 với users)
- full_name, avatar_url, phone_number, bio, employer fields (company_name, company_website, industry), freelancer fields (skills JSON, hourly_rate), avg_rating, total_reviews.

### email_verification_tokens (N-1 users)
- token (unique), expires_at (TTL 24h), used_at.

### refresh_tokens (N-1 users)
- token_hash (unique), expires_at (TTL 7 ngày), revoked_at.

### jobs (N-1 users: employer)
- title, description (rich-text HTML), required_skills JSON, job_type (fixed/hourly), budget/hourly_rate/estimated_hours, deadline, status (open/in_progress/completed/cancelled), proposal_count.

### proposals (N-1 jobs, N-1 users: freelancer)
- cover_letter, bid_price, estimated_duration, status (pending/accepted/rejected).
- UNIQUE(job_id, freelancer_id): 1 freelancer chỉ apply 1 lần/job.

### offers
- job_id, proposal_id, employer_id, freelancer_id, contract_value, estimated_duration.
- status: pending/accepted/declined/expired, expires_at.

### contracts
- Tạo tự động khi freelancer accept offer.
- offer_id UNIQUE (1-1), job_id, employer_id, freelancer_id, total_value.
- status: active/completed/cancelled.

### milestones (N-1 contracts)
- amount, due_date, status: not_started/in_progress/submitted/approved/disputed.
- revision_count, max_revisions (default 3).

### deliverables (N-1 milestones)
- file_url/link_url/description, submitted_at.

### escrows (1-1 milestones)
- amount, is_frozen, locked_at, released_at.

### wallets (1-1 users)
- balance, frozen_balance.

### transactions (N-1 wallets)
- type: deposit/escrow_lock/milestone_release/platform_fee/withdrawal_request/refund/dispute_resolution.
- status: pending/completed/failed, reference_id, description.

### withdrawal_requests (N-1 users: freelancer)
- bank_name, bank_account, account_holder.
- status: pending/completed/rejected, admin_id, admin_note, processed_at.

### chat_threads (1-1 contracts)
- 1 contract ↔ 1 thread.

### messages (N-1 chat_threads)
- sender_id, content (<= 2000), attachment_url, is_read, sent_at.

### message_attachments (N-1 messages)
- file_url, file_name, mime_type, file_size (max 20MB), uploaded_at.

### chat_reports (N-1 messages)
- reporter_id, reason: offensive/fraud/harassment/other.
- status: pending/resolved, handled_by (supporter/admin).

### disputes (1-1 milestones)
- contract_id, initiator_id, reason.
- status: open/escalated/pending_evidence/resolved.
- assigned_supporter_id, resolution, timestamps.

### evidences (N-1 disputes)
- file_url, description, uploaded_at.

### dispute_assessments (1-1 disputes)
- staff_id (supporter), summary_note, decision: self_handle/escalate.

### dispute_decisions (1-1 disputes)
- admin_id, freelancer_pct, employer_pct, note, decided_at.

### reviews (blind review)
- UNIQUE(contract_id, reviewer_id).
- rating 1..5, status: pending/public, published_at.

### admin_logs
- action, target_user_id, reason, metadata.

### notifications
- title, body, is_read, created_at.

## Quy tắc làm việc (từ tài liệu rule)
- Git Flow:
  - main: production-ready, không push trực tiếp.
  - develop: tích hợp feature, không push trực tiếp, merge vào main cuối sprint.
  - feature/*: dev tính năng, merge vào develop qua PR.
- Đặt tên nhánh: <type>/TV<số>-<mô-tả-ngắn> (vd feature/TV1-auth-jwt-refresh-token).
- Commit message: Conventional Commits: <type>(<scope>): <mô tả>.
- PR:
  - PR theo 1 UC hoặc nhóm UC liên quan.
  - Ít nhất 1 người approve; không tự merge PR.
  - Resolve conflict trước khi review; xóa nhánh feature sau merge.
- Bảo mật repo:
  - Cấm commit .env, node_modules, dist, logs, credentials/secrets.
  - .gitignore tối thiểu: .env, .env.*, node_modules, dist, *.log, .DS_Store.
- Merge conflict: người tạo PR chịu trách nhiệm; không tự ý xóa code người khác.

## Checklist khi triển khai backend (gợi ý để AI bám)
- Bám đúng domain: Job → Proposal → Offer → Contract → Milestone → Escrow/Payment → Dispute → Review.
- Implement đúng unique constraints (apply 1 lần/job, 1 escrow/milestone, 1 thread/contract, 1 dispute/milestone, 1 review/user/contract).
- Thiết kế idempotency cho release escrow (không release 2 lần).
- Token lifecycle: refresh token hash + revoke; email verify token TTL.
- Audit/log và notification có schema riêng.

## Quy ước tổ chức source code (quan trọng)
User yêu cầu: mỗi service tuy là microservice nhưng codebase phải tổ chức giống “monolith Spring Boot” tiêu chuẩn theo layer để dễ đọc và đồng nhất.

Quy ước áp dụng cho TẤT CẢ services:
- domain/entity/model: các model domain (@Document/@Entity), enums, value objects.
- repository: Spring Data repositories.
- service: business logic (không để logic ở controller).
- controller: REST API.
- dto/request/response: input/output models + validation.
- config: security/configuration/properties.
- exception: custom exceptions + handler (ưu tiên tái sử dụng libs/common nếu phù hợp).

Package gợi ý: com.nhom611.<service>.{domain,repository,service,controller,dto,config,exception}.
Ghi chú: nếu cần tách theo feature (vd auth), vẫn giữ layer là trục chính (vd com...dto.auth.*, com...service.auth.*), không thay layer bằng feature package.
