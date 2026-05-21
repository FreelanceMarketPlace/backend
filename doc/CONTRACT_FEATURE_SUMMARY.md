# Contract và Milestone System - Implementation Summary

## ✅ Mục tiêu: Bám sát tài liệu phân tích thiết kế

Tài liệu này mô tả cụm use case trong [Lớp 6 - Nhóm 11.txt](Lớp%206%20-%20Nhóm%2011.txt): `Contract và Milestone`, trong đó contract không phải feature độc lập mà là điểm khởi tạo cho cả chuỗi nghiệp vụ gồm milestone, deliverable, escrow, notification và chat thread.

---

### Backend Implementation (Java Spring Boot)

#### 1. **Domain Layer**
- **OfferStatus.java**: Trạng thái offer đúng theo tài liệu
  - `PENDING` - Chờ freelancer phản hồi
  - `ACCEPTED` - Freelancer chấp nhận offer
  - `DECLINED` - Freelancer từ chối offer
  - `EXPIRED` - Offer hết hạn

- **ContractStatus.java**: Trạng thái contract theo thiết kế
  - `ACTIVE` - Hợp đồng đang hiệu lực / job ở trạng thái In Progress
  - `COMPLETED` - Toàn bộ milestone đã hoàn tất
  - `CANCELLED` - Hợp đồng bị huỷ

- **MilestoneStatus.java**: Trạng thái milestone theo use case
  - `NOT_STARTED` - Chưa bắt đầu
  - `IN_PROGRESS` - Đang thực hiện
  - `SUBMITTED` - Freelancer đã nộp deliverable
  - `APPROVED` - Employer đã duyệt và release escrow
  - `DISPUTED` - Có tranh chấp

- **Offer.java**: Entity phản ánh bảng `offers`
  - `offerId`, `jobId`, `proposalId`
  - `employerId`, `freelancerId`
  - `jobDescription`, `contractValue`, `estimatedDuration`
  - `status`, `expiresAt`, `createdAt`

- **Contract.java**: Entity phản ánh bảng `contracts`
  - `contractId`
  - `jobId`, `offerId`
  - `employerId`, `freelancerId`
  - `totalValue`
  - `startDate`, `endDate`
  - `status`, `createdAt`
  - Ràng buộc 1-1 giữa `contract.offerId` và `offers.offer_id`

- **Milestone.java**: Entity phản ánh bảng `milestones`
  - `milestoneId`, `contractId`
  - `title`, `description`, `amount`, `dueDate`
  - `status`, `revisionCount`, `maxRevisions`
  - `createdAt`, `updatedAt`

- **Deliverable.java**: Entity phản ánh bảng `deliverables`
  - `deliverableId`, `milestoneId`
  - `fileUrl`, `linkUrl`, `description`, `submittedAt`

- **Escrow.java**: Entity phản ánh bảng `escrows`
  - `escrowId`, `milestoneId`
  - `amount`, `isFrozen`, `lockedAt`, `releasedAt`

#### 2. **Repository Layer**
- **OfferRepository**
  - `findByProposalId()` - Lấy offer gốc từ proposal đã chọn
  - `findByJobId()` - Tra cứu offer theo job
  - `findByFreelancerId()` - Lấy offer của freelancer
  - `existsByProposalId()` - Tránh tạo offer trùng trên cùng proposal

- **ContractRepository**
  - `findByOfferId()` - Lấy contract gắn với offer
  - `findByJobId()` - Lấy contract theo job
  - `findByEmployerId()` - Employer xem danh sách contract
  - `findByFreelancerId()` - Freelancer xem danh sách contract
  - `existsByOfferId()` - Đảm bảo offer chỉ sinh một contract

- **MilestoneRepository**
  - `findByContractId()` - Lấy milestones của một contract
  - `findByContractIdAndStatus()` - Lọc milestone theo trạng thái
  - `countByContractIdAndStatus()` - Phục vụ thống kê / logic hoàn thành contract

