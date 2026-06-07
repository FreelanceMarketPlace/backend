# Payment Service - API Testing Guide

## 🧪 Postman / cURL Testing

### Prerequisites
- MongoDB running on localhost:27017
- Payment Service running on localhost:8084
- Test User ID: `user123`

---

## 1️⃣ Flow 1: Basic Deposit Flow

### Step 1: Initiate Deposit (Get QR Code)
```bash
curl -X POST http://localhost:8084/api/deposits/initiate \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500000
  }'
```

**Expected Response (201 Created):**
```json
{
  "depositId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "user123",
  "amount": 500000,
  "status": "PENDING",
  "qrCodeUrl": "data:image/svg+xml;base64,PHN2ZyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnIHdpZHRoPScyMDAnIGhlaWdodD0nMjAwJz4...",
  "qrCodeData": "BANK|1234567890123|500000.00|user123",
  "expiresAt": "2026-06-06T10:15:30.123456",
  "createdAt": "2026-06-06T10:00:30.123456"
}
```

**⚠️ Important**: Save the `depositId` from response, use it in next step!

---

### Step 2: Confirm Deposit (User "Scans" QR)
```bash
# Replace {depositId} with the value from Step 1
curl -X POST http://localhost:8084/api/deposits/a1b2c3d4-e5f6-7890-abcd-ef1234567890/confirm \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionRefCode": "REF_12345"
  }'
```

**Expected Response (200 OK):**
```json
{
  "depositId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "user123",
  "amount": 500000,
  "status": "COMPLETED",
  "qrCodeUrl": "data:image/svg+xml;base64,...",
  "qrCodeData": "BANK|1234567890123|500000.00|user123",
  "expiresAt": "2026-06-06T10:15:30.123456",
  "createdAt": "2026-06-06T10:00:30.123456"
}
```

---

### Step 3: Check Wallet Balance
```bash
curl -X GET http://localhost:8084/api/wallets/user123 \
  -H "X-User-Id: user123"
```

**Expected Response (200 OK):**
```json
{
  "walletId": "wallet-uuid-here",
  "userId": "user123",
  "balance": 500000,
  "frozenBalance": 0,
  "totalBalance": 500000,
  "updatedAt": "2026-06-06T10:05:30.123456"
}
```

✅ Balance should increase by 500,000!

---

### Step 4: Check Transaction History
```bash
curl -X GET "http://localhost:8084/api/transactions?page=0&pageSize=10" \
  -H "X-User-Id: user123"
```

**Expected Response (200 OK):**
```json
{
  "transactions": [
    {
      "transactionId": "tx-uuid-1",
      "userId": "user123",
      "type": "DEPOSIT",
      "status": "COMPLETED",
      "amount": 500000,
      "description": "Nạp tiền thành công",
      "createdAt": "2026-06-06T10:05:30.123456"
    },
    {
      "transactionId": "tx-uuid-2",
      "userId": "user123",
      "type": "DEPOSIT",
      "status": "PENDING",
      "amount": 500000,
      "description": "Nạp tiền vào ví (chờ xác nhận)",
      "createdAt": "2026-06-06T10:00:30.123456"
    }
  ],
  "total": 2,
  "page": 0,
  "pageSize": 10
}
```

✅ Should see 2 transactions: PENDING (initiate) + COMPLETED (confirm)

---

## 2️⃣ Flow 2: Error Scenarios

### Error 1: Invalid Amount
```bash
curl -X POST http://localhost:8084/api/deposits/initiate \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": -1000
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "Số tiền phải lớn hơn 0",
  "timestamp": "2026-06-06T10:10:00",
  "status": 400
}
```

---

### Error 2: Amount Exceeds Limit
```bash
curl -X POST http://localhost:8084/api/deposits/initiate \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 200000000
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "Số tiền vượt quá giới hạn",
  "timestamp": "2026-06-06T10:10:00",
  "status": 400
}
```

---

### Error 3: Deposit Expired
```bash
# Wait 15 minutes, then try to confirm
curl -X POST http://localhost:8084/api/deposits/a1b2c3d4-e5f6-7890-abcd-ef1234567890/confirm \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionRefCode": "REF_12345"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "Mã QR đã hết hạn, vui lòng tạo mới",
  "timestamp": "2026-06-06T10:16:00",
  "status": 400
}
```

---

### Error 4: Invalid Ref Code
```bash
curl -X POST http://localhost:8084/api/deposits/a1b2c3d4-e5f6-7890-abcd-ef1234567890/confirm \
  -H "X-User-Id: user123" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionRefCode": "INVALID"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "Reference code không hợp lệ",
  "timestamp": "2026-06-06T10:10:00",
  "status": 400
}
```

