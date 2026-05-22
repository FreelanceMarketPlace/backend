# Proposal System - Implementation Summary

## ✅ Hoàn thành: Proposal Feature cho Freelancer & Employer

### Backend Implementation (Java Spring Boot)

#### 1. **Domain Layer**
- **ProposalStatus.java**: Enum với 4 trạng thái
  - `PENDING` - Đã submit, chờ employer phản hồi
  - `ACCEPTED` - Employer chấp nhận
  - `REJECTED` - Employer từ chối
  - `WITHDRAWN` - Freelancer rút lại
# Proposal và Offer System - Implementation Summary

## ✅ Mục tiêu: Bám sát tài liệu phân tích thiết kế

Tài liệu này mô tả cụm use case `Apply và Matching` trong [Lớp 6 - Nhóm 11.txt](Lớp%206%20-%20Nhóm%2011.txt): Freelancer gửi Proposal để ứng tuyển job, Employer xem và quản lý proposal, rồi gửi Offer cho freelancer phù hợp. Offer được freelancer Accept/Decline; nếu Accept thì mới đi tiếp sang luồng Contract.

### Backend Implementation (Java Spring Boot)

#### 1. **Domain Layer**
- **ProposalStatus.java**: Enum với 4 trạng thái
  - `PENDING` - Đã submit, chờ Employer xem xét
  - `SHORTLISTED` - Employer đánh dấu ứng viên phù hợp
  - `REJECTED` - Employer từ chối proposal
  - `WITHDRAWN` - Freelancer rút lại proposal

- **Proposal.java**: Entity MongoDB với fields:
  - `id`: MongoDB ObjectId
  - `jobId`: Reference đến job (indexed)
  - `freelancerId`: Reference đến freelancer (indexed)
  - `jobTitle`: Title của job (để hiển thị nhanh)
  - `coverLetter`: Nội dung proposal / mô tả phù hợp với job
  - `bidAmount`: Số tiền freelancer đề xuất
  - `estimatedDuration`: Thời gian hoàn thành dự kiến
  - `status`: Trạng thái proposal
  - `createdAt`, `updatedAt`, `respondedAt`: Timestamps
  - **Compound indexes**: Đảm bảo `(jobId, freelancerId)` unique + performance tốt

- **OfferStatus.java**: Enum với 4 trạng thái
  - `PENDING` - Offer đã gửi, chờ freelancer phản hồi
  - `ACCEPTED` - Freelancer chấp nhận offer
  - `DECLINED` - Freelancer từ chối offer
  - `EXPIRED` - Offer hết hạn

- **Offer.java**: Entity MongoDB phản ánh bảng `offers`
  - `offerId`, `jobId`, `proposalId`
  - `employerId`, `freelancerId`
  - `jobDescription`, `contractValue`, `estimatedDuration`
  - `status`, `expiresAt`, `createdAt`

#### 2. **Repository Layer**
- **ProposalRepository**: Spring Data MongoDB
  - `existsByJobIdAndFreelancerId()` - Check duplicate proposal
  - `findByJobId()` - Lấy proposals của 1 job
  - `findByJobIdAndStatus()` - Filter by status
  - `findByFreelancerId()` - Proposals của freelancer
  - `countByJobIdAndStatus()` - Đếm proposals để hiển thị badge trên job card

- **OfferRepository**: Spring Data MongoDB
  - `findByProposalId()` - Tìm offer được tạo từ proposal đã shortlist
  - `findByFreelancerId()` - Lấy offer của freelancer
  - `findByJobId()` - Tra cứu offer theo job
  - `existsByProposalId()` - Đảm bảo một proposal chỉ sinh một offer

#### 3. **Service Layer**
- **ProposalService**: Business logic
  - `submitProposal()` - Freelancer gửi proposal (validate job tồn tại, job Open, không duplicate)
  - `getJobProposals()` - Employer xem proposal của job mình đăng
  - `getFreelancerProposals()` - Freelancer xem own proposals
  - `shortlistProposal()` - Employer đánh dấu proposal phù hợp để gửi Offer
  - `rejectProposal()` - Employer reject
  - `withdrawProposal()` - Freelancer withdraw pending proposal
  - Auto-increment `job.proposalCount` khi submit và update khi withdraw

- **OfferService**: Business logic cho Offer
  - `createOfferFromProposal()` - Tạo offer từ proposal đã shortlist
  - `getReceivedOffers()` - Freelancer xem các offer nhận được
  - `acceptOffer()` - Freelancer chấp nhận offer
  - `declineOffer()` - Freelancer từ chối offer
  - `expireOffer()` - Hết hạn offer theo `expiresAt`

#### 4. **Controller & DTOs**
- **ProposalController**: REST endpoints
  ```
  POST   /jobs/{jobId}/proposals                    - Submit proposal
  GET    /jobs/{jobId}/proposals?status=PENDING     - View job proposals (employer)
  GET    /freelancer/proposals?status=PENDING       - View own proposals (freelancer)
  POST   /proposals/{proposalId}/shortlist          - Shortlist proposal (employer)
  POST   /proposals/{proposalId}/reject             - Reject proposal (employer)
  POST   /proposals/{proposalId}/withdraw           - Withdraw proposal (freelancer)
  ```

