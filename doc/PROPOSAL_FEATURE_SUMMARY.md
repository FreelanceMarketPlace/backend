# Proposal System - Implementation Summary

## ✅ Hoàn thành: Proposal Feature cho Freelancer & Employer

### Backend Implementation (Java Spring Boot)

#### 1. **Domain Layer**
- **ProposalStatus.java**: Enum với 4 trạng thái
  - `PENDING` - Đã submit, chờ employer phản hồi
  - `ACCEPTED` - Employer chấp nhận
  - `REJECTED` - Employer từ chối
  - `WITHDRAWN` - Freelancer rút lại

- **Proposal.java**: Entity MongoDB với fields:
  - `id`: MongoDB ObjectId
  - `jobId`: Reference đến job (indexed)
  - `freelancerId`: Reference đến freelancer (indexed)
  - `jobTitle`: Title của job (để display)
  - `bidAmount`: Số tiền freelancer đề xuất
  - `message`: Mô tả tại sao freelancer phù hợp
  - `status`: Trạng thái proposal
  - `createdAt`, `updatedAt`, `respondedAt`: Timestamps
  - **Compound indexes**: Đảm bảo (jobId, freelancerId) unique + performance tốt

#### 2. **Repository Layer**
- **ProposalRepository**: Spring Data MongoDB
  - `existsByJobIdAndFreelancerId()` - Check duplicate proposal
  - `findByJobId()` - Lấy proposals của 1 job
  - `findByJobIdAndStatus()` - Filter by status
  - `findByFreelancerId()` - Proposals của freelancer
  - `countByJobIdAndStatus()` - Đếm proposals (để limit 1 accepted per job)

#### 3. **Service Layer**
- **ProposalService**: Business logic
  - `submitProposal()` - Freelancer submit proposal (validate job exists, not duplicate, job is open)
  - `getJobProposals()` - Employer view proposals (verify ownership)
  - `getFreelancerProposals()` - Freelancer view own proposals
  - `acceptProposal()` - Employer accept (auto-reject others, prevent multiple accepted)
  - `rejectProposal()` - Employer reject
  - `withdrawProposal()` - Freelancer withdraw pending proposal
  - Auto-increment `job.proposalCount` khi submit

#### 4. **Controller & DTOs**
- **ProposalController**: REST endpoints
  ```
  POST   /jobs/{jobId}/proposals                    - Submit proposal
  GET    /jobs/{jobId}/proposals?status=PENDING     - View job proposals (employer)
  GET    /freelancer/proposals?status=PENDING       - View own proposals (freelancer)
  POST   /proposals/{proposalId}/accept             - Accept proposal (employer)
  POST   /proposals/{proposalId}/reject             - Reject proposal (employer)
  POST   /proposals/{proposalId}/withdraw           - Withdraw proposal (freelancer)
  ```

- **ProposalDtos.java**: Request/Response DTOs
  ```java
  SubmitProposalRequest {
    bidAmount: BigDecimal (>0),
    message: String (10-500 chars)
  }
  
  ProposalResponse {
    id, jobId, jobTitle, freelancerId,
    bidAmount, message, status,
    createdAt, updatedAt, respondedAt
  }
  ```

---

### Frontend Implementation (React + TypeScript)

#### 1. **Types** (`types/proposal.ts`)
```typescript
type ProposalStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN'

interface SubmitProposalRequest {
  bidAmount: number
  message: string
}

interface ProposalResponse {
  id: string
  jobId: string
  jobTitle: string
  freelancerId: string
  bidAmount: number
  message: string
  status: ProposalStatus
  createdAt: string
  updatedAt: string
  respondedAt: string | null
}
```

#### 2. **API Layer** (`api/proposalApi.ts`)
- 6 functions: submit, getJobProposals, getFreelancerProposals, accept, reject, withdraw
- Lỗi handling & axios integration

#### 3. **UI Components**

**a) JobDetailPage.tsx (Enhanced)**
- Freelancer nhìn thấy button "Submit Proposal" khi job status = OPEN
- Modal form với:
  - Bid Amount input (number, min 0)
  - Message textarea (10-500 chars)
  - Error display & loading state
