# Contract Feature - Quick Start Guide

## 🚀 Getting Started

### Backend Setup

1. **Build contract-service**
   ```bash
   cd /home/sontung/backend
   mvn clean install -pl services/contract-service
   ```

2. **Run contract-service**
   ```bash
   mvn spring-boot:run -pl services/contract-service
   # Service runs on http://localhost:8084
   ```

3. **Verify API**
   ```bash
   curl http://localhost:8084/api/contracts
   # Should return empty list or auth error
   ```

### Frontend Setup

1. **Install dependencies** (if needed)
   ```bash
   cd /home/sontung/frontend/app
   npm install
   ```

2. **Set environment variables**
   ```bash
   # .env.local or in vite.config.ts
   VITE_CONTRACT_API_BASE_URL=http://localhost:8084
   ```

3. **Run frontend**
   ```bash
   npm run dev
   # Frontend runs on http://localhost:5173
   ```

---

## 🧪 Testing Workflow

### Test 1: Create Contract from Offer

**Backend API Call**
```bash
curl -X POST http://localhost:8084/api/contracts/from-offer/{offerId} \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json"
```

**Expected Response**
```json
{
  "id": "contract_123",
  "jobId": "job_456",
  "offerId": "offer_789",
  "employerId": "emp_001",
  "freelancerId": "free_001",
  "totalValue": 1000,
  "status": "ACTIVE",
  "startDate": "2026-05-22T10:00:00",
  "endDate": null,
  "createdAt": "2026-05-22T10:00:00"
}
```

### Test 2: Get Contract Details

```bash
curl http://localhost:8084/api/contracts/{contractId} \
  -H "Authorization: Bearer {token}"
```

### Test 3: Get Contract Milestones

```bash
curl http://localhost:8084/api/contracts/{contractId}/milestones \
  -H "Authorization: Bearer {token}"
```

### Test 4: Submit Milestone

```bash
curl -X POST http://localhost:8084/api/milestones/{milestoneId}/submit \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "fileUrl": "https://example.com/file.zip",
    "linkUrl": "https://github.com/user/project",
    "description": "Project completed as per requirements"
  }'
```

### Test 5: Request Revision

```bash
curl -X POST http://localhost:8084/api/milestones/{milestoneId}/revision \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "revisionDesc": "Please update the header style and fix the button alignment"
  }'
```

### Test 6: Approve Milestone

```bash
curl -X POST http://localhost:8084/api/milestones/{milestoneId}/approve \
  -H "Authorization: Bearer {token}"
```

---

## 🖥️ Frontend Testing

### User Flow 1: Freelancer Perspective

1. Navigate to `/freelancer/contracts`
   - See list of contracts
   
2. Click on a contract
   - View `/contracts/{contractId}`
   - See all milestones
   
3. Click on a milestone
   - View `/contracts/{contractId}/milestones/{milestoneId}`
   - See "Submit Deliverable" button (if status = IN_PROGRESS)
   
4. Click "Submit Deliverable"
   - Navigate to `/contracts/{contractId}/milestones/{milestoneId}/submit`
   - Fill form (fileUrl or linkUrl required)
   - Submit
   
5. After submission
   - Milestone status → SUBMITTED
   - Wait for employer feedback

### User Flow 2: Employer Perspective

1. Navigate to `/employer/contracts`
   - See list of contracts
   
2. Click on a contract
   - View `/contracts/{contractId}`
   - See all milestones
   
3. Click on a milestone with status SUBMITTED
   - View `/contracts/{contractId}/milestones/{milestoneId}`
   - See "Request Revision" and "Approve & Release Payment" buttons
   
4. Option A: Request Revision
   - Click "Request Revision"
   - Navigate to `/contracts/{contractId}/milestones/{milestoneId}/revision`
   - Fill revision details
   - Submit
   - Milestone status → IN_PROGRESS
   
