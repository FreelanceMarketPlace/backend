# Payment Service - Deposit Flow Implementation Summary

## ✅ Completed Implementation

### Backend (Java Spring Boot)

#### 📁 Project Structure
```
payment-service/src/main/java/com/nhom611/paymentsvc/
├── domain/
│   ├── TransactionType.java      ✅ Enum (DEPOSIT, ESCROW_LOCK, ...)
│   ├── TransactionStatus.java    ✅ Enum (PENDING, COMPLETED, FAILED)
│   ├── DepositStatus.java        ✅ Enum (PENDING, COMPLETED, FAILED, EXPIRED)
│   ├── Wallet.java               ✅ Entity (balance, frozenBalance)
│   ├── Transaction.java          ✅ Entity (transaction log)
│   └── Deposit.java              ✅ Entity (deposit order + QR code)
├── repository/
│   ├── WalletRepository.java     ✅ Find by userId
│   ├── TransactionRepository.java ✅ Find by userId, type, date range
│   └── DepositRepository.java    ✅ Find by depositId, userId, status
├── service/
│   ├── WalletService.java        ✅ Wallet operations (lock/unlock/add/subtract)
│   ├── TransactionService.java   ✅ Transaction logging
│   ├── DepositService.java       ✅ Deposit flow (initiate → confirm)
│   └── QRCodeService.java        ✅ QR code generation (mock + real option)
├── controller/
│   ├── WalletController.java     ✅ GET /api/wallets/{userId}
│   ├── DepositController.java    ✅ POST /deposits/initiate, confirm
│   └── TransactionController.java ✅ GET /transactions, /transactions/filter
├── exception/
│   └── GlobalExceptionHandler.java ✅ Centralized error handling
├── config/
│   └── WebConfig.java            ✅ CORS configuration
└── dto/
    └── PaymentDtos.java          ✅ All request/response DTOs
```

#### 🔄 Deposit Flow Implementation
```
Step 1: Initiate Deposit
POST /api/deposits/initiate
├── Validate amount (> 0, <= 100M)
├── Get or create wallet
├── Generate mock bank account
├── Create QR code (base64 SVG or API)
├── Create Deposit order (status: PENDING)
├── Create Transaction log (type: DEPOSIT, status: PENDING)
└── Return: { depositId, qrCodeUrl, expiresAt, ... }

Step 2: Confirm Deposit
POST /api/deposits/{depositId}/confirm
├── Find deposit order
├── Check status (must be PENDING)
├── Check expiration (< 15 minutes)
├── Validate ref code
├── Update wallet: balance += amount
├── Update deposit: status = COMPLETED
├── Update transaction: status = COMPLETED
└── Return: { depositId, status: "COMPLETED", ... }
```

#### 📊 Database Collections
```
wallets
├── walletId (PK)
├── userId (FK → users)
├── balance (khả dụng)
├── frozenBalance (bị khoá)
└── timestamps

deposits
├── depositId (PK)
├── userId, walletId (FK)
├── amount, status
├── qrCodeUrl, qrCodeData
├── expiresAt (15 phút)
├── transactionRefCode (callback)
└── timestamps

transactions
├── transactionId (PK)
├── userId, walletId (FK)
├── type, status, amount
├── relatedMilestoneId, relatedContractId, relatedEscrowId
├── description
└── timestamps
```

#### 🎯 Key Features
1. ✅ QR Code Generation (mock SVG + ready for real API)
2. ✅ Transaction Logging (bất biến log)
3. ✅ Wallet Balance Management (balance + frozen)
4. ✅ Error Handling (validation, expiration, ref code)
5. ✅ Idempotency (check deposit status before update)

### Frontend (React + TypeScript)

#### 📁 Files Created
```
app/src/
├── api/paymentApi.ts             ✅ API functions
├── types/payment.ts              ✅ TypeScript types
└── pages/employer/
    ├── WalletPage.tsx            ✅ Main wallet component
    └── WalletPage.css            ✅ Professional styling
```

#### 🎨 UI Features
```
WalletPage Component
├── Balance Display Section
│   ├── Available balance
│   ├── Frozen balance
│   └── Total balance
├── Action Buttons
│   ├── + Nạp Tiền (Deposit)
│   └── Rút Tiền (Withdrawal - TODO)
├── Deposit Modal
│   ├── Step 1: Amount input
│   ├── Step 2: QR code display
│   │   └── Mock ref code input
│   └── Step 3: Success confirmation
└── Transaction History Table
    ├── Type, Status, Amount
    ├── Description, Timestamp
    └── Pagination support
```

#### 🎯 UI Flow
```
1. User click "Nạp Tiền" button
   ↓
2. Modal opens → Input amount
   ↓
3. Click "Tiếp tục" → Show QR code
   ↓
4. Enter mock ref code (simulate scan)
   ↓
5. Click "Xác Nhận Thanh Toán"
   ↓
6. Success message → Wallet updates
   ↓
7. Transaction appears in history
```

## 📡 API Endpoints Summary

### Wallet APIs
```
GET /api/wallets/{userId}
├── Headers: X-User-Id: {userId}
└── Response: { walletId, balance, frozenBalance, totalBalance }
```