- **DeliverableRepository**
  - `findByMilestoneId()` - Lấy deliverable của milestone

- **EscrowRepository**
  - `findByMilestoneId()` - Lấy escrow theo milestone
  - `existsByMilestoneId()` - Một milestone chỉ có một escrow

#### 3. **Service Layer**
- **ContractService**: Business logic khởi tạo contract
  - `createContractFromOffer()` - Tạo contract khi freelancer accept offer
  - `initMilestones()` - Khởi tạo milestone list cho contract
  - `createChatThreadForContract()` - Tạo thread riêng cho contract
  - `updateJobStatusToInProgress()` - Chuyển job sang `IN_PROGRESS`
  - `notifyBothParties()` - Gửi thông báo cho employer và freelancer

- **MilestoneService**: Business logic cho milestone
  - `submitMilestone()` - Freelancer nộp deliverable
  - `requestRevision()` - Employer yêu cầu chỉnh sửa
  - `approveMilestone()` - Employer approve milestone
  - `markContractCompletedIfAllApproved()` - Kết thúc contract khi tất cả milestone đã xong

- **EscrowService**: Business logic giải ngân
  - `lockEscrowForMilestone()` - Khoá tiền theo milestone
  - `releaseEscrow()` - Giải ngân sau khi milestone được approve
  - `markEscrowReleased()` - Cập nhật trạng thái escrow
  - `creditWallet()` - Cộng tiền vào ví freelancer
  - `logTransaction()` - Ghi nhận giao dịch

#### 4. **Controller & DTOs**
- **ContractController**: REST endpoints
  ```
  POST   /offers/{offerId}/accept                    - Freelancer accept offer
  POST   /contracts/from-offer/{offerId}             - Hệ thống tạo contract từ offer
  GET    /employer/contracts                         - Employer xem contract
  GET    /freelancer/contracts                       - Freelancer xem contract
  GET    /contracts/{contractId}                     - Xem chi tiết contract
  ```

- **MilestoneController**: REST endpoints
  ```
  GET    /contracts/{contractId}/milestones          - Xem milestones của contract
  POST   /milestones/{milestoneId}/submit            - Freelancer submit deliverable
  POST   /milestones/{milestoneId}/revision          - Employer request revision
  POST   /milestones/{milestoneId}/approve           - Employer approve milestone
  ```

- **EscrowController**: REST endpoints nội bộ / business API
  ```
  POST   /escrows/{milestoneId}/lock                 - Khoá tiền cho milestone
  POST   /escrows/{milestoneId}/release              - Giải ngân milestone
  ```

- **ContractDtos.java**: Request/Response DTOs
  ```java
  CreateContractRequest {
    offerId: String
  }

  MilestoneSubmitRequest {
    fileUrl: String | null
    linkUrl: String | null
    description: String | null
  }

  RevisionRequest {
    revisionDesc: String
  }

  MilestoneApproveRequest {
    confirmedByEmployerId: String
  }
  ```

---

### Integration Flow

#### 1. Offer Accepted → Auto Create Contract
- Freelancer accept offer
- Hệ thống kiểm tra offer còn hợp lệ, chưa expired và chưa bị decline
- Auto tạo contract mới, gắn `jobId`, `offerId`, `employerId`, `freelancerId`
- Khởi tạo milestone list theo kế hoạch dự án
- Tạo chat thread riêng cho contract
- Job chuyển sang `In Progress`
- Gửi notification cho cả employer và freelancer

#### 2. Submit Milestone
- Freelancer vào Contract Dashboard
- Chọn contract và milestone đang `In Progress`
- Upload file hoặc nhập link deliverable
- Hệ thống lưu deliverable, chuyển milestone sang `Submitted`
- Gửi notification cho employer

