# Payment Service - Implementation Guide

## 📋 Tổng quan

**Payment Service** xử lý toàn bộ các giao dịch tài chính:
- Nạp tiền vào wallet (Deposit)
- Quản lý số dư ví
- Khoá/mở khoá tiền (Escrow)
- Lịch sử giao dịch

## 🏗️ Architecture

```
Controller (REST API)
    ↓
Service (Business Logic)
    ↓
Repository (Data Access)
    ↓
MongoDB (Database)
```

### Entities

1. **Wallet**
   - Ví của user
   - Fields: walletId, userId, balance, frozenBalance

2. **Deposit**
   - Lệnh nạp tiền (mỗi order có QR code)
   - Fields: depositId, userId, amount, status, qrCodeUrl, expiresAt

3. **Transaction**
   - Log bất biến của tất cả giao dịch
   - Fields: transactionId, userId, type, status, amount

## 🔄 Flow Nạp Tiền (Deposit Flow)

```
1. User request nạp tiền
   POST /api/deposits/initiate
   → Return: QR code (data URL)

2. User quét QR (mock: nhập ref code)
   POST /api/deposits/{depositId}/confirm
   → Validate ref code
   → Update wallet: balance += amount
   → Create COMPLETED transaction

3. User xem lịch sử
   GET /api/transactions
   → Return: list of transactions
```

## 📡 API Endpoints

### Wallet APIs

```
GET /api/wallets/{userId}
  Headers: X-User-Id: {userId}
  Response: { walletId, userId, balance, frozenBalance, totalBalance }
```

### Deposit APIs

```
POST /api/deposits/initiate
  Headers: X-User-Id: {userId}
  Body: { amount: number }
  Response: { 
    depositId, 
    amount, 
    status: "PENDING",
    qrCodeUrl: "data:image/svg+xml;base64,..."  ← QR code dạng data URL
    qrCodeData: "BANK|123456789|500000|userid",
    expiresAt: "2026-06-06T10:15:00"
  }

POST /api/deposits/{depositId}/confirm
  Headers: X-User-Id: {userId}
  Body: { transactionRefCode: "REF_12345" }
  Response: { 
    depositId,
    status: "COMPLETED",
    amount,
    completedAt
  }

GET /api/deposits/{depositId}
  Headers: X-User-Id: {userId}
  Response: { depositId, userId, amount, status, ... }
```

### Transaction APIs

```
GET /api/transactions
  Headers: X-User-Id: {userId}
  Params: ?page=0&pageSize=10
  Response: { 
    transactions: [ { transactionId, type, status, amount, createdAt }, ... ],
    total: number,
    page: number,
    pageSize: number
  }

GET /api/transactions/filter
  Headers: X-User-Id: {userId}
  Params: ?type=DEPOSIT&page=0&pageSize=10
  Response: { transactions, total, page, pageSize }
```

## 💡 Key Features

### 1. QR Code Generation
- Mock: SVG-based placeholder
- Production: Integrate với goqr.me / qrserver.com / zxing library
- Format: `data:image/svg+xml;base64,...`

### 2. Transaction Logging
Tất cả giao dịch được ghi log bất biến:
- DEPOSIT - Nạp tiền
- ESCROW_LOCK - Khoá tiền cho milestone
- MILESTONE_RELEASE - Giải ngân
- PLATFORM_FEE - Phí hệ thống (10%)
- WITHDRAWAL_REQUEST - Yêu cầu rút tiền
- DISPUTE_RESOLUTION - Phán quyết tranh chấp

### 3. Wallet Management
- **balance**: Số dư khả dụng
- **frozenBalance**: Tiền bị khoá trong escrow
- **total**: balance + frozenBalance

### 4. Error Handling
- Invalid amount (≤ 0 hoặc > 100M)
- Insufficient balance
- Expired QR code
- Invalid ref code

## 🔗 Integration Points

### Với Contract Service
```
Fund Milestone Flow:
1. Employer call Contract Service: POST /api/contracts/{contractId}/fund-milestone
2. Contract Service call Payment Service: POST /api/wallets/{walletId}/lock
3. Payment Service: lock tiền (balance → frozenBalance)
4. Create Transaction: type=ESCROW_LOCK
```

### Với Notification Service
```
- Deposit initiated → Send notification với QR code
- Deposit confirmed → Send confirmation notification
- Deposit failed → Send error notification
```

## 📊 Database Schema (MongoDB)

### wallets collection
```json
{
  "walletId": "uuid",
  "userId": "uuid",
  "balance": 5000000.00,
  "frozenBalance": 1000000.00,
  "updatedAt": ISODate,
  "createdAt": ISODate
}
```

### deposits collection
```json
{
  "depositId": "uuid",
  "userId": "uuid",
  "walletId": "uuid",
  "amount": 500000.00,
  "status": "PENDING|COMPLETED|FAILED|EXPIRED",
  "qrCodeUrl": "data:image/svg+xml;base64,...",
  "qrCodeData": "BANK|123456789|500000|userid",
  "expiresAt": ISODate,
  "transactionRefCode": "REF_12345",
  "completedAt": ISODate,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

### transactions collection
```json
{
  "transactionId": "uuid",
  "walletId": "uuid",
  "userId": "uuid",
  "type": "DEPOSIT|ESCROW_LOCK|MILESTONE_RELEASE|...",
  "status": "PENDING|COMPLETED|FAILED",
  "amount": 500000.00,
  "relatedMilestoneId": "uuid",
  "relatedContractId": "uuid",
  "relatedEscrowId": "uuid",
  "relatedDepositId": "uuid",
  "description": "Nạp tiền vào ví",
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

## 🧪 Testing Flow

### 1. Create Deposit (Get QR Code)
```bash
curl -X POST http://localhost:8084/api/deposits/initiate \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{"amount": 500000}'
```

Response:
```json
{
  "depositId": "abc-123",
  "userId": "user123",
  "amount": 500000,
  "status": "PENDING",
  "qrCodeUrl": "data:image/svg+xml;base64,...",
  "expiresAt": "2026-06-06T10:15:00"
}
```

### 2. Confirm Deposit (Simulate QR Scan)
```bash
curl -X POST http://localhost:8084/api/deposits/abc-123/confirm \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{"transactionRefCode": "REF_12345"}'
```

### 3. Check Wallet
```bash
curl -X GET http://localhost:8084/api/wallets/user123 \
  -H "X-User-Id: user123"
```

### 4. Check Transaction History
```bash
curl -X GET "http://localhost:8084/api/transactions?page=0&pageSize=10" \
  -H "X-User-Id: user123"
```

## 🚀 Next Steps

1. ✅ Implement Deposit logic
2. ⏳ Implement Fund Escrow (Contract Service integration)
3. ⏳ Implement Release Escrow
4. ⏳ Implement Withdrawal Request
5. ⏳ Implement Admin Approval
6. ⏳ Frontend: Wallet & Deposit UI

## 📝 Notes

- QR code expires sau 15 phút
- Max deposit: 100M VND
- Transaction log là bất biến (không thể sửa/xoá)
- frozenBalance không thể rút được, chỉ có thể được giải ngân khi milestone approve