---

## 3️⃣ Flow 3: Filter Transactions

### Get Only DEPOSIT Transactions
```bash
curl -X GET "http://localhost:8084/api/transactions/filter?type=DEPOSIT&page=0&pageSize=10" \
  -H "X-User-Id: user123"
```

**Expected Response (200 OK):**
```json
{
  "transactions": [
    {
      "transactionId": "tx-uuid-1",
      "type": "DEPOSIT",
      "status": "COMPLETED",
      "amount": 500000,
      ...
    }
  ],
  "total": 1,
  "page": 0,
  "pageSize": 10
}
```

---

## 4️⃣ Flow 4: Multiple Users

### Test with Different User
```bash
# User 2 deposit
curl -X POST http://localhost:8084/api/deposits/initiate \
  -H "X-User-Id: user456" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000000
  }'

# Should create separate wallet and deposit for user456
```

---

## 🧬 Automated Testing Script

### Using Bash
```bash
#!/bin/bash

USER_ID="user_$(date +%s)"
AMOUNT=500000

echo "Testing Payment Service..."
echo "User: $USER_ID"
echo ""

# Step 1: Initiate
echo "1️⃣  Initiating deposit..."
DEPOSIT_RESPONSE=$(curl -s -X POST http://localhost:8084/api/deposits/initiate \
  -H "X-User-Id: $USER_ID" \
  -H "Content-Type: application/json" \
  -d "{\"amount\": $AMOUNT}")

DEPOSIT_ID=$(echo $DEPOSIT_RESPONSE | grep -o '"depositId":"[^"]*' | cut -d'"' -f4)
echo "Deposit ID: $DEPOSIT_ID"
echo ""

# Step 2: Confirm
echo "2️⃣  Confirming deposit..."
CONFIRM_RESPONSE=$(curl -s -X POST http://localhost:8084/api/deposits/$DEPOSIT_ID/confirm \
  -H "X-User-Id: $USER_ID" \
  -H "Content-Type: application/json" \
  -d '{"transactionRefCode": "REF_12345"}')

echo "Confirm Response: $CONFIRM_RESPONSE"
echo ""

# Step 3: Check Wallet
echo "3️⃣  Checking wallet..."
curl -s http://localhost:8084/api/wallets/$USER_ID \
  -H "X-User-Id: $USER_ID" | jq .

echo ""
echo "4️⃣  Checking transactions..."
curl -s "http://localhost:8084/api/transactions?page=0&pageSize=10" \
  -H "X-User-Id: $USER_ID" | jq .

echo ""
echo "✅ Test completed!"
```

**Run:**
```bash
chmod +x test_payment.sh
./test_payment.sh
```

---

## 📊 QR Code Visualization

The QR code returned is a base64-encoded SVG. To view it:

1. **In Browser Console:**
```javascript
const qrUrl = "data:image/svg+xml;base64,..."; // from API response
window.open(qrUrl);
```

2. **In Frontend:**
```jsx
<img src={deposit.qrCodeUrl} alt="QR Code" />
```

3. **Save as File:**
```bash
# Copy the qrCodeUrl value and decode
echo "PHN2ZyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnIC4uLj48L3N2Zz4=" | base64 -d > qr.svg
open qr.svg
```

---

## 🔍 MongoDB Inspection

Check what's in the database:

```bash
# Connect to MongoDB
mongo mongodb://localhost:27017/nhom611

# Check wallets
db.wallets.find()

# Check deposits
db.deposits.find()

# Check transactions
db.transactions.find().pretty()

# Find user's transactions
db.transactions.find({"userId": "user123"})

# Count collections
db.wallets.countDocuments()
db.deposits.countDocuments()
db.transactions.countDocuments()
```

---

## ✅ Checklist for Full Testing

- [ ] Initiate deposit returns QR code
- [ ] QR code URL is valid (base64)
- [ ] Confirm deposit with valid ref code
- [ ] Wallet balance updated
- [ ] Transactions logged correctly
- [ ] Error: invalid amount
- [ ] Error: amount too large
- [ ] Error: QR expired (wait 15 min)
- [ ] Error: invalid ref code
- [ ] Multiple users have separate wallets
- [ ] Transaction filtering by type works
- [ ] Pagination works (page=0, pageSize=10)

---

## 🚀 Load Testing (Optional)

```bash
# Install Apache Bench
apt-get install apache2-utils

# Test 100 requests, 10 concurrent
ab -n 100 -c 10 http://localhost:8084/api/wallets/user123
```

---

**Last Updated**: 2026-06-06
**Status**: Ready for manual and automated testing
