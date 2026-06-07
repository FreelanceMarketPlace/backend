# Payment Service - Quick Reference

## 🎯 What We Built

### Nạp Tiền (Deposit) Feature - COMPLETE ✅

**3-step process:**
1. User click "Nạp Tiền" → Input amount → Get QR code
2. User scan QR (mock: enter ref code) → Confirm
3. Success → Wallet updated → Transaction logged

---

## 📡 API Quick Reference

### Initiate Deposit
```bash
POST http://localhost:8084/api/deposits/initiate
Headers: X-User-Id: user123
Body: { "amount": 500000 }

Response:
{
  "depositId": "abc-123",
  "qrCodeUrl": "data:image/svg+xml;base64,..."  ← Show this QR
  "status": "PENDING",
  "expiresAt": "2026-06-06T10:15:00"
}
```

### Confirm Deposit
```bash
POST http://localhost:8084/api/deposits/abc-123/confirm
Headers: X-User-Id: user123
Body: { "transactionRefCode": "REF_12345" }

Response:
{
  "depositId": "abc-123",
  "status": "COMPLETED",
  "amount": 500000
}
```

### Get Wallet
```bash
GET http://localhost:8084/api/wallets/user123
Headers: X-User-Id: user123

Response:
{
  "walletId": "wallet-123",
  "balance": 500000,
  "frozenBalance": 1000000,
  "totalBalance": 1500000
}
```

### Get Transaction History
```bash
GET http://localhost:8084/api/transactions?page=0&pageSize=10
Headers: X-User-Id: user123

Response:
{
  "transactions": [
    {
      "transactionId": "tx-123",
      "type": "DEPOSIT",
      "status": "COMPLETED",
      "amount": 500000,
      "description": "Nạp tiền thành công",
      "createdAt": "2026-06-06T10:05:00"
    }
  ],
  "total": 1,
  "page": 0,
  "pageSize": 10
}
```

---

## 🏗️ Backend Services Overview

### WalletService
```java
// Get or create wallet
Wallet wallet = walletService.getOrCreateWallet(userId);

// Add money (deposit confirmed)
walletService.addBalance(userId, 500000);

// Lock money (fund escrow)
walletService.lockBalance(userId, 500000);

// Unlock money (refund)
walletService.unlockBalance(userId, 500000);
```

### DepositService
```java
// Step 1: Generate QR
PaymentDtos.DepositResponse response = 
  depositService.initiateDeposit(userId, 500000);

// Step 2: Confirm (user scanned QR)
PaymentDtos.DepositResponse result = 
  depositService.confirmDeposit(userId, depositId, "REF_12345");
```

### TransactionService
```java
// Create transaction log
transactionService.createTransaction(
  userId, walletId,
  TransactionType.DEPOSIT,
  TransactionStatus.COMPLETED,
  500000,
  "Nạp tiền thành công"
);

// Get history
PaymentDtos.TransactionHistoryResponse history =
  transactionService.getTransactionHistory(userId, 0, 10);
```

---

## 📊 Data Flow

```
Employer → Frontend (WalletPage)
  ↓
Click "Nạp Tiền"
  ↓
POST /deposits/initiate {amount: 500000}
  ↓
Backend: DepositService.initiateDeposit()
  ├── Validate amount
  ├── Get wallet
  ├── Generate QR code (SVG base64)
  ├── Create Deposit (PENDING)
  ├── Create Transaction (PENDING)
  └── Return QR code URL
  ↓
Frontend: Show QR code + ref code input
  ↓
Enter ref code (mock)
  ↓
POST /deposits/{id}/confirm {transactionRefCode: "REF_12345"}
  ↓
Backend: DepositService.confirmDeposit()
  ├── Check deposit (PENDING + not expired)
  ├── Validate ref code
  ├── Update wallet: balance += 500000
  ├── Update deposit: COMPLETED
  ├── Update transaction: COMPLETED
  └── Return success
  ↓
Frontend: Show success message
  ↓
Auto-refresh: GET /wallets/{userId}
  ↓
Balance updated ✓
Transaction appears in history ✓
```

---

## 🎨 Frontend Components

### WalletPage.tsx
Located: `/home/sontung/frontend/app/src/pages/employer/WalletPage.tsx`

**State Variables:**
```typescript
const [wallet, setWallet] = useState<Wallet | null>(null);
const [currentDeposit, setCurrentDeposit] = useState<Deposit | null>(null);
const [depositStep, setDepositStep] = useState<'form' | 'qr' | 'confirm'>('form');
const [depositAmount, setDepositAmount] = useState<number>(0);
const [refCode, setRefCode] = useState<string>('');
const [transactions, setTransactions] = useState<Transaction[]>([]);
```

**Main Functions:**
```typescript
loadWalletData()           // Fetch wallet + transactions
handleInitiateDeposit()    // Call API to get QR
handleConfirmDeposit()     // Confirm with ref code
handleCloseDeposit()       // Close modal
```