- **OfferController**: REST endpoints
  ```
  POST   /proposals/{proposalId}/offers              - Create offer from shortlisted proposal
  GET    /freelancer/offers                          - View received offers
  POST   /offers/{offerId}/accept                    - Accept offer (freelancer)
  POST   /offers/{offerId}/decline                   - Decline offer (freelancer)
  ```

- **ProposalDtos.java**: Request/Response DTOs
  ```java
  SubmitProposalRequest {
    bidAmount: BigDecimal (>0),
    coverLetter: String (10-500 chars),
    estimatedDuration: Integer (>0)
  }

  ProposalResponse {
    id, jobId, jobTitle, freelancerId,
    bidAmount, coverLetter, estimatedDuration, status,
    createdAt, updatedAt, respondedAt
  }

  OfferResponse {
    offerId, jobId, proposalId,
    employerId, freelancerId,
    jobDescription, contractValue, estimatedDuration,
    status, expiresAt, createdAt
  }
  ```

---

### Frontend Implementation (React + TypeScript)

#### 1. **Types** (`types/proposal.ts`)
```typescript
type ProposalStatus = 'PENDING' | 'SHORTLISTED' | 'REJECTED' | 'WITHDRAWN'
type OfferStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED'

interface SubmitProposalRequest {
  bidAmount: number
  coverLetter: string
  estimatedDuration: number
}

interface ProposalResponse {
  id: string
  jobId: string
  jobTitle: string
  freelancerId: string
  bidAmount: number
  coverLetter: string
  estimatedDuration: number
  status: ProposalStatus
  createdAt: string
  updatedAt: string
  respondedAt: string | null
}

interface OfferResponse {
  offerId: string
  jobId: string
  proposalId: string
  employerId: string
  freelancerId: string
  jobDescription: string
  contractValue: number
  estimatedDuration: number
  status: OfferStatus
  expiresAt: string
  createdAt: string
}
```

#### 2. **API Layer** (`api/proposalApi.ts`)
- Proposal APIs: submit, getJobProposals, getFreelancerProposals, shortlist, reject, withdraw
- Offer APIs: create from proposal, get received offers, accept, decline
- Lỗi handling & axios integration

#### 3. **UI Components**

**a) JobDetailPage.tsx (Enhanced)**
- Freelancer nhìn thấy button "Submit Proposal" khi job status = OPEN
- Modal form với:
  - Bid Amount input (number, min 0)
  - Cover letter textarea (10-500 chars)
  - Estimated duration input (days)
  - Error display & loading state
- Hiển thị proposal count trên job card
- Auto-refresh job data sau khi submit

**b) FreelancerProposalsPage.tsx (New)**
- Dashboard cho freelancer xem tất cả proposals
- Filter by status: ALL, PENDING, SHORTLISTED, REJECTED, WITHDRAWN
- Hiển thị:
  - Job title, bid amount, cover letter snippet
  - Status color-coded
  - Created date & responded date
- Pagination (10 items/page)
- Withdraw button cho PENDING proposals
- Status colors: blue (pending), amber (shortlisted), red (rejected), gray (withdrawn)

**c) EmployerJobsPage.tsx (Enhanced)**
- Mỗi job card có button "Proposals (n)" để xem proposals
- Modal popup hiển thị:
  - Freelancer ID, bid amount, cover letter
  - Status (color-coded)
  - Shortlist/Reject buttons cho PENDING proposals
  - Nút Create Offer cho proposal đã shortlist
- Offer flow: tạo offer từ proposal phù hợp rồi freelancer Accept/Decline

#### 4. **Navigation Updates**
- **AppLayout.tsx**: Thêm link "My Proposals" cho FREELANCER role
- **App.tsx**: Route mới `/freelancer/proposals` với ProtectedRoute
- **AppLayout.tsx**: Thêm link "My Offers" cho FREELANCER role
- **App.tsx**: Route mới `/freelancer/offers` với ProtectedRoute

---

## 🔄 Workflow

### Freelancer Flow
1. Browse jobs: `GET /jobs`
2. View job detail: `GET /jobs/{jobId}`
3. Submit proposal: `POST /jobs/{jobId}/proposals` (modal form)
4. View my proposals: `/freelancer/proposals`
5. Filter & check status
6. Withdraw if still PENDING
7. Nhận offer và accept/decline offer ở trang offers