- Hiển thị proposal count trên job card
- Auto-refresh job data sau khi submit

**b) FreelancerProposalsPage.tsx (New)**
- Dashboard cho freelancer xem tất cả proposals
- Filter by status: ALL, PENDING, ACCEPTED, REJECTED, WITHDRAWN
- Hiển thị:
  - Job title, bid amount, message
  - Status color-coded
  - Created date & responded date
- Pagination (10 items/page)
- Withdraw button cho PENDING proposals
- Status colors: blue (pending), green (accepted), red (rejected), gray (withdrawn)

**c) EmployerJobsPage.tsx (Enhanced)**
- Mỗi job card có button "Proposals (n)" để xem proposals
- Modal popup hiển thị:
  - Freelancer ID, bid amount, message
  - Status (color-coded)
  - Accept/Reject buttons cho PENDING proposals
  - Proposals đã được nhận thuê không thể thay đổi
- Accept logic: auto-reject tất cả pending proposals khác

#### 4. **Navigation Updates**
- **AppLayout.tsx**: Thêm link "My Proposals" cho FREELANCER role
- **App.tsx**: Route mới `/freelancer/proposals` với ProtectedRoute

---

## 🔄 Workflow

### Freelancer Flow
1. Browse jobs: `GET /jobs`
2. View job detail: `GET /jobs/{jobId}`
3. Submit proposal: `POST /jobs/{jobId}/proposals` (modal form)
4. View my proposals: Navigate to "/freelancer/proposals"
5. Filter & check status
6. Withdraw if still PENDING

### Employer Flow
1. Create job: `POST /jobs`
2. View employer jobs: `GET /employer/jobs`
3. Click "Proposals (n)" button → modal opens
4. Review freelancer info & bid
5. Accept or Reject: `POST /proposals/{id}/accept|reject`
6. Auto-reject other pending proposals when accept one

---

## 🎯 Key Features

✅ **Validation**
- Freelancer không thể propose 2 lần cho 1 job
- Job phải OPEN để nhận proposals
- Proposal message 10-500 chars, bid > 0
- Employer phải là job owner để view/accept/reject

✅ **Business Logic**
- Khi accept 1 proposal → auto-reject pending others (1 contract per job)
- Proposal count tự động increment/decrement
- Timestamps: createdAt, updatedAt, respondedAt (khi employer react)

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
    │   ├── message
    │   ├── status (PENDING → ACCEPTED/REJECTED/WITHDRAWN)
    │   └── timestamps
```

---

## 🚀 Next Steps (Optional Future Features)

1. **Contract Creation** - Auto-create contract when proposal accepted
2. **Notifications** - Email/push when proposal status changes
3. **User Profiles** - Show freelancer rating, skills, portfolio
4. **Job Status Update** - Change job status to IN_PROGRESS when proposal accepted
5. **Search Freelancers** - Employer search by skills for direct hiring
6. **Review/Rating** - After contract completion

---

## ✅ Testing Checklist

### Backend
- [ ] Submit proposal - duplicate check ✓
- [ ] Submit proposal - job not found ✓
- [ ] Submit proposal - job not OPEN ✓
- [ ] Accept proposal - verify employer ✓
- [ ] Accept proposal - auto-reject others ✓
- [ ] Reject proposal - can't reject accepted ✓
- [ ] Withdraw proposal - freelancer only ✓

### Frontend
- [ ] Freelancer submit proposal modal
- [ ] FreelancerProposalsPage pagination
- [ ] EmployerJobsPage proposals modal
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
│   └── Proposal.java (NEW)
├── repository/
│   └── ProposalRepository.java (NEW)
├── service/
│   └── ProposalService.java (NEW)
├── controller/
│   └── ProposalController.java (NEW)
└── dto/
    └── ProposalDtos.java (NEW)
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
│   │   └── FreelancerProposalsPage.tsx (NEW)
│   └── employer/
│       └── EmployerJobsPage.tsx (MODIFIED - add modal)
├── components/
│   └── AppLayout.tsx (MODIFIED - add nav link)
└── App.tsx (MODIFIED - add route)
```

---

**Status**: ✅ COMPLETE - Ready for testing & deployment