---

## 💾 Database Collections

### wallets
```json
{
  "_id": ObjectId,
  "walletId": "string (UUID)",
  "userId": "string (FK → users)",
  "balance": number (khả dụng),
  "frozenBalance": number (bị khoá),
  "createdAt": Date,
  "updatedAt": Date
}
```

### deposits
```json
{
  "_id": ObjectId,
  "depositId": "string (UUID)",
  "userId": "string",
  "walletId": "string",
  "amount": number,
  "status": "PENDING|COMPLETED|FAILED|EXPIRED",
  "qrCodeUrl": "data:image/svg+xml;base64,...",
  "qrCodeData": "BANK|123456|500000|userid",
  "expiresAt": Date,
  "transactionRefCode": "string (optional)",
  "completedAt": Date,
  "createdAt": Date,
  "updatedAt": Date
}
```

### transactions
```json
{
  "_id": ObjectId,
  "transactionId": "string (UUID)",
  "userId": "string",
  "walletId": "string",
  "type": "DEPOSIT|ESCROW_LOCK|MILESTONE_RELEASE|...",
  "status": "PENDING|COMPLETED|FAILED",
  "amount": number,
  "description": "string",
  "relatedMilestoneId": "string (optional)",
  "relatedContractId": "string (optional)",
  "relatedEscrowId": "string (optional)",
  "relatedDepositId": "string (optional)",
  "createdAt": Date,
  "updatedAt": Date
}
```

---

## ⚙️ Configuration

### application.yml
```yaml
spring:
  application:
    name: payment-service

server:
  port: 8084

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/nhom611
```

### API Base URL (Frontend)
```typescript
// In http.ts or env config
const PAYMENT_API_BASE = 'http://localhost:8084';
```

---

## 🧪 Testing

### 1. Initiate Deposit (Get QR)
```bash
curl -X POST http://localhost:8084/api/deposits/initiate \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{"amount": 500000}'
```

### 2. Confirm Deposit
```bash
curl -X POST http://localhost:8084/api/deposits/abc-123/confirm \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{"transactionRefCode": "REF_12345"}'
```

### 3. Check Balance
```bash
curl http://localhost:8084/api/wallets/user123 \
  -H "X-User-Id: user123"
```

---

## 🔗 Integration Points

### With Contract Service (Next Feature: Fund Escrow)
```
Employer clicks "Fund Milestone"
  ↓
Contract Service validates milestone
  ↓
Calls: Payment Service → WalletService.lockBalance()
  ├── Deduct from balance
  ├── Add to frozenBalance
  └── Create Transaction (ESCROW_LOCK)
  ↓
Create Escrow record
  ↓
Freelancer can now work on milestone
```

### With Notification Service
```
Deposit initiated → Send notification with QR
Deposit completed → Send success notification
Deposit failed → Send error notification
```

---

## ⚠️ Important Notes

1. **QR Expiration**: 15 minutes
2. **Max Deposit**: 100,000,000 VND
3. **Transaction Log**: Immutable (cannot edit/delete)
4. **Balance vs Frozen**: 
   - **balance**: Can be used for new deposits
   - **frozenBalance**: Locked in escrow, only released on milestone approval
5. **Mock vs Real**:
   - Mock: Simple ref code input (for testing)
   - Real: Will integrate Stripe/Momo/VNPay callback

---

## 📂 Key Files

**Backend**
- Entities: `/services/payment-service/src/main/java/com/nhom611/paymentsvc/domain/`
- Services: `/services/payment-service/src/main/java/com/nhom611/paymentsvc/service/`
- Controllers: `/services/payment-service/src/main/java/com/nhom611/paymentsvc/controller/`
- DTOs: `/services/payment-service/src/main/java/com/nhom611/paymentsvc/dto/PaymentDtos.java`

**Frontend**
- API: `/frontend/app/src/api/paymentApi.ts`
- Types: `/frontend/app/src/types/payment.ts`
- UI: `/frontend/app/src/pages/employer/WalletPage.tsx`

**Docs**
- Service Guide: `/backend/doc/PAYMENT_SERVICE_GUIDE.md`
- Implementation Details: `/backend/doc/PAYMENT_DEPOSIT_IMPLEMENTATION.md`
- Visual Summary: `/backend/doc/PAYMENT_VISUAL_SUMMARY.md`

---

## 🚀 Next Steps

1. **Fund Escrow** (Integrate with Contract Service)
2. **Release Escrow** (90% freelancer + 10% platform fee)
3. **Withdrawal** (Freelancer cash out)
4. **Admin Approval** (Withdrawal management)
5. **Real Payment Gateway** (Stripe/Momo integration)

---

**Last Updated**: 2026-06-06
**Status**: ✅ DEPOSIT COMPLETE - READY TO INTEGRATE
