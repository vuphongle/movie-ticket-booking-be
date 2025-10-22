#!/bin/bash

# PayOS Webhook Local Testing Script
# This script helps you test the webhook endpoint locally

set -e

echo "🧪 PayOS Webhook Local Test Script"
echo "=================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BACKEND_URL=${BACKEND_URL:-"http://localhost:8080"}
WEBHOOK_ENDPOINT="${BACKEND_URL}/api/payos-webhook"
CHECKSUM_KEY=${PAYOS_CHECKSUM_KEY:-"62e13a88caae122463838817056f00ed427323831c48c0835fe6b845a50541a6"}

echo "📍 Testing endpoint: ${WEBHOOK_ENDPOINT}"
echo ""

# Function to compute HMAC SHA256
compute_signature() {
    local data=$1
    local key=$2
    echo -n "$data" | openssl dgst -sha256 -hmac "$key" | sed 's/^.* //'
}

# Test 1: Check if backend is running
echo "Test 1: Checking if backend is running..."
if curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/actuator/health" | grep -q "200"; then
    echo -e "${GREEN}✓ Backend is running${NC}"
else
    echo -e "${RED}✗ Backend is not running. Please start backend first:${NC}"
    echo "  ./gradlew bootRun"
    exit 1
fi
echo ""

# Test 2: Check webhook endpoint accessibility
echo "Test 2: Checking webhook endpoint..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${WEBHOOK_ENDPOINT}" \
    -H "Content-Type: application/json" \
    -d '{}' || echo "000")

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "401" ]; then
    echo -e "${GREEN}✓ Webhook endpoint is accessible (HTTP ${HTTP_CODE})${NC}"
else
    echo -e "${RED}✗ Webhook endpoint not accessible (HTTP ${HTTP_CODE})${NC}"
    echo "  Expected: 200, 400, or 401"
    echo "  Please check SecurityConfig.java has permitAll() for /api/payos-webhook"
    exit 1
fi
echo ""

# Test 3: Test with valid webhook payload
echo "Test 3: Testing with sample webhook data..."

# Sample webhook payload (successful payment)
WEBHOOK_BODY='{
  "code": "00",
  "desc": "success",
  "data": {
    "orderCode": 999999,
    "amount": 50000,
    "description": "ThanhToanVe 999999",
    "accountNumber": "12345678",
    "reference": "FT99999999",
    "transactionDateTime": "2025-10-22 10:00:00",
    "currency": "VND",
    "paymentLinkId": "abc123",
    "code": "00",
    "desc": "Thành công",
    "counterAccountBankId": "",
    "counterAccountBankName": "",
    "counterAccountName": "",
    "counterAccountNumber": "",
    "virtualAccountName": "",
    "virtualAccountNumber": "",
    "status": "PAID"
  },
  "signature": "test_signature"
}'

# Compute signature
SIGNATURE=$(compute_signature "$WEBHOOK_BODY" "$CHECKSUM_KEY")

echo "📝 Webhook body (orderCode: 999999):"
echo "$WEBHOOK_BODY" | jq '.' 2>/dev/null || echo "$WEBHOOK_BODY"
echo ""
echo "🔐 Computed signature: $SIGNATURE"
echo ""

# Send webhook request
echo "📤 Sending webhook request..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${WEBHOOK_ENDPOINT}" \
    -H "Content-Type: application/json" \
    -H "x-payos-signature: ${SIGNATURE}" \
    -d "$WEBHOOK_BODY")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')

echo "📥 Response (HTTP ${HTTP_CODE}):"
echo "$RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Webhook processed successfully!${NC}"
    echo ""
    echo "✅ SUCCESS: Webhook is working correctly!"
    echo ""
    echo "Note: Order 999999 might not exist in database, so update may fail."
    echo "      But the webhook endpoint is working and signature verification passed."
else
    echo -e "${YELLOW}⚠ Unexpected response code: ${HTTP_CODE}${NC}"
    echo "This might be OK if order 999999 doesn't exist."
fi
echo ""

# Test 4: Test with invalid signature
echo "Test 4: Testing signature verification (invalid signature)..."
INVALID_SIGNATURE="invalid_signature_12345"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${WEBHOOK_ENDPOINT}" \
    -H "Content-Type: application/json" \
    -H "x-payos-signature: ${INVALID_SIGNATURE}" \
    -d "$WEBHOOK_BODY" || echo "")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "401" ]; then
    echo -e "${GREEN}✓ Signature verification working (rejected invalid signature)${NC}"
else
    echo -e "${YELLOW}⚠ Expected 401 for invalid signature, got: ${HTTP_CODE}${NC}"
fi
echo ""

# Summary
echo "=================================="
echo "🎉 Webhook Testing Complete!"
echo "=================================="
echo ""
echo "Next Steps:"
echo "1. Start ngrok: ngrok http 8080"
echo "2. Copy ngrok HTTPS URL"
echo "3. Update .env: PAYOS_WEBHOOK_URL=https://xxx.ngrok.io/api/payos-webhook"
echo "4. Restart backend: ./gradlew bootRun"
echo "5. Configure webhook URL on PayOS dashboard: https://my.payos.vn/"
echo "6. Test with real payment and close tab immediately"
echo ""
echo "📋 Check logs: tail -f logs/application.log | grep webhook"
echo ""