### Deposit APIs
```
POST /api/deposits/initiate
├── Request: { amount }
├── Response: { depositId, qrCodeUrl, expiresAt }

POST /api/deposits/{depositId}/confirm
├── Request: { transactionRefCode }
├── Response: { depositId, status: "COMPLETED" }

GET /api/deposits/{depositId}
└── Response: { depositId, userId, amount, status, ... }
```

### Transaction APIs
```
GET /api/transactions?page=0&pageSize=10
├── Response: { transactions[], total, page, pageSize }

GET /api/transactions/filter?type=DEPOSIT&page=0&pageSize=10
└── Response: { transactions[], total, page, pageSize }
```

## 🧪 Testing Guide

### 1. Test Deposit Initiation
```bash
curl -X POST http://localhost:8084/api/deposits/initiate \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{"amount": 500000}'

# Response:
{
  "depositId": "abc-123",
  "amount": 500000,
  "status": "PENDING",
  "qrCodeUrl": "data:image/svg+xml;base64,...",
  "expiresAt": "2026-06-06T10:15:00"
}
```

### 2. Test Deposit Confirmation
```bash
curl -X POST http://localhost:8084/api/deposits/abc-123/confirm \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{"transactionRefCode": "REF_12345"}'

# Response:
{
  "depositId": "abc-123",
  "status": "COMPLETED",
  "amount": 500000
}
```

### 3. Check Wallet
```bash
curl http://localhost:8084/api/wallets/user123 \
  -H "X-User-Id: user123"

# Response:
{
  "walletId": "wallet-123",
  "balance": 500000,
  "frozenBalance": 0,
  "totalBalance": 500000
}
```

### 4. Check Transaction History
```bash
curl "http://localhost:8084/api/transactions?page=0&pageSize=10" \
  -H "X-User-Id: user123"

# Response:
{
  "transactions": [
    {
      "transactionId": "tx-123",
      "type": "DEPOSIT",
      "status": "COMPLETED",
      "amount": 500000,
      "description": "Nạp tiền thành công"
    }
  ],
  "total": 1,
  "page": 0,
  "pageSize": 10
}
```

## 🔗 Integration Points (Next Steps)

### 1. With Contract Service (Fund Escrow)
```
Employer: POST /api/contracts/{contractId}/fund-milestone
  ↓
Contract Service: Check contract & milestone
  ↓
Contract Service → Payment Service: Lock wallet
  ↓
Payment Service: 
  ├── Get wallet
  ├── Check balance >= amount
  ├── Lock: balance -= amount, frozenBalance += amount
  ├── Create Escrow (isFrozen = true)
  ├── Create Transaction (type: ESCROW_LOCK)
  └── Return success
```

### 2. With Notification Service
```
Deposit initiated → Send notification with QR code
Deposit confirmed → Send confirmation notification
Deposit failed → Send error notification
```

### 3. With Admin Service (Withdrawal Approval)
```
Freelancer: Request withdrawal
Admin: Approve withdrawal
Payment Service: Release from wallet
```

## 📋 Files Modified/Created

### Backend (13 files)
- ✅ 3 Enums (TransactionType, TransactionStatus, DepositStatus)
- ✅ 3 Domain entities (Wallet, Transaction, Deposit)
- ✅ 3 Repositories (WalletRepository, TransactionRepository, DepositRepository)
- ✅ 4 Services (WalletService, TransactionService, DepositService, QRCodeService)
- ✅ 3 Controllers (WalletController, DepositController, TransactionController)
- ✅ 1 Exception Handler (GlobalExceptionHandler)
- ✅ 1 Config (WebConfig)
- ✅ 1 DTO (PaymentDtos)

### Frontend (3 files)
- ✅ 1 API layer (paymentApi.ts)
- ✅ 1 Types file (payment.ts)
- ✅ 1 Component (WalletPage.tsx + WalletPage.css)

### Documentation
- ✅ PAYMENT_SERVICE_GUIDE.md (API docs + architecture)

## 🚀 Next Features to Implement

1. ⏳ **Fund Escrow** (Contract Service integration)
   - Deduct từ wallet balance
   - Create Escrow (isFrozen = true)
   - Create Transaction (type: ESCROW_LOCK)

2. ⏳ **Release Escrow** (After milestone approval)
   - Release từ frozenBalance
   - 90% → Freelancer wallet
   - 10% → Platform fee
   - Create Transactions

3. ⏳ **Withdrawal Request**
   - Create withdrawal order
   - Admin approval flow
   - Transfer to bank

4. ⏳ **Refund Logic**
   - When contract cancelled
   - Return frozen balance

5. ⏳ **Admin Dashboard**
   - View all transactions
   - Approve/reject withdrawals
   - Generate financial reports

## ⚠️ Notes

- QR code expires sau 15 phút
- Max deposit: 100M VND (config-able)
- Transaction log là bất biến
- Mock ref code: any string >= 5 chars (chỉ để test)
- Production: Integrate thực payment gateway (Stripe, Momo, VNPay)
- Frontend already styled professional (mobile-responsive)

---

**Status**: ✅ Nạp tiền (Deposit) - COMPLETED
**Next**: Fund Escrow integration