5. Option B: Approve
   - Click "Approve & Release Payment"
   - See confirmation dialog
   - Confirm to approve
   - Milestone status → APPROVED

---

## 🔍 Debugging Tips

### Backend Issues

**Contract not created after accept offer**
- Check: Offer status was PENDING
- Check: Offer not expired (expiresAt > now)
- Check: MongoDB connection
- Check: Logs for exceptions

**Milestone not submitted**
- Check: Milestone status is IN_PROGRESS
- Check: fileUrl or linkUrl provided
- Check: Request body format correct

**Revision count not working**
- Check: revisionCount not already at maxRevisions
- Check: Milestone status is SUBMITTED
- Check: Request body has revisionDesc

### Frontend Issues

**Contract list empty**
- Check: API returning correct user ID
- Check: Network request succeeds (check Network tab)
- Check: Backend running on correct port

**Can't submit deliverable**
- Check: Milestone status displays as "IN_PROGRESS"
- Check: Form validation (need fileUrl or linkUrl)
- Check: Authorization token valid

**Dialog not showing for approve**
- Check: Milestone status is "SUBMITTED"
- Check: User role is EMPLOYER
- Check: Click event listener attached

---

## 📊 Database Schema (MongoDB)

### offers collection
```json
{
  "_id": ObjectId,
  "offerId": "string",
  "jobId": "string",
  "proposalId": "string",
  "employerId": "string",
  "freelancerId": "string",
  "jobDescription": "string",
  "contractValue": number,
  "estimatedDuration": "string",
  "status": "PENDING|ACCEPTED|DECLINED|EXPIRED",
  "expiresAt": ISODate,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

### contracts collection
```json
{
  "_id": ObjectId,
  "contractId": "string",
  "jobId": "string",
  "offerId": "string",
  "employerId": "string",
  "freelancerId": "string",
  "totalValue": number,
  "startDate": ISODate,
  "endDate": ISODate,
  "status": "ACTIVE|COMPLETED|CANCELLED",
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

### milestones collection
```json
{
  "_id": ObjectId,
  "milestoneId": "string",
  "contractId": "string",
  "title": "string",
  "description": "string",
  "amount": number,
  "dueDate": ISODate,
  "status": "NOT_STARTED|IN_PROGRESS|SUBMITTED|APPROVED|DISPUTED",
  "revisionCount": number,
  "maxRevisions": number,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

### deliverables collection
```json
{
  "_id": ObjectId,
  "deliverableId": "string",
  "milestoneId": "string",
  "fileUrl": "string",
  "linkUrl": "string",
  "description": "string",
  "submittedAt": ISODate,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

### escrows collection
```json
{
  "_id": ObjectId,
  "escrowId": "string",
  "milestoneId": "string",
  "amount": number,
  "isFrozen": boolean,
  "lockedAt": ISODate,
  "releasedAt": ISODate,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

---

## 🐛 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| 404 Contract not found | Wrong contractId | Verify ID from list endpoint |
| 400 Offer not PENDING | Already accepted/expired | Check offer status first |
| 400 Max revisions reached | Exceeded revision limit | Contact employer for approval |
| 401 Unauthorized | Invalid/expired token | Refresh token via login |
| 500 Database error | MongoDB connection | Check MongoDB running |
| CORS error | Frontend/backend port mismatch | Check VITE_CONTRACT_API_BASE_URL |

---

## ✅ Checklist Before Merging

- [ ] Backend test: All API endpoints working
- [ ] Frontend test: Freelancer workflow complete
- [ ] Frontend test: Employer workflow complete
- [ ] Database: Verify all collections created
- [ ] Error handling: Proper error messages shown
- [ ] Authorization: Only authorized users can access
- [ ] No console errors in browser DevTools
- [ ] No build warnings/errors

---

## 📞 Support

For issues or questions:
1. Check error messages in browser console & server logs
2. Verify API endpoints using curl/Postman
3. Check database collections exist
4. Review documentation in `IMPLEMENTATION_NOTES.md`