#### 3. Request Revision
- Employer mở milestone ở trạng thái `Submitted`
- Nhập mô tả yêu cầu chỉnh sửa
- Hệ thống tăng `revisionCount`
- Nếu chưa vượt `maxRevisions` thì milestone quay về `In Progress`
- Gửi notification cho freelancer

#### 4. Approve Milestone & Release Payment
- Employer approve milestone `Submitted`
- Hệ thống kiểm tra escrow đang `LOCKED`
- Release escrow, trừ phí nền tảng, cộng net amount vào ví freelancer
- Ghi transaction log
- Cập nhật milestone sang `Approved`
- Nếu tất cả milestone đã approved thì contract có thể chuyển `COMPLETED`

---

### Frontend Implementation (React + TypeScript)

#### 1. **Types** (`types/contract.ts`)
```typescript
type OfferStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED'
type ContractStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
type MilestoneStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'SUBMITTED' | 'APPROVED' | 'DISPUTED'

interface ContractResponse {
  id: string
  jobId: string
  offerId: string
  employerId: string
  freelancerId: string
  totalValue: number
  status: ContractStatus
  startDate: string
  endDate: string | null
  createdAt: string
}
```

#### 2. **API Layer** (`api/contractApi.ts`)
- Các hàm chính:
  - `acceptOffer()`
  - `getEmployerContracts()`
  - `getFreelancerContracts()`
  - `getContractDetail()`
  - `getContractMilestones()`
  - `submitMilestone()`
  - `requestRevision()`
  - `approveMilestone()`
- Error handling & axios integration theo cùng pattern với các API hiện tại

#### 3. **UI Components**

**a) OfferDetailPage.tsx / Offer action**
- Freelancer xem offer và chọn chấp nhận hoặc từ chối
- Khi accept → contract được tạo tự động

**b) ContractDashboard.tsx (New)**
- Danh sách contract theo role
- Tab hiển thị milestones, deliverables, status và timeline
- Entry point cho submit / revision / approve milestone

**c) MilestoneSubmitForm.tsx (New)**
- Freelancer upload file hoặc nhập link
- Hiển thị validate cho file/link/description

**d) RevisionRequestForm.tsx (New)**
- Employer nhập nội dung yêu cầu chỉnh sửa
- Chặn submit khi milestone đã vượt quá `maxRevisions`

**e) MilestoneApproveDialog.tsx (New)**
- Employer xác nhận approve milestone
- Hiển thị thông tin escrow và số tiền giải ngân

#### 4. **Navigation Updates**
- **AppLayout.tsx**: Thêm menu cho Contract Dashboard theo role `EMPLOYER` và `FREELANCER`
- **App.tsx**: Thêm route cho contract dashboard và milestone actions

---

## 🔄 Workflow

### Freelancer Flow
1. Nhận offer từ employer
2. Accept offer
3. Contract được tạo tự động
4. Mở Contract Dashboard
5. Submit deliverable theo từng milestone
6. Theo dõi revision / approve / payment release

### Employer Flow
1. Tạo job và nhận proposal
2. Gửi offer dựa trên proposal được chọn
3. Freelancer accept offer → contract tự sinh
4. Xem contract và milestones trên dashboard
5. Request revision nếu deliverable chưa đạt
6. Approve milestone để release escrow

---

## 🎯 Key Features

✅ **Data Consistency**
- `offers.offer_id` là nguồn gốc trực tiếp của `contracts.offer_id`
- Một contract gắn với một offer, một escrow gắn với một milestone
- Một milestone chỉ có một deliverable set và một escrow

✅ **Business Logic**
- Accept offer → tạo contract tự động
- Contract tạo xong thì job chuyển sang `In Progress`
- Submit milestone → milestone `Submitted`
- Request revision → milestone quay về `In Progress` và tăng `revision_count`
- Approve milestone → release escrow, cộng tiền vào ví freelancer