### Employer Flow
1. Create job: `POST /jobs`
2. View employer jobs: `GET /employer/jobs`
3. Click "Proposals (n)" button → modal opens
4. Review freelancer info, bid, cover letter và rating
5. Shortlist / Reject proposal
6. Create Offer từ proposal phù hợp
7. Có thể tạo nhiều offer pending cho các ứng viên shortlist, nhưng chỉ offer đầu tiên được accept mới chốt job
8. Khi 1 freelancer accept thành công, hệ thống chuyển job sang `IN_PROGRESS` và tự động expire các offer pending còn lại của job đó
9. Chờ freelancer Accept/Decline offer để đi sang Contract

---

## 🎯 Key Features

✅ **Validation**
- Freelancer không thể propose 2 lần cho 1 job
- Job phải OPEN để nhận proposals
- Proposal cover letter 10-500 chars, bid > 0, estimated duration > 0
- Employer phải là job owner để view/shortlist/reject

✅ **Business Logic**
- Proposal count tự động increment/decrement theo submit/withdraw
- Timestamps: createdAt, updatedAt, respondedAt (khi employer react)
- Employer shortlist proposal rồi tạo offer
- Offer accept → contract sẽ được tạo ở luồng contract
- Chỉ 1 offer có thể thắng cuộc cho mỗi job; offer accept đầu tiên sẽ khóa job và làm hết hạn các offer pending còn lại

✅ **UX**
- Modal forms (cleaner than page navigation)
- Status color-coding (quick visual feedback)
- Loading states & error handling
- Pagination cho large datasets
- Responsive design

---

## 📊 Data Model Relationship

```
Job (employer-owned)
├── proposalCount (int, auto-increment)
└── Proposals[] (1-to-many)
    ├── Proposal (freelancer-owned)
    │   ├── jobId (reference to Job)
    │   ├── freelancerId
    │   ├── bidAmount
    │   ├── coverLetter
    │   ├── estimatedDuration
    │   ├── status (PENDING → SHORTLISTED/REJECTED/WITHDRAWN)
    │   └── timestamps
    └── Offer[] (proposal được shortlist sẽ sinh offer)
        ├── offerId
        ├── proposalId
        ├── employerId
        ├── freelancerId
        └── status (PENDING → ACCEPTED/DECLINED/EXPIRED)

Rule xử lý cạnh tranh:
- Nhiều proposal có thể được shortlist và tạo offer.
- Chỉ offer ACCEPTED đầu tiên mới được phép chốt job.
- Các offer PENDING còn lại của cùng job sẽ tự chuyển sang EXPIRED khi job đã được nhận.
```

---

## 🚀 Next Steps (Optional Future Features)

1. **Contract Creation** - Auto-create contract when offer accepted
2. **Notifications** - Email/push when proposal/offer status changes
3. **User Profiles** - Show freelancer rating, skills, portfolio
4. **Job Status Update** - Change job status to IN_PROGRESS when offer accepted
5. **Search Freelancers** - Employer search by skills for direct hiring
6. **Review/Rating** - After contract completion

---

## ✅ Testing Checklist

### Backend
- [ ] Submit proposal - duplicate check
- [ ] Submit proposal - job not found
- [ ] Submit proposal - job not OPEN
- [ ] Shortlist proposal - verify employer
- [ ] Reject proposal - employer only
- [ ] Withdraw proposal - freelancer only
- [ ] Create offer from shortlisted proposal
- [ ] Freelancer accept/decline offer

### Frontend
- [ ] Freelancer submit proposal modal
- [ ] FreelancerProposalsPage pagination
- [ ] EmployerJobsPage proposals modal
- [ ] Offer list page works
- [ ] Status filtering works
- [ ] Error messages display
- [ ] Navigation links work

---

## Files Modified/Created

### Backend
```
services/job-service/src/main/java/com/nhom611/jobsvc/
├── domain/
│   ├── ProposalStatus.java (NEW)
│   ├── OfferStatus.java (NEW)
│   ├── Proposal.java (NEW)
│   └── Offer.java (NEW)
├── repository/
│   ├── ProposalRepository.java (NEW)
│   └── OfferRepository.java (NEW)
├── service/
│   ├── ProposalService.java (NEW)
│   └── OfferService.java (NEW)
├── controller/
│   ├── ProposalController.java (NEW)
│   └── OfferController.java (NEW)
└── dto/
    ├── ProposalDtos.java (NEW)
    └── OfferDtos.java (NEW)
```

### Frontend
```
app/src/
├── types/
│   └── proposal.ts (NEW)
├── api/
│   └── proposalApi.ts (NEW)
├── pages/
│   ├── jobs/
│   │   └── JobDetailPage.tsx (MODIFIED - add modal)
│   ├── freelancer/
│   │   ├── FreelancerProposalsPage.tsx (NEW)
│   │   └── FreelancerOffersPage.tsx (NEW)
│   └── employer/
│       └── EmployerJobsPage.tsx (MODIFIED - add modal)
├── components/
│   └── AppLayout.tsx (MODIFIED - add nav link)
└── App.tsx (MODIFIED - add routes)
```

**Status**: ✅ UPDATED - Proposal feature now bám đúng khung `Apply và Matching` trong tài liệu phân tích thiết kế