✅ **UX**
- Contract Dashboard là trung tâm thao tác nghiệp vụ
- Có form riêng cho submit milestone và request revision
- Trạng thái milestone / contract / escrow hiển thị rõ ràng

---

## 📊 Data Model Relationship

```
Job
├── Proposal[]
├── Offer[]
└── Contract
    ├── offer_id (1-1)
    ├── milestone[] (1-N)
    ├── chat_thread (1-1)
    └── status = ACTIVE → COMPLETED/CANCELLED

Milestone
├── deliverable (1-N, thực tế hệ thống lưu 1 lần nộp chính)
├── escrow (1-1)
└── status = NOT_STARTED → IN_PROGRESS → SUBMITTED → APPROVED / DISPUTED
```

---

## 🚀 Next Steps (Optional Future Features)

1. **Chat Integration** - Gắn trực tiếp vào contract thread
2. **Dispute Handling** - Mở tranh chấp từ milestone `Submitted` hoặc `Disputed`
3. **Payment/Wallet** - Tách riêng service escrow & ví theo nghiệp vụ tài chính
4. **Notifications** - Đồng bộ notification cho submit/revision/approve/release
5. **Review/Rating** - Sau khi contract `Completed`
6. **Admin Dashboard** - Xử lý tranh chấp và log tài chính

---

## ✅ Testing Checklist

### Backend
- [ ] Accept offer → contract được tạo tự động
- [ ] Contract tạo xong → job chuyển `In Progress`
- [ ] Milestone submit thành công
- [ ] Request revision khi revision_count chưa vượt giới hạn
- [ ] Approve milestone và release escrow
- [ ] Không release escrow hai lần
- [ ] Gửi notification đúng bên

### Frontend
- [ ] ContractDashboard hiển thị đúng theo role
- [ ] MilestoneSubmitForm validate file/link/description
- [ ] RevisionRequestForm chặn khi vượt max revisions
- [ ] Approve dialog hiển thị đúng escrow amount
- [ ] Status badges render đúng cho contract/milestone

---

## Files Modified/Created

### Backend
```
services/contract-service/src/main/java/com/nhom611/contractsvc/
├── domain/
│   ├── OfferStatus.java (NEW)
│   ├── ContractStatus.java (NEW)
│   ├── MilestoneStatus.java (NEW)
│   ├── Offer.java (NEW)
│   ├── Contract.java (NEW)
│   ├── Milestone.java (NEW)
│   ├── Deliverable.java (NEW)
│   └── Escrow.java (NEW)
├── repository/
│   ├── OfferRepository.java (NEW)
│   ├── ContractRepository.java (NEW)
│   ├── MilestoneRepository.java (NEW)
│   ├── DeliverableRepository.java (NEW)
│   └── EscrowRepository.java (NEW)
├── service/
│   ├── ContractService.java (NEW)
│   ├── MilestoneService.java (NEW)
│   └── EscrowService.java (NEW)
├── controller/
│   ├── ContractController.java (NEW)
│   ├── MilestoneController.java (NEW)
│   └── EscrowController.java (NEW)
└── dto/
    └── ContractDtos.java (NEW)
```

### Frontend
```
app/src/
├── types/
│   └── contract.ts (NEW)
├── api/
│   └── contractApi.ts (NEW)
├── pages/
│   ├── contracts/
│   │   ├── ContractDashboard.tsx (NEW)
│   │   ├── MilestoneSubmitForm.tsx (NEW)
│   │   ├── RevisionRequestForm.tsx (NEW)
│   │   └── MilestoneApproveDialog.tsx (NEW)
│   └── offers/
│       └── OfferDetailPage.tsx (MODIFIED or NEW)
├── components/
│   └── AppLayout.tsx (MODIFIED - add contract menu)
└── App.tsx (MODIFIED - add routes)
```

---

**Status**: ✅ UPDATED - Contract feature now bám đúng khung `Contract và Milestone` trong tài liệu phân tích thiết kế
